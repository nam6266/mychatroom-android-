#include "SurvivorWorld.h"
#include <algorithm>
#include <cmath>

namespace {
    constexpr float kPi = 3.14159265359F;
    constexpr float kShipSizeOfWidth = 0.10F;     // smaller than Alien Shooter (0.18): bigger view
    constexpr float kMaxSpeedOfWidth = 0.55F;     // ship speed: about half a screen width per second
    constexpr float kDeadZone = 0.12F;            // ignore tiny accidental stick movements
    constexpr float kTurnSpeed = 10.0F;           // radians per second the ship can turn
    constexpr float kShipHitRadiusOfSize = 0.35F; // forgiving: only the ship's body counts
    // Gun: auto-aims at the nearest alien within range.
    constexpr float kFireIntervalSec = 0.33F;     // ~3 shots per second
    // UFO explosion: 22 frames at ~18 fps, same sheet as Alien Shooter.
    constexpr float kBlastSec = 1.2F;
    constexpr float kBlastSizeOfWidth = 0.09F;    // = alien size, so it lines up with the UFO

    // Shortest signed difference between two angles, in [-pi, pi].
    float angleDelta(float from, float to) {
        float d = std::fmod(to - from + kPi, 2.0F * kPi);
        if (d < 0.0F) {
            d += 2.0F * kPi;
        }
        return d - kPi;
    }
}

SurvivorWorld::SurvivorWorld(std::size_t maxEnemies, std::size_t maxBullets, std::size_t maxBlasts)
        : m_enemies(maxEnemies), m_bullets(maxBullets),
          m_blasts(maxBlasts, kBlastSec, kBlastSizeOfWidth),
          m_rng(std::random_device{}()) {}

void SurvivorWorld::setScreen(float width, float height) {
    if (width == m_width && height == m_height) {
        return;
    }
    m_width = width;
    m_height = height;
    m_size = width * kShipSizeOfWidth;
    m_maxSpeed = width * kMaxSpeedOfWidth;
    reset();   // every size depends on the screen, so start a fresh round
}

void SurvivorWorld::reset() {
    m_x = m_y = m_prevX = m_prevY = 0.0F;
    m_angle = m_prevAngle = 0.0F;
    m_joyX = m_joyY = 0.0F;
    m_enemies.reset(m_width, m_height);
    m_bullets.reset(m_width, m_height);
    m_blasts.reset(m_width);
    m_fireTimer = 0.0F;
    m_time = 0.0F;
    m_kills = 0;
    m_gameOver = false;
}

void SurvivorWorld::beginTick() {
    m_prevX = m_x;
    m_prevY = m_y;
    m_prevAngle = m_angle;
    m_enemies.beginTick();
    m_bullets.beginTick();
}

void SurvivorWorld::setJoystick(float x, float y) {
    // The stick is a circle: never let the input be longer than 1, so moving
    // diagonally isn't faster than moving straight.
    const float len = std::sqrt(x * x + y * y);
    if (len > 1.0F) {
        x /= len;
        y /= len;
    }
    m_joyX = x;
    m_joyY = y;
}

void SurvivorWorld::update(float dt) {
    if (!hasScreen()) {
        return;
    }
    m_blasts.update(dt);            // explosions finish playing even after game over
    if (m_gameOver) {
        return;
    }

    m_time += dt;
    movePlayer(dt);                                         // 1. ship
    m_enemies.update(dt, m_x, m_y, m_time, m_rng);          // 2. aliens spawn + chase + spread out
    m_bullets.update(dt);                                   // 3. bullets fly
    autoFire(dt);                                           // 4. gun picks a target and shoots
    bulletsHitEnemies();                                    // 5. hits, kills, explosions
    // 6. last: an alien touching the ship ends the round (so a kill this tick can save you)
    if (m_enemies.touching(m_x, m_y, m_size * kShipHitRadiusOfSize) >= 0) {
        m_gameOver = true;
    }
}

void SurvivorWorld::movePlayer(float dt) {
    const float len = std::sqrt(m_joyX * m_joyX + m_joyY * m_joyY);
    if (len < kDeadZone) {
        return;   // stick at rest: the ship stays put
    }

    // 1. Move: direction from the stick, always at full speed (how far the
    //    stick is pushed doesn't matter, as long as it's past the dead zone).
    const float dirX = m_joyX / len;
    const float dirY = m_joyY / len;
    m_x += dirX * m_maxSpeed  * dt;
    m_y += dirY * m_maxSpeed  * dt;

    // 2. Turn towards the travel direction, at most kTurnSpeed per second, so
    //    the ship swings round smoothly instead of snapping.
    const float target = std::atan2(dirX, -dirY);   // 0 = up, clockwise
    const float maxStep = kTurnSpeed * dt;
    m_angle += std::clamp(angleDelta(m_angle, target), -maxStep, maxStep);
    m_angle = angleDelta(0.0F, m_angle);            // keep it in [-pi, pi] while circling
}

void SurvivorWorld::autoFire(float dt) {
    m_fireTimer += dt;
    if (m_fireTimer < kFireIntervalSec) {
        return;
    }
    // Aim at the closest alien the bullet can actually reach.
    const int target = m_enemies.nearest(m_x, m_y, m_bullets.range());
    if (target < 0) {
        m_fireTimer = kFireIntervalSec;   // nothing in range: fire the moment something is
        return;
    }
    const auto &e = m_enemies.enemies()[static_cast<std::size_t>(target)];
    const float dx = e.x - m_x;
    const float dy = e.y - m_y;
    const float len = std::sqrt(dx * dx + dy * dy);
    if (len < 1e-3F) {
        return;
    }
    m_bullets.fire(m_x, m_y, dx / len, dy / len);
    m_fireTimer = 0.0F;
}

void SurvivorWorld::bulletsHitEnemies() {
    for (auto &b : m_bullets.bullets()) {
        if (!b.active) {
            continue;
        }
        const int hit = m_enemies.touching(b.x, b.y, m_bullets.hitRadius());
        if (hit < 0) {
            continue;
        }
        b.active = false;                 // a bullet is used up by its first hit
        float x = 0.0F;
        float y = 0.0F;
        if (m_enemies.damage(hit, &x, &y)) {
            ++m_kills;
            m_blasts.spawn(x, y, m_rng);
        }
    }
}
