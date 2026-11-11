//
// Created by OS on 04/23/25.
//

#include "Enemy.h"
#include <algorithm>

namespace {
    constexpr float kSizeOfScreenWidth = 0.15f;
    // metero.png: the rock fills only ~43% of the image's width (~63% of its
    // height, because the 534x400 image is drawn into a square). 0.22 matches
    // the visible rock's half-width, so a hit always looks like a hit.
    constexpr float kHitRadiusOfSize = 0.22f;
    constexpr float kSpawnIntervalSec = 0.33f;     // was 20 frames at 60 fps
    // Speeds are fractions of the screen height per second, so the game
    // feels the same on every screen size and at any tick rate.
    constexpr float kStartSpeedOfHeight = 0.45f;
    constexpr float kSpeedGainOfHeight = 0.015f;   // per second of play
    constexpr float kMaxSpeedOfHeight = 1.20f;
}

Enemy::Enemy(std::size_t maxCount) : m_enemies(maxCount) {}

void Enemy::reset(float screenWidth, float screenHeight) {
    m_screenWidth = screenWidth;
    m_screenHeight = screenHeight;
    m_size = screenWidth * kSizeOfScreenWidth;
    m_speed = screenHeight * kStartSpeedOfHeight;
    m_spawnTimer = 0.0f;
    for (auto &enemy : m_enemies) {
        enemy = EnemyState{};
    }
}

void Enemy::beginTick() {
    for (auto &enemy : m_enemies) {
        enemy.prevX = enemy.x;
        enemy.prevY = enemy.y;
    }
}

int Enemy::update(float dt, std::mt19937 &rng) {
    m_speed = std::min(m_speed + m_screenHeight * kSpeedGainOfHeight * dt,
                       m_screenHeight * kMaxSpeedOfHeight);

    m_spawnTimer += dt;
    if (m_spawnTimer >= kSpawnIntervalSec) {
        if (EnemyState *slot = findInactive()) {
            spawn(*slot, rng);
            m_spawnTimer = 0.0f;
        } else {
            // Pool is full: spawn as soon as a slot frees up.
            m_spawnTimer = kSpawnIntervalSec;
        }
    }

    int dodged = 0;
    const float half = m_size * 0.5f;
    for (auto &enemy : m_enemies) {
        if (!enemy.active) continue;
        enemy.y += m_speed * dt;
        if (enemy.y - half > m_screenHeight) {
            enemy.active = false;
            ++dodged;
        }
    }
    return dodged;
}

bool Enemy::touches(const EnemyState &enemy, float x, float yTop, float yBottom,
                    float radius) const {
    const float hitDistance = radius + m_size * kHitRadiusOfSize;
    // closest point of the segment x, [yTop, yBottom] to the meteor centre
    const float closestY = std::clamp(enemy.y, yTop, yBottom);
    const float dx = enemy.x - x;
    const float dy = enemy.y - closestY;
    return dx * dx + dy * dy < hitDistance * hitDistance;
}

bool Enemy::collidesWith(float x, float yTop, float yBottom, float radius) const {
    for (const auto &enemy : m_enemies) {
        if (enemy.active && touches(enemy, x, yTop, yBottom, radius)) return true;
    }
    return false;
}

bool Enemy::hitBySegment(float x, float yTop, float yBottom, float radius,
                         float *hitX, float *hitY) {
    for (auto &enemy : m_enemies) {
        if (enemy.active && touches(enemy, x, yTop, yBottom, radius)) {
            enemy.active = false;   // destroyed
            if (hitX) *hitX = enemy.x;
            if (hitY) *hitY = enemy.y;
            return true;
        }
    }
    return false;
}

EnemyState *Enemy::findInactive() {
    for (auto &enemy : m_enemies) {
        if (!enemy.active) return &enemy;
    }
    return nullptr;
}

void Enemy::spawn(EnemyState &enemy, std::mt19937 &rng) const {
    const float half = m_size * 0.5f;
    std::uniform_real_distribution<float> column(half, std::max(half, m_screenWidth - half));
    enemy.x = column(rng);
    enemy.y = -half;          // just above the top edge, slides in
    enemy.prevX = enemy.x;    // no interpolation streak from the old position
    enemy.prevY = enemy.y;
    enemy.active = true;
}
