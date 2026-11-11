#include "Bullet.h"

namespace {
    constexpr float kSizeOfScreenWidth = 0.022f;   // bullet width
    constexpr float kLengthOfSize = 3.0f;          // bullets are drawn 3x as long as wide
    constexpr float kHitRadiusOfSize = 0.9f;       // a little generous, feels better
    constexpr float kSpeedOfHeight = 1.5f;         // crosses the screen in ~0.7 s
    constexpr float kFireIntervalSec = 0.15f;      // ~6-7 volleys per second
    constexpr float kLaneSpacingOfSize = 2.4f;     // gap between side-by-side bullets
}

Bullets::Bullets(std::size_t maxCount) : m_bullets(maxCount) {}

void Bullets::reset(float screenWidth, float screenHeight) {
    m_size = screenWidth * kSizeOfScreenWidth;
    m_speed = screenHeight * kSpeedOfHeight;
    m_fireTimer = kFireIntervalSec;   // first shot on the very first tick
    for (auto &bullet : m_bullets) {
        bullet = BulletState{};
    }
}

void Bullets::beginTick() {
    for (auto &bullet : m_bullets) {
        bullet.prevX = bullet.x;
        bullet.prevY = bullet.y;
    }
}

void Bullets::update(float dt, float gunX, float gunY, int lanes) {
    // 1. Move the bullets that are already flying.
    const float halfLength = m_size * kLengthOfSize * 0.5f;
    for (auto &bullet : m_bullets) {
        if (!bullet.active) continue;
        bullet.y -= m_speed * dt;
        if (bullet.y + halfLength < 0.0f) {
            bullet.active = false;   // left the top of the screen
        }
    }

    // 2. Auto-fire a volley: `lanes` bullets in a row, e.g. for 3 lanes
    //    the x offsets are -spacing, 0, +spacing.
    m_fireTimer += dt;
    if (m_fireTimer >= kFireIntervalSec) {
        const float spacing = m_size * kLaneSpacingOfSize;
        const float firstOffset = -0.5f * static_cast<float>(lanes - 1) * spacing;
        int fired = 0;
        for (int lane = 0; lane < lanes; ++lane) {
            BulletState *slot = findInactive();
            if (!slot) break;   // pool full: fire what we can
            const float x = gunX + firstOffset + static_cast<float>(lane) * spacing;
            slot->x = x;
            slot->y = gunY;
            slot->prevX = x;      // no interpolation streak from the old position
            slot->prevY = gunY;
            slot->active = true;
            ++fired;
        }
        // If nothing could fire, try again next tick.
        m_fireTimer = fired > 0 ? 0.0f : kFireIntervalSec;
    }
}

float Bullets::hitRadius() const {
    return m_size * kHitRadiusOfSize;
}

BulletState *Bullets::findInactive() {
    for (auto &bullet : m_bullets) {
        if (!bullet.active) return &bullet;
    }
    return nullptr;
}
