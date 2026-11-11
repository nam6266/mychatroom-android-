#include "GameWorld.h"
#include <algorithm>

namespace {
    constexpr int kPointsPerDodge = 1;    // meteor fell off the bottom
    constexpr int kPointsPerKill = 10;    // meteor shot down
    constexpr int kPointsPerAlien = 25;   // UFO shot down
    constexpr float kUpgradeDropChance = 0.20f;   // 1 in 5 kills drops an upgrade
    // explosion pools: duration (s), size (fraction of screen width)
    constexpr float kMeteorBlastSec = 0.55f, kMeteorBlastSize = 0.22f;
    constexpr float kAlienBlastSec = 1.2f, kAlienBlastSize = 0.16f;   // 22 frames at ~18 fps
}

GameWorld::GameWorld(std::size_t maxEnemies, std::size_t maxBullets, std::size_t maxPowerUps,
                     std::size_t maxExplosions, std::size_t maxAliens, std::size_t maxAlienBlasts)
        : m_enemy(maxEnemies), m_bullets(maxBullets), m_powerUps(maxPowerUps),
          m_explosions(maxExplosions, kMeteorBlastSec, kMeteorBlastSize),
          m_aliens(maxAliens),
          m_alienBlasts(maxAlienBlasts, kAlienBlastSec, kAlienBlastSize),
          m_rng(std::random_device{}()) {}

void GameWorld::setScreen(float width, float height) {
    if (width == m_width && height == m_height) return;
    m_width = width;
    m_height = height;
    reset();
}

void GameWorld::reset() {
    m_player.reset(m_width, m_height);
    m_enemy.reset(m_width, m_height);
    m_bullets.reset(m_width, m_height);
    m_powerUps.reset(m_width, m_height);
    m_explosions.reset(m_width);
    m_aliens.reset(m_width, m_height);
    m_alienBlasts.reset(m_width);
    m_score = 0;
    m_weaponLevel = 1;
    m_gameOver = false;
}

void GameWorld::beginTick() {
    m_player.beginTick();
    m_enemy.beginTick();
    m_bullets.beginTick();
    m_powerUps.beginTick();
    m_aliens.beginTick();
}

void GameWorld::movePlayerBy(float dx) {
    if (m_gameOver || !hasScreen()) return;
    m_player.moveBy(dx);
}

void GameWorld::update(float dt) {
    if (!hasScreen()) return;
    // Explosions keep playing after game over, so the last one finishes.
    m_explosions.update(dt);
    m_alienBlasts.update(dt);
    if (m_gameOver) return;

    // 1. Meteors fall (and new ones spawn).
    m_score += m_enemy.update(dt, m_rng) * kPointsPerDodge;
    m_aliens.update(dt, m_rng);   // aliens weave around and spawn

    // 2. Bullets fly, and the gun fires from the rocket's nose.
    const float gunY = m_player.y() - m_player.size() * 0.5f;
    m_bullets.update(dt, m_player.x(), gunY, m_weaponLevel);

    // 3. Bullets vs meteors and aliens: target and bullet disappear, player
    //    scores, the target explodes, maybe an upgrade drops.
    for (auto &bullet : m_bullets.bullets()) {
        if (!bullet.active) continue;
        float hitX = 0.0f, hitY = 0.0f;
        // path this tick: from prevY (lower) up to y
        if (m_enemy.hitBySegment(bullet.x, bullet.y, bullet.prevY, m_bullets.hitRadius(),
                                 &hitX, &hitY)) {
            bullet.active = false;
            m_score += kPointsPerKill;
            m_explosions.spawn(hitX, hitY, m_rng);
            maybeDropUpgrade(hitX, hitY);
        } else {
            const AlienHit hit = m_aliens.hitBySegment(bullet.x, bullet.y, bullet.prevY,
                                                       m_bullets.hitRadius(), &hitX, &hitY);
            if (hit == AlienHit::None) continue;
            bullet.active = false;            // the bullet is used up either way
            if (hit == AlienHit::Destroyed) {
                m_score += kPointsPerAlien;
                m_alienBlasts.spawn(hitX, hitY, m_rng);
                maybeDropUpgrade(hitX, hitY);
            }
        }
    }

    // 4. Upgrades fall; touching one raises the weapon level (max 3).
    m_powerUps.update(dt);
    const int collected = m_powerUps.collect(m_player.x(), m_player.hitTop(),
                                             m_player.hitBottom(), m_player.hitRadius());
    m_weaponLevel = std::min(kMaxWeaponLevel, m_weaponLevel + collected);

    // 5. Meteor vs player, last, so a meteor shot just in time can't kill you.
    if (m_enemy.collidesWith(m_player.x(), m_player.hitTop(), m_player.hitBottom(),
                             m_player.hitRadius()) ||
        m_aliens.collidesWith(m_player.x(), m_player.hitTop(), m_player.hitBottom(),
                              m_player.hitRadius())) {
        m_gameOver = true;
    }
}

void GameWorld::maybeDropUpgrade(float x, float y) {
    // No more drops once the level we have, plus orbs already falling,
    // would reach the max: never show an upgrade the player can't use.
    if (m_weaponLevel + m_powerUps.activeCount() >= kMaxWeaponLevel) return;
    std::uniform_real_distribution<float> roll(0.0f, 1.0f);
    if (roll(m_rng) < kUpgradeDropChance) {
        m_powerUps.spawn(x, y);
    }
}
