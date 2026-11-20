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

// Player laser shots for Space Survivor (a fixed pool). Unlike Alien Shooter
// they can fly in any direction, so each one keeps its own velocity, and they
// expire after a while because the map never ends.
//
// Laser level 1..6 (kMaxLevel) only changes how thick the shot is: the six
// sprites laser_lv1..6 get thicker, and the hitbox grows with them. Damage and
// fire rate per level live in SurvivorWorld.
class SurvivorBullets {
public:
    static constexpr int kMaxLevel = 6;

    explicit SurvivorBullets(std::size_t maxCount);

    void reset(float screenWidth, float screenHeight);
    void setLevel(int level);                            // 1..kMaxLevel, clamped
    void beginTick();
    void update(float dt);                               // move + expire
    // dir = unit vector. speedScale > 1 = a faster shot that flies the same distance.
    void fire(float x, float y, float dirX, float dirY, float speedScale = 1.0F);

    std::vector<SurvivorBulletState> &bullets() { return m_bullets; }
    const std::vector<SurvivorBulletState> &bullets() const { return m_bullets; }
    int level() const { return m_level; }
    float length() const { return m_length; }            // drawn length along the flight
    float thickness() const;                             // drawn thickness: grows with the level
    float hitRadius() const;
    float headOffset() const;                            // from the centre to the glowing tip
    float range() const { return m_range; }              // how far a bullet flies

private:
    std::vector<SurvivorBulletState> m_bullets;
    int m_level = 1;
    float m_length = 0.0F;
    float m_speed = 0.0F;
    float m_range = 0.0F;
};

#endif //MY_CHATROOM_SURVIVORBULLET_H
