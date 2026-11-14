#include "SurvivorBullet.h"
#include <algorithm>

namespace {
    constexpr float kSizeOfScreenWidth = 0.022F;   // same bolt size as Alien Shooter
    constexpr float kSpeedOfScreenWidth = 1.1F;    // ~2x the ship's speed
    constexpr float kRangeOfScreenHeight = 0.55F;  // flies a bit past the screen edge
}

SurvivorBullets::SurvivorBullets(std::size_t maxCount) : m_bullets(maxCount) {}

void SurvivorBullets::reset(float screenWidth, float screenHeight) {
    m_size = screenWidth * kSizeOfScreenWidth;
    m_speed = screenWidth * kSpeedOfScreenWidth;
    m_range = screenHeight * kRangeOfScreenHeight;
    for (auto &b : m_bullets) { b = SurvivorBulletState{};
}
}

void SurvivorBullets::beginTick() {
    for (auto &b : m_bullets) {
        b.prevX = b.x;
        b.prevY = b.y;
    }
}

void SurvivorBullets::update(float dt) {
    for (auto &b : m_bullets) {
        if (!b.active) {
            continue;
        }
        b.x += b.vx * dt;
        b.y += b.vy * dt;
        b.life -= dt;
        if (b.life <= 0.0F) {
            b.active = false;
        }
    }
}

void SurvivorBullets::fire(float x, float y, float dirX, float dirY) {
    auto slot = std::find_if(m_bullets.begin(), m_bullets.end(),
                             [](const SurvivorBulletState &b) { return !b.active; });
    if (slot == m_bullets.end()) {
        return;   // pool full: skip this shot
    }
    *slot = SurvivorBulletState{};
    slot->x = slot->prevX = x;
    slot->y = slot->prevY = y;
    slot->vx = dirX * m_speed;
    slot->vy = dirY * m_speed;
    slot->life = m_range / m_speed;        // lifetime = range / speed
    slot->active = true;
}
