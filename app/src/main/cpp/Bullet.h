#ifndef MY_CHATROOM_BULLET_H
#define MY_CHATROOM_BULLET_H

#include <cstddef>
#include <vector>

struct BulletState {
    float x = 0.0f;
    float y = 0.0f;
    float prevX = 0.0f;   // position at the start of the tick, for interpolation
    float prevY = 0.0f;   // also the bottom of the path travelled this tick
    bool active = false;
};

// A fixed-size pool of player bullets, fired automatically while the game runs.
// All units are pixels and seconds. Only the logic thread touches Bullets.
class Bullets {
public:
    explicit Bullets(std::size_t maxCount);

    void reset(float screenWidth, float screenHeight);
    void beginTick();

    // Moves every bullet up, drops the ones that left the screen, and every
    // kFireIntervalSec fires a volley of `lanes` bullets side by side,
    // centred on (gunX, gunY). lanes = weapon level (1, 2 or 3).
    void update(float dt, float gunX, float gunY, int lanes);

    std::vector<BulletState> &bullets() { return m_bullets; }
    const std::vector<BulletState> &bullets() const { return m_bullets; }
    float size() const { return m_size; }   // drawn width; length is 3x this
    float hitRadius() const;

private:
    BulletState *findInactive();

    std::vector<BulletState> m_bullets;
    float m_size = 0.0f;
    float m_speed = 0.0f;       // pixels per second, upwards
    float m_fireTimer = 0.0f;   // seconds since the last shot
};

#endif //MY_CHATROOM_BULLET_H
