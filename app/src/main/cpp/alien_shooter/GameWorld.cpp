#include "GameWorld.h"
#include <algorithm>

namespace {
    constexpr int kPointsPerDodge = 1;    // meteor fell off the bottom
    constexpr int kPointsPerKill = 10;    // meteor shot down
    constexpr int kPointsPerAlien = 25;   // UFO shot down
    constexpr float kUpgradeDropChance = 0.20F;   // 1 in 5 kills drops a gun upgrade
    constexpr float kLaserDropChance = 0.08F;     // ~1 in 12 kills drops a laser orb
    constexpr float kLaserDurationSec = 6.0F;     // one laser orb = 6 s of beam
    constexpr float kLaserHitsPerSec = 20.0F;     // damage: like 20 bullets a second
    constexpr float kLaserWidthOfScreen = 0.035F; // beam width (hit strip and drawn core)
    // explosion pools: duration (s), size (fraction of screen width)
    constexpr float kMeteorBlastSec = 0.55F, kMeteorBlastSize = 0.22F;
    constexpr float kAlienBlastSec = 1.2F, kAlienBlastSize = 0.16F;   // 22 frames at ~18 fps
}

GameWorld::GameWorld(std::size_t maxEnemies, std::size_t maxBullets, std::size_t maxPowerUps,
                     std::size_t maxExplosions, std::size_t maxAliens, std::size_t maxAlienBlasts)
        : m_enemy(maxEnemies), m_bullets(maxBullets), m_powerUps(maxPowerUps),
          m_explosions(maxExplosions, kMeteorBlastSec, kMeteorBlastSize),
          m_aliens(maxAliens),
          m_alienBlasts(maxAlienBlasts, kAlienBlastSec, kAlienBlastSize),
          m_rng(std::random_device{}()) {}

void GameWorld::setScreen(float width, float height) {
    if (width == m_width && height == m_height) {
        return;
    }
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
    m_laserTime = 0.0F;
    m_laserTopY = 0.0F;
    m_laserDamageTimer = 0.0F;
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
    if (m_gameOver || !hasScreen()) {
        return;
    }
    m_player.moveBy(dx);
}

void GameWorld::update(float dt) {
    if (!hasScreen()) {
        return;
    }
    // Explosions keep playing after game over, so the last one finishes.
    m_explosions.update(dt);
    m_alienBlasts.update(dt);
    if (m_gameOver) {
        return;
    }

    // 1. Meteors fall (and new ones spawn).
    m_score += m_enemy.update(dt, m_rng) * kPointsPerDodge;
    m_aliens.update(dt, m_rng);   // aliens weave around and spawn

    // 2. Bullets fly, and the gun fires from the rocket's nose.
    const float gunY = m_player.y() - m_player.size() * 0.5F;
    const bool laserOn = m_laserTime > 0.0F;
    // While the laser is on the gun pauses (0 lanes); bullets already flying keep going.
    m_bullets.update(dt, m_player.x(), gunY, laserOn ? 0 : m_weaponLevel);

    // 3. Bullets vs meteors and aliens: target and bullet disappear, player
    //    scores, the target explodes, maybe an upgrade drops.
    for (auto &bullet : m_bullets.bullets()) {
        if (!bullet.active) {
            continue;
        }
        float hitX = 0.0F;
        float hitY = 0.0F;
        // path this tick: from prevY (lower) up to y
        if (m_enemy.hitBySegment(bullet.x, bullet.y, bullet.prevY, m_bullets.hitRadius(),
                                 &hitX, &hitY)) {
            bullet.active = false;
            onMeteorKilled(hitX, hitY);
        } else {
            const AlienHit hit = m_aliens.hitBySegment(bullet.x, bullet.y, bullet.prevY,
                                                       m_bullets.hitRadius(), &hitX, &hitY);
            if (hit == AlienHit::None) {
                continue;
            }
            bullet.active = false;            // the bullet is used up either way
            onAlienHit(hit, hitX, hitY);
        }
    }

    // 3b. Laser beam (only while a laser orb's time is running).
    if (laserOn) {
        updateLaser(dt, gunY);
    }

    // 4. Orbs fall; red ones raise the gun level (max 3), blue ones start or
    //    refresh the laser.
    m_powerUps.update(dt);
    int gunUpgrades = 0;
    int lasers = 0;
    m_powerUps.collect(m_player.x(), m_player.hitTop(), m_player.hitBottom(),
                       m_player.hitRadius(), gunUpgrades, lasers);
    m_weaponLevel = std::min(kMaxWeaponLevel, m_weaponLevel + gunUpgrades);
    if (lasers > 0) {
        m_laserTime = kLaserDurationSec;
    }

    // 5. Meteor vs player, last, so a meteor shot just in time can't kill you.
    if (m_enemy.collidesWith(m_player.x(), m_player.hitTop(), m_player.hitBottom(),
                             m_player.hitRadius()) ||
        m_aliens.collidesWith(m_player.x(), m_player.hitTop(), m_player.hitBottom(),
                              m_player.hitRadius())) {
        m_gameOver = true;
    }
}

