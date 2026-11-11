//
// Created by OS on 04/23/25.
//

#ifndef MY_CHATROOM_ENEMY_H
#define MY_CHATROOM_ENEMY_H

#include <cstddef>
#include <random>
#include <vector>

struct EnemyState {
    float x = 0.0f;
    float y = 0.0f;
    float prevX = 0.0f;   // position at the start of the tick, for interpolation
    float prevY = 0.0f;
    bool active = false;
};

// A fixed-size pool of falling meteors. All units are pixels and seconds.
// Only the logic thread touches an Enemy.
class Enemy {
public:
    explicit Enemy(std::size_t maxCount);

    void reset(float screenWidth, float screenHeight);
    void beginTick();

    // Advances every active meteor by dt seconds and spawns new ones.
    // Returns how many meteors fell off the bottom this tick (= dodged).
    int update(float dt, std::mt19937 &rng);

    // True if any meteor touches the capsule x, [yTop, yBottom] widened by radius.
    bool collidesWith(float x, float yTop, float yBottom, float radius) const;

    // Bullet hit test. The bullet's path this tick is the vertical segment
    // x, [yTop, yBottom]; testing the whole path means a fast bullet can't
    // jump over a meteor between two ticks. Destroys the first meteor it
    // touches and returns true; its position is written to hitX/hitY if given
    // (used to drop an upgrade where the meteor was).
    bool hitBySegment(float x, float yTop, float yBottom, float radius,
                      float *hitX = nullptr, float *hitY = nullptr);

    const std::vector<EnemyState> &enemies() const { return m_enemies; }
    std::size_t maxCount() const { return m_enemies.size(); }
    float size() const { return m_size; }

private:
    EnemyState *findInactive();
    bool touches(const EnemyState &enemy, float x, float yTop, float yBottom, float radius) const;
    void spawn(EnemyState &enemy, std::mt19937 &rng) const;

    std::vector<EnemyState> m_enemies;
    float m_screenWidth = 0.0f;
    float m_screenHeight = 0.0f;
    float m_size = 0.0f;
    float m_speed = 0.0f;        // pixels per second, grows over time
    float m_spawnTimer = 0.0f;   // seconds since the last spawn
};

#endif //MY_CHATROOM_ENEMY_H
