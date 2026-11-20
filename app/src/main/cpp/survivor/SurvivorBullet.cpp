#include "SurvivorBullet.h"
#include <algorithm>

namespace {
    constexpr float kLengthOfScreenWidth = 0.20F;  // a long laser streak
    constexpr float kSpeedOfScreenWidth = 1.1F;    // ~2x the ship's speed
    constexpr float kRangeOfScreenHeight = 0.55F;  // flies a bit past the screen edge
    // Thickness / length of each sprite (laser_lv1..6.png pixel sizes), so the
    // drawn shot keeps the image's shape and the hitbox matches what you see.
    constexpr float kThicknessOfLength[SurvivorBullets::kMaxLevel] = {
            52.0F / 603.0F, 68.0F / 604.0F, 83.0F / 603.0F,
            99.0F / 606.0F, 114.0F / 602.0F, 130.0F / 584.0F};
    constexpr float kHitOfThickness = 0.6F;        // the glow is soft: count a bit more than its core
    constexpr float kHeadOfLength = 0.4F;          // the bright tip sits near the front end
}

float SurvivorBullets::thickness() const {
    return m_length * kThicknessOfLength[static_cast<std::size_t>(m_level - 1)];
}

float SurvivorBullets::hitRadius() const { return thickness() * kHitOfThickness; }

float SurvivorBullets::headOffset() const { return m_length * kHeadOfLength; }

void SurvivorBullets::setLevel(int level) {
    m_level = std::clamp(level, 1, kMaxLevel);
}

SurvivorBullets::SurvivorBullets(std::size_t maxCount) : m_bullets(maxCount) {}

void SurvivorBullets::reset(float screenWidth, float screenHeight) {
    m_length = screenWidth * kLengthOfScreenWidth;
    m_level = 1;
    m_speed = screenWidth * kSpeedOfScreenWidth;
    m_range = screenHeight * kRangeOfScreenHeight;
    for (auto &b : m_bullets) {
        b = SurvivorBulletState{};
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

void SurvivorBullets::fire(float x, float y, float dirX, float dirY, float speedScale) {
    auto slot = std::find_if(m_bullets.begin(), m_bullets.end(),
                             [](const SurvivorBulletState &b) { return !b.active; });
    if (slot == m_bullets.end()) {
        return;   // pool full: skip this shot
    }
    *slot = SurvivorBulletState{};
    slot->x = slot->prevX = x;
    slot->y = slot->prevY = y;
    const float speed = m_speed * speedScale;
    slot->vx = dirX * speed;
    slot->vy = dirY * speed;
    slot->life = m_range / speed;          // lifetime = range / speed: same distance at any speed
    slot->active = true;
}
