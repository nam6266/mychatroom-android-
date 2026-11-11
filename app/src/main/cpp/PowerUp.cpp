#include "PowerUp.h"
#include <algorithm>

namespace {
    constexpr float kSizeOfScreenWidth = 0.07f;    // orb diameter
    constexpr float kHitRadiusOfSize = 0.55f;      // a bit bigger than the orb: easy to grab
    constexpr float kSpeedOfHeight = 0.22f;        // slow fall, ~4.5 s to cross the screen
}

PowerUps::PowerUps(std::size_t maxCount) : m_powerUps(maxCount) {}

void PowerUps::reset(float screenWidth, float screenHeight) {
    m_screenHeight = screenHeight;
    m_size = screenWidth * kSizeOfScreenWidth;
    m_speed = screenHeight * kSpeedOfHeight;
    for (auto &p : m_powerUps) {
        p = PowerUpState{};
    }
}

void PowerUps::beginTick() {
    for (auto &p : m_powerUps) {
        p.prevX = p.x;
        p.prevY = p.y;
    }
}

void PowerUps::spawn(float x, float y) {
    for (auto &p : m_powerUps) {
        if (p.active) continue;
        p.x = p.prevX = x;
        p.y = p.prevY = y;
        p.active = true;
        return;
    }
}

void PowerUps::update(float dt) {
    const float half = m_size * 0.5f;
    for (auto &p : m_powerUps) {
        if (!p.active) continue;
        p.y += m_speed * dt;
        if (p.y - half > m_screenHeight) {
            p.active = false;   // missed it
        }
    }
}

int PowerUps::collect(float x, float yTop, float yBottom, float radius) {
    const float hitDistance = radius + m_size * kHitRadiusOfSize;
    int collected = 0;
    for (auto &p : m_powerUps) {
        if (!p.active) continue;
        const float closestY = std::clamp(p.y, yTop, yBottom);
        const float dx = p.x - x;
        const float dy = p.y - closestY;
        if (dx * dx + dy * dy < hitDistance * hitDistance) {
            p.active = false;
            ++collected;
        }
    }
    return collected;
}

int PowerUps::activeCount() const {
    int count = 0;
    for (const auto &p : m_powerUps) {
        if (p.active) ++count;
    }
    return count;
}