void GameWorld::onMeteorKilled(float x, float y) {
    m_score += kPointsPerKill;
    m_explosions.spawn(x, y, m_rng);
    maybeDropUpgrade(x, y);
}

void GameWorld::onAlienHit(AlienHit hit, float x, float y) {
    if (hit != AlienHit::Destroyed) {
        return;   // just damaged: it flashes, nothing else
    }
    m_score += kPointsPerAlien;
    m_alienBlasts.spawn(x, y, m_rng);
    maybeDropUpgrade(x, y);
}

float GameWorld::laserWidth() const {
    return m_width * kLaserWidthOfScreen;
}

void GameWorld::updateLaser(float dt, float gunY) {
    m_laserTime = std::max(0.0F, m_laserTime - dt);
    const float x = m_player.x();
    const float halfWidth = laserWidth() * 0.5F;

    // 1. Who is first in the beam? Ask both enemy types, keep the nearer one
    //    (larger y = lower on screen = closer to the ship).
    float meteorContactY = 0.0F;
    float alienContactY = 0.0F;
    const int meteor = m_enemy.firstInBeam(x, halfWidth, gunY, &meteorContactY);
    const int alien = m_aliens.firstInBeam(x, halfWidth, gunY, &alienContactY);
    const bool targetIsMeteor = meteor >= 0 && (alien < 0 || meteorContactY >= alienContactY);
    const bool targetIsAlien = alien >= 0 && !targetIsMeteor;

    // 2. The beam stops at that target, or runs off the top of the screen.
    m_laserTopY = targetIsMeteor ? meteorContactY : targetIsAlien ? alienContactY : 0.0F;

    // 3. Damage on a fixed clock: one hit every 1/20 s, whatever the tick rate.
    m_laserDamageTimer += dt;
    const float hitInterval = 1.0F / kLaserHitsPerSec;
    if (m_laserDamageTimer < hitInterval) {
        return;
    }
    m_laserDamageTimer -= hitInterval;
    float hitX = 0.0F;
    float hitY = 0.0F;
    if (targetIsMeteor) {
        m_enemy.destroy(meteor, &hitX, &hitY);   // meteors have no hp: gone in one hit
        onMeteorKilled(hitX, hitY);
    } else if (targetIsAlien) {
        onAlienHit(m_aliens.damage(alien, &hitX, &hitY), hitX, hitY);
    } else {
        // Beam reached the top of the screen without touching anything: no damage.
    }
}

void GameWorld::maybeDropUpgrade(float x, float y) {
    std::uniform_real_distribution<float> roll(0.0F, 1.0F);
    const float r = roll(m_rng);
    // Laser orb first: rarer, and only one falling at a time.
    if (r < kLaserDropChance) {
        if (m_powerUps.activeCount(PowerUpKind::Laser) == 0) {
            m_powerUps.spawn(x, y, PowerUpKind::Laser);
        }
        return;
    }
    // Gun upgrade: none once the level we have, plus red orbs already
    // falling, would reach the max (never show one the player can't use).
    if (m_weaponLevel + m_powerUps.activeCount(PowerUpKind::GunUpgrade) >= kMaxWeaponLevel) {
        return;
    }
    if (r < kLaserDropChance + kUpgradeDropChance) {
        m_powerUps.spawn(x, y, PowerUpKind::GunUpgrade);
    }
}
