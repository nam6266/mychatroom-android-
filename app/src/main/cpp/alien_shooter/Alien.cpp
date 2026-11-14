#include "Alien.h"
#include <algorithm>
#include <cmath>

namespace {
    constexpr float kTwoPi = 6.28318530718F;
    constexpr float kSizeOfScreenWidth = 0.16F;   // UFO width on screen
    constexpr float kHitRadiusOfSize = 0.42F;     // round-ish saucer + dome
    constexpr float kFirstSpawnSec = 5.0F;        // a few seconds of meteors first
    constexpr float kSpawnMinSec = 4.0F;          // then one every 4-7 s
    constexpr float kSpawnMaxSec = 7.0F;
    constexpr int kMaxAliveAtOnce = 3;
    constexpr int kMinHp = 10;                    // hits needed to destroy one: 10-15
    constexpr int kMaxHp = 15;
    constexpr float kHitFlashSec = 0.10F;         // orange dome after each hit
    // Path, as fractions of the screen, per second where it's a speed.
    constexpr float kFallSpeedOfHeight = 0.07F;   // ~14 s to drift across the screen
    constexpr float kSwayMinOfWidth = 0.18F;
    constexpr float kSwayMaxOfWidth = 0.32F;
    constexpr float kSwayMinFreq = 0.25F;         // one full sway every 2.5-4 s
    constexpr float kSwayMaxFreq = 0.40F;
    constexpr float kBobOfHeight = 0.05F;         // loops up and down while weaving
}

Aliens::Aliens(std::size_t maxCount) : m_aliens(maxCount) {}

void Aliens::reset(float screenWidth, float screenHeight) {
    m_screenWidth = screenWidth;
    m_screenHeight = screenHeight;
    m_size = screenWidth * kSizeOfScreenWidth;
    m_spawnTimer = kFirstSpawnSec;
    for (auto &a : m_aliens) {
        a = AlienState{};
    }
}

void Aliens::beginTick() {
    for (auto &a : m_aliens) {
        a.prevX = a.x;
        a.prevY = a.y;
    }
}

void Aliens::place(AlienState &a) const {
    const float w = kTwoPi * a.swayFreq;
    a.x = a.centerX + a.swayAmp * std::sin(w * a.age + a.phase);
    // vertical bob at twice the sway speed -> figure-8 loops
    a.y = a.entryY + a.fallSpeed * a.age + a.bobAmp * std::sin(2.0F * w * a.age + a.phase);
}

void Aliens::spawn(AlienState &a, std::mt19937 &rng) const {
    std::uniform_real_distribution<float> unit(0.0F, 1.0F);
    const float half = m_size * 0.5F;
    a = AlienState{};
    a.swayAmp = m_screenWidth * (kSwayMinOfWidth + (kSwayMaxOfWidth - kSwayMinOfWidth) * unit(rng));
    // keep the whole sway on screen
    const float minCenter = half + a.swayAmp;
    const float maxCenter = std::max(minCenter, m_screenWidth - half - a.swayAmp);
    a.centerX = minCenter + (maxCenter - minCenter) * unit(rng);
    a.swayFreq = kSwayMinFreq + (kSwayMaxFreq - kSwayMinFreq) * unit(rng);
    a.bobAmp = m_screenHeight * kBobOfHeight;
    a.phase = kTwoPi * unit(rng);
    a.fallSpeed = m_screenHeight * kFallSpeedOfHeight;
    a.entryY = -m_size;            // starts just above the top edge
    std::uniform_int_distribution<int> hpDist(kMinHp, kMaxHp);
    a.hp = hpDist(rng);
    a.active = true;
    place(a);
    a.prevX = a.x;                 // no interpolation streak from the old position
    a.prevY = a.y;
}

void Aliens::update(float dt, std::mt19937 &rng) {
    for (auto &a : m_aliens) {
        if (!a.active) {
            continue;
        }
        a.age += dt;
        a.hitFlash = std::max(0.0F, a.hitFlash - dt);
        place(a);
        if (a.y - m_size > m_screenHeight) {
            a.active = false;   // flew off the bottom
        }
    }

    m_spawnTimer -= dt;
    if (m_spawnTimer <= 0.0F) {
        const int alive = static_cast<int>(std::count_if(m_aliens.begin(), m_aliens.end(),
                                                         [](const AlienState &a) { return a.active; }));
        auto slot = std::find_if(m_aliens.begin(), m_aliens.end(),
                                 [](const AlienState &a) { return !a.active; });
        if (alive < kMaxAliveAtOnce && slot != m_aliens.end()) {
            spawn(*slot, rng);
            std::uniform_real_distribution<float> next(kSpawnMinSec, kSpawnMaxSec);
            m_spawnTimer = next(rng);
        } else {
            m_spawnTimer = 0.5F;   // busy sky: try again shortly
        }
    }
}

bool Aliens::touches(const AlienState &a, float x, float yTop, float yBottom, float radius) const {
    const float hitDistance = radius + m_size * kHitRadiusOfSize;
    const float closestY = std::clamp(a.y, yTop, yBottom);
    const float dx = a.x - x;
    const float dy = a.y - closestY;
    return dx * dx + dy * dy < hitDistance * hitDistance;
}

AlienHit Aliens::damage(int index, float *hitX, float *hitY) {
    AlienState &a = m_aliens[static_cast<std::size_t>(index)];
    --a.hp;
    if (a.hp > 0) {
        a.hitFlash = kHitFlashSec;   // still flying: just flash
        return AlienHit::Damaged;
    }
    a.active = false;                // out of hp: destroyed
    if (hitX != nullptr) {
        *hitX = a.x;
    }
    if (hitY != nullptr) {
        *hitY = a.y;
    }
    return AlienHit::Destroyed;
}

AlienHit Aliens::hitBySegment(float x, float yTop, float yBottom, float radius,
                              float *hitX, float *hitY) {
    for (int i = 0; i < static_cast<int>(m_aliens.size()); ++i) {
        const AlienState &a = m_aliens[static_cast<std::size_t>(i)];
        if (a.active && touches(a, x, yTop, yBottom, radius)) {
            return damage(i, hitX, hitY);
        }
    }
    return AlienHit::None;
}

int Aliens::firstInBeam(float x, float halfWidth, float gunY, float *contactY) const {
    const float radius = m_size * kHitRadiusOfSize;
    int best = -1;
    float bestY = 0.0F;
    for (int i = 0; i < static_cast<int>(m_aliens.size()); ++i) {
        const auto &a = m_aliens[static_cast<std::size_t>(i)];
        if (!a.active || a.y >= gunY) {
            continue;
        }
        if (a.y + radius <= 0.0F) {
            continue;   // not on screen yet: can't be hit
        }
        if (std::fabs(a.x - x) >= radius + halfWidth) {
            continue;
        }
        if (best < 0 || a.y > bestY) {
            best = i;
            bestY = a.y;
        }
    }
    if (best >= 0 && (contactY != nullptr)) {
        *contactY = bestY + radius;
    }
    return best;
}

bool Aliens::collidesWith(float x, float yTop, float yBottom, float radius) const {
    for (const auto &a : m_aliens) {
        if (a.active && touches(a, x, yTop, yBottom, radius)) {
            return true;
        }
    }
    return false;
}
