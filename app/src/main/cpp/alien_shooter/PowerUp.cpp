#include "PowerUp.h"
#include <algorithm>

namespace {
    constexpr float kSizeOfScreenWidth = 0.07F;    // orb diameter
    constexpr float kHitRadiusOfSize = 0.55F;      // a bit bigger than the orb: easy to grab
    constexpr float kSpeedOfHeight = 0.22F;        // slow fall, ~4.5 s to cross the screen
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

void PowerUps::spawn(float x, float y, PowerUpKind kind) {
    for (auto &p : m_powerUps) {
        if (p.active) {
            continue;
        }
        p.x = p.prevX = x;
        p.y = p.prevY = y;
        p.kind = kind;
        p.active = true;
        return;
    }
}

void PowerUps::update(float dt) {
    const float half = m_size * 0.5F;
    for (auto &p : m_powerUps) {
        if (!p.active) {
            continue;
        }
        p.y += m_speed * dt;
        if (p.y - half > m_screenHeight) {
            p.active = false;   // missed it
        }
    }
}

void PowerUps::collect(float x, float yTop, float yBottom, float radius,
                       int &gunUpgrades, int &lasers) {
    const float hitDistance = radius + m_size * kHitRadiusOfSize;
    gunUpgrades = 0;
    lasers = 0;
    for (auto &p : m_powerUps) {
        if (!p.active) {
            continue;
        }
        const float closestY = std::clamp(p.y, yTop, yBottom);
        const float dx = p.x - x;
        const float dy = p.y - closestY;
        if (dx * dx + dy * dy < hitDistance * hitDistance) {
            p.active = false;
            if (p.kind == PowerUpKind::Laser) {
                ++lasers;
            } else {
                ++gunUpgrades;
            }
        }
    }
}

int PowerUps::activeCount(PowerUpKind kind) const {
    int count = 0;
    for (const auto &p : m_powerUps) {
        if (p.active && p.kind == kind) {
            ++count;
        }
    }
    return count;
}
