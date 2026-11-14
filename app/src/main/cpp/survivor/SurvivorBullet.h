#ifndef MY_CHATROOM_SURVIVORBULLET_H
#define MY_CHATROOM_SURVIVORBULLET_H

#include <cstddef>
#include <vector>

struct SurvivorBulletState {
    float x = 0.0F, y = 0.0F;
    float prevX = 0.0F, prevY = 0.0F;
    float vx = 0.0F, vy = 0.0F;   // velocity (px/s): bullets fly in any direction here
    float life = 0.0F;            // seconds left before it fades out
    bool active = false;
};

// Player bullets for Space Survivor (a fixed pool). Unlike Alien Shooter they
// can fly in any direction, so each one keeps its own velocity, and they
// expire after a while because the map never ends.
class SurvivorBullets {
public:
    explicit SurvivorBullets(std::size_t maxCount);

    void reset(float screenWidth, float screenHeight);
    void beginTick();
    void update(float dt);                               // move + expire
    void fire(float x, float y, float dirX, float dirY);  // dir = unit vector

    std::vector<SurvivorBulletState> &bullets() { return m_bullets; }
    const std::vector<SurvivorBulletState> &bullets() const { return m_bullets; }
    float size() const { return m_size; }
    float hitRadius() const { return m_size * 0.6F; }
    float range() const { return m_range; }              // how far a bullet flies

private:
    std::vector<SurvivorBulletState> m_bullets;
    float m_size = 0.0F, m_speed = 0.0F, m_range = 0.0F;
};

#endif //MY_CHATROOM_SURVIVORBULLET_H
