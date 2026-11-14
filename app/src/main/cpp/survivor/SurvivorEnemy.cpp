#include "SurvivorEnemy.h"
#include <algorithm>
#include <cmath>

namespace {
    constexpr float kTwoPi = 6.28318530718F;
    constexpr float kSizeOfScreenWidth = 0.09F;   // a bit smaller than the ship (0.10)
    constexpr float kHitRadiusOfSize = 0.42F;     // same fit as Alien Shooter's UFO
    constexpr float kSpeedOfScreenWidth = 0.12F;  // slow: about 1/5 of the ship's speed
    constexpr int kHp = 3;                        // bullets needed per alien
    constexpr float kHitFlashSec = 0.10F;
    // Spawn rate: one every 1.2 s at the start, speeding up to one every
    // 0.35 s after 90 s of survival.
    constexpr float kStartSpawnSec = 1.2F;
    constexpr float kFastestSpawnSec = 0.35F;
    constexpr float kRampSec = 90.0F;
    constexpr float kFirstSpawnSec = 1.0F;
    // Crowd: two aliens closer than this many sizes push apart.
    constexpr float kSpacingOfSize = 0.85F;
}

SurvivorEnemies::SurvivorEnemies(std::size_t maxCount) : m_enemies(maxCount) {}

float SurvivorEnemies::hitRadius() const { return m_size * kHitRadiusOfSize; }

float SurvivorEnemies::spawnDistance() const {
    // A circle just bigger than the screen's half-diagonal (centre -> corner)
    // is off-screen in every direction, even at the corners.
    const float halfDiagonal = 0.5F * std::sqrt(m_screenWidth * m_screenWidth +
                                                m_screenHeight * m_screenHeight);
    return halfDiagonal + m_size;
}

void SurvivorEnemies::reset(float screenWidth, float screenHeight) {
    m_screenWidth = screenWidth;
    m_screenHeight = screenHeight;
    m_size = screenWidth * kSizeOfScreenWidth;
    m_speed = screenWidth * kSpeedOfScreenWidth;
    m_spawnTimer = kFirstSpawnSec;
    for (auto &e : m_enemies) { e = SurvivorEnemyState{};
}
    // Cell = one alien size: bigger than any distance two things interact at
    // (alien-alien spacing, bullet-alien and ship-alien hit distances), so a
    // query only ever needs the cells right around it. The grid reaches a bit
    // past the spawn circle, so it covers every alien that matters.
    m_grid.configure(m_size, spawnDistance() + m_size, m_enemies.size());
}

void SurvivorEnemies::rebuildGrid(float centerX, float centerY) {
    m_grid.build(static_cast<int>(m_enemies.size()), centerX, centerY,
                 [this](int i, float &x, float &y) {
                     const SurvivorEnemyState &e = m_enemies[static_cast<std::size_t>(i)];
                     x = e.x;
                     y = e.y;
                     return e.active;
                 });
}

void SurvivorEnemies::beginTick() {
    for (auto &e : m_enemies) {
        e.prevX = e.x;
        e.prevY = e.y;
    }
}

void SurvivorEnemies::spawn(SurvivorEnemyState &e, float targetX, float targetY,
                            std::mt19937 &rng) const {
    const float distance = spawnDistance();
    std::uniform_real_distribution<float> angleDist(0.0F, kTwoPi);
    const float angle = angleDist(rng);
    e = SurvivorEnemyState{};
    e.x = targetX + std::cos(angle) * distance;
    e.y = targetY + std::sin(angle) * distance;
    e.prevX = e.x;    // no interpolation streak from its old slot
    e.prevY = e.y;
    e.hp = kHp;
    e.active = true;
}

