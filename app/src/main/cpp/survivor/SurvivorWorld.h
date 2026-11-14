#ifndef MY_CHATROOM_SURVIVORWORLD_H
#define MY_CHATROOM_SURVIVORWORLD_H

#include <cstddef>
#include <random>
#include "SurvivorEnemy.h"
#include "SurvivorBullet.h"
#include "Explosion.h"

// Space Survivor: game rules only (no threads, no JNI), like GameWorld for
// Alien Shooter. The world is an endless plane in pixels; the camera follows
// the ship, so the ship's world position is also where the camera looks.
//
// Angles are radians, 0 = facing up the screen, growing clockwise
// (screen y points down), so the renderer can rotate the sprite directly.
class SurvivorWorld {
public:
    SurvivorWorld(std::size_t maxEnemies, std::size_t maxBullets, std::size_t maxBlasts);

    // Sizes depend on the screen; a new size starts a new round.
    void setScreen(float width, float height);
    void reset();                       // new round: ship at (0, 0), no enemies
    void beginTick();                   // remember previous state for interpolation
    void setJoystick(float x, float y); // -1..1 each, from the on-screen stick
    void update(float dt);

    bool hasScreen() const { return m_width > 0.0F && m_height > 0.0F; }
    bool isGameOver() const { return m_gameOver; }
    int kills() const { return m_kills; }
    float timeSurvived() const { return m_time; }

    float playerX() const { return m_x; }
    float playerY() const { return m_y; }
    float playerPrevX() const { return m_prevX; }
    float playerPrevY() const { return m_prevY; }
    float playerAngle() const { return m_angle; }
    float playerPrevAngle() const { return m_prevAngle; }
    float playerSize() const { return m_size; }

    const SurvivorEnemies &enemies() const { return m_enemies; }
    const SurvivorBullets &bullets() const { return m_bullets; }
    const Explosions &blasts() const { return m_blasts; }

private:
    void movePlayer(float dt);
    void autoFire(float dt);
    void bulletsHitEnemies();

    float m_width = 0.0F;
    float m_height = 0.0F;
    float m_size = 0.0F;       // ship sprite size (px)
    float m_maxSpeed = 0.0F;   // px per second
    float m_joyX = 0.0F;
    float m_joyY = 0.0F;
    float m_x = 0.0F, m_y = 0.0F, m_prevX = 0.0F, m_prevY = 0.0F;
    float m_angle = 0.0F, m_prevAngle = 0.0F;

    SurvivorEnemies m_enemies;
    SurvivorBullets m_bullets;
    Explosions m_blasts;       // UFO destruction animations (shared class with Alien Shooter)
    std::mt19937 m_rng;
    float m_fireTimer = 0.0F;
    float m_time = 0.0F;       // seconds survived this round
    int m_kills = 0;
    bool m_gameOver = false;
};

#endif //MY_CHATROOM_SURVIVORWORLD_H
