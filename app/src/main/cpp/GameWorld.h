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

    bool hasScreen() const { return m_width > 0.0f && m_height > 0.0f; }
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

private:
    Player m_player;
    Enemy m_enemy;
    Bullets m_bullets;
    PowerUps m_powerUps;
    Explosions m_explosions;     // meteor blasts
    Aliens m_aliens;
    Explosions m_alienBlasts;    // UFO destruction (its own, longer animation)
    std::mt19937 m_rng;
    float m_width = 0.0f;
    float m_height = 0.0f;
    int m_score = 0;
    int m_weaponLevel = 1;

    void maybeDropUpgrade(float x, float y);
    bool m_gameOver = false;
};

#endif //MY_CHATROOM_GAMEWORLD_H
