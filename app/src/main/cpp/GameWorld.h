#ifndef MY_CHATROOM_GAMEWORLD_H
#define MY_CHATROOM_GAMEWORLD_H

#include <cstddef>
#include <random>
#include "Player.h"
#include "Enemy.h"

// Pure game rules: no threads, no JNI. Easy to unit test on a desktop.
class GameWorld {
public:
    explicit GameWorld(std::size_t maxEnemies);

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

private:
    Player m_player;
    Enemy m_enemy;
    std::mt19937 m_rng;
    float m_width = 0.0f;
    float m_height = 0.0f;
    int m_score = 0;
    bool m_gameOver = false;
};

#endif //MY_CHATROOM_GAMEWORLD_H
