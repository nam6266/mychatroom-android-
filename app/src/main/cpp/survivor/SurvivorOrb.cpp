#include "SurvivorOrb.h"
#include <algorithm>
#include <cmath>

namespace {
    constexpr float kSizeOfScreenWidth = 0.028F;     // small green dot
    constexpr float kMagnetOfScreenWidth = 0.18F;    // pick-up pull starts this close to the ship
    constexpr float kFlySpeedOfScreenWidth = 1.4F;   // faster than the ship, so it always catches up
    constexpr float kCollectOfScreenWidth = 0.045F;  // touching the ship's body
    constexpr float kNukeLifeSec = 20.0F;            // a red orb left alone disappears, so a new one can drop
}

SurvivorOrbs::SurvivorOrbs(std::size_t maxCount) : m_orbs(maxCount) {}

void SurvivorOrbs::reset(float screenWidth) {
    m_size = screenWidth * kSizeOfScreenWidth;
    m_magnetRange = screenWidth * kMagnetOfScreenWidth;
    m_flySpeed = screenWidth * kFlySpeedOfScreenWidth;
    m_collectRange = screenWidth * kCollectOfScreenWidth;
    for (auto &o : m_orbs) {
        o = SurvivorOrbState{};
    }
}

void SurvivorOrbs::beginTick() {
    for (auto &o : m_orbs) {
        o.prevX = o.x;
        o.prevY = o.y;
    }
}

void SurvivorOrbs::spawn(float x, float y, OrbKind kind) {
    if (m_orbs.empty()) {
        return;
    }
    // A free slot, or else the green orb that has waited longest (the player
    // probably left it far behind anyway). A red Nuke orb is never thrown away.
    auto slot = std::find_if(m_orbs.begin(), m_orbs.end(),
                             [](const SurvivorOrbState &o) { return !o.active; });
    if (slot == m_orbs.end()) {
        slot = std::max_element(m_orbs.begin(), m_orbs.end(),
                                [](const SurvivorOrbState &a, const SurvivorOrbState &b) {
                                    const float ageA = (a.kind == OrbKind::Nuke) ? -1.0F : a.age;
                                    const float ageB = (b.kind == OrbKind::Nuke) ? -1.0F : b.age;
                                    return ageA < ageB;
                                });
        if (slot->kind == OrbKind::Nuke) {
            return;   // the whole pool is red orbs: nothing we may replace
        }
    }
    *slot = SurvivorOrbState{};
    slot->kind = kind;
    slot->x = x;
    slot->y = y;
    slot->prevX = x;   // no interpolation streak from the slot's old position
    slot->prevY = y;
    slot->active = true;
}

bool SurvivorOrbs::hasNukeOnGround() const {
    return std::any_of(m_orbs.begin(), m_orbs.end(), [](const SurvivorOrbState &o) {
        return o.active && o.kind == OrbKind::Nuke;
    });
}

SurvivorOrbs::Collected SurvivorOrbs::update(float dt, float playerX, float playerY) {
    Collected collected;
    const float magnetSq = m_magnetRange * m_magnetRange;
    for (auto &o : m_orbs) {
        if (!o.active) {
            continue;
        }
        o.age += dt;
        if (o.kind == OrbKind::Nuke && o.age > kNukeLifeSec) {
            o.active = false;
            continue;
        }
        const float dx = playerX - o.x;
        const float dy = playerY - o.y;
        const float distSq = dx * dx + dy * dy;
        if (distSq > magnetSq) {
            continue;   // too far: lies still
        }
        const float dist = std::sqrt(distSq);
        if (dist <= m_collectRange) {
            o.active = false;
            if (o.kind == OrbKind::Nuke) {
                ++collected.nukes;
            } else {
                ++collected.exp;
            }
            continue;
        }
        // Fly straight at the ship, but never past it in one tick.
        const float step = std::min(m_flySpeed * dt, dist);
        o.x += dx / dist * step;
        o.y += dy / dist * step;
    }
    return collected;
}