void SurvivorEnemies::update(float dt, float targetX, float targetY, float elapsed,
                             std::mt19937 &rng) {
    // 1. Spawn on a timer that gets shorter the longer the player survives.
    m_spawnTimer -= dt;
    if (m_spawnTimer <= 0.0F) {
        auto slot = std::find_if(m_enemies.begin(), m_enemies.end(),
                                 [](const SurvivorEnemyState &e) { return !e.active; });
        if (slot != m_enemies.end()) {
            spawn(*slot, targetX, targetY, rng);
        }
        const float ramp = std::min(1.0F, elapsed / kRampSec);
        m_spawnTimer = kStartSpawnSec + (kFastestSpawnSec - kStartSpawnSec) * ramp;
    }

    // 2. Chase: step straight towards the player at a constant slow speed.
    for (auto &e : m_enemies) {
        if (!e.active) {
            continue;
        }
        e.age += dt;
        e.hitFlash = std::max(0.0F, e.hitFlash - dt);
        const float dx = targetX - e.x;
        const float dy = targetY - e.y;
        const float dist = std::sqrt(dx * dx + dy * dy);
        if (dist > 1e-3F) {
            const float step = std::min(dist, m_speed * dt);   // never overshoot
            e.x += dx / dist * step;
            e.y += dy / dist * step;
        }
    }

    // 3. Crowd: push overlapping aliens apart, using the grid to find neighbours.
    rebuildGrid(targetX, targetY);
    separate();

    // 4. Pushing moved some aliens, so file them again: the grid now matches
    //    the final positions, and bullet / ship hit tests this tick are exact.
    rebuildGrid(targetX, targetY);
}

void SurvivorEnemies::separate() {
    const float minDist = m_size * kSpacingOfSize;
    const float minDistSq = minDist * minDist;
    const int n = static_cast<int>(m_enemies.size());
    for (int i = 0; i < n; ++i) {
        SurvivorEnemyState &a = m_enemies[static_cast<std::size_t>(i)];
        if (!a.active) {
            continue;
        }
        // Before: compared with every other alien. Now: only those filed in
        // the cells around this one.
        m_grid.forEachCandidate(a.x, a.y, minDist, [&](int j) {
            if (j <= i) {
                return true;   // each pair once (and never with itself)
            }
            SurvivorEnemyState &b = m_enemies[static_cast<std::size_t>(j)];
            if (!b.active) {
                return true;
            }
            float dx = b.x - a.x;
            float dy = b.y - a.y;
            const float distSq = dx * dx + dy * dy;
            if (distSq >= minDistSq) {
                return true;
            }
            float dist = std::sqrt(distSq);
            if (dist < 1e-3F) {  // exactly on top: pick a side
                dx = 1.0F;
                dy = 0.0F;
                dist = 1.0F;
            }
            // each moves half the overlap, away from the other
            const float push = (minDist - dist) * 0.5F;
            a.x -= dx / dist * push;  a.y -= dy / dist * push;
            b.x += dx / dist * push;  b.y += dy / dist * push;
            return true;
        });
    }
}

int SurvivorEnemies::nearest(float x, float y, float maxDistance) const {
    int best = -1;
    float bestSq = maxDistance * maxDistance;
    for (int i = 0; i < static_cast<int>(m_enemies.size()); ++i) {
        const auto &e = m_enemies[static_cast<std::size_t>(i)];
        if (!e.active) {
            continue;
        }
        const float dx = e.x - x;
        const float dy = e.y - y;
        const float dSq = dx * dx + dy * dy;
        if (dSq < bestSq) {
            bestSq = dSq;
            best = i;
        }
    }
    return best;
}

int SurvivorEnemies::touching(float x, float y, float radius) const {
    const float reach = radius + hitRadius();
    int found = -1;
    m_grid.forEachCandidate(x, y, reach, [&](int i) {
        const auto &e = m_enemies[static_cast<std::size_t>(i)];
        if (!e.active) {
            return true;   // killed earlier this tick
        }
        const float dx = e.x - x;
        const float dy = e.y - y;
        if (dx * dx + dy * dy < reach * reach) {
            found = i;
            return false;                          // first hit is enough: stop looking
        }
        return true;
    });
    return found;
}

bool SurvivorEnemies::damage(int index, float *hitX, float *hitY) {
    SurvivorEnemyState &e = m_enemies[static_cast<std::size_t>(index)];
    if (--e.hp > 0) {
        e.hitFlash = kHitFlashSec;
        return false;
    }
    e.active = false;
    if (hitX != nullptr) {
        *hitX = e.x;
    }
    if (hitY != nullptr) {
        *hitY = e.y;
    }
    return true;
}
