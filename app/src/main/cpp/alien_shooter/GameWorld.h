#ifndef MY_CHATROOM_GAMEWORLD_H
#define MY_CHATROOM_GAMEWORLD_H

#include <cstddef>
#include <random>
#include "Player.h"
#include "Enemy.h"
#include "Bullet.h"
#include "PowerUp.h"
#include "Explosion.h"
#include "Alien.h"

// Pure game rules: no threads, no JNI. Easy to unit test on a desktop.
class GameWorld {
public:
    static constexpr int kMaxWeaponLevel = 3;   // 1, 2 or 3 bullets per volley

    GameWorld(std::size_t maxEnemies, std::size_t maxBullets, std::size_t maxPowerUps,
              std::size_t maxExplosions, std::size_t maxAliens, std::size_t maxAlienBlasts);

    // Resets the round when the size actually changes (first layout, rotation).
    void setScreen(float width, float height);
    void reset();

    void beginTick();             // remember previous positions for interpolation
    void movePlayerBy(float dx);
    void update(float dt);

    bool hasScreen() const { return m_width > 0.0F && m_height > 0.0F; }
    bool isGameOver() const { return m_gameOver; }
    int score() const { return m_score; }
    const Player &player() const { return m_player; }
    const Enemy &enemy() const { return m_enemy; }
    const Bullets &bullets() const { return m_bullets; }
    const PowerUps &powerUps() const { return m_powerUps; }
    const Explosions &explosions() const { return m_explosions; }
    const Aliens &aliens() const { return m_aliens; }
    const Explosions &alienBlasts() const { return m_alienBlasts; }
    int weaponLevel() const { return m_weaponLevel; }
    // Laser: seconds left (0 = off), where the beam ends, and its width.
    float laserTimeLeft() const { return m_laserTime; }
    float laserTopY() const { return m_laserTopY; }
    float laserWidth() const;

private:
    Player m_player;
    Enemy m_enemy;
    Bullets m_bullets;
    PowerUps m_powerUps;
    Explosions m_explosions;     // meteor blasts
    Aliens m_aliens;
    Explosions m_alienBlasts;    // UFO destruction (its own, longer animation)
    std::mt19937 m_rng;
    float m_width = 0.0F;
    float m_height = 0.0F;
    int m_score = 0;
    int m_weaponLevel = 1;

    float m_laserTime = 0.0F;        // seconds of laser left
    float m_laserTopY = 0.0F;        // where the beam ends this tick
    float m_laserDamageTimer = 0.0F; // counts up to the next laser hit

    void maybeDropUpgrade(float x, float y);
    void updateLaser(float dt, float gunY);
    void onMeteorKilled(float x, float y);
    void onAlienHit(AlienHit hit, float x, float y);
    bool m_gameOver = false;
};

#endif //MY_CHATROOM_GAMEWORLD_H
