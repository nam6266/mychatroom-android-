#ifndef MY_CHATROOM_EXPLOSION_H
#define MY_CHATROOM_EXPLOSION_H

#include <cstddef>
#include <random>
#include <vector>

struct ExplosionState {
    float x = 0.0f;
    float y = 0.0f;
    float age = 0.0f;    // seconds since it started
    float seed = 0.0f;   // 0..1, makes every explosion look a little different
    bool active = false;
};

// Short-lived explosion effects where meteors were destroyed. Pure timing and
// position: what an explosion looks like is decided by the renderer, which
// gets a 0..1 progress for each one. Only the logic thread touches Explosions.
class Explosions {
public:
    // durationSec: how long one explosion plays; sizeOfScreenWidth: its size.
    Explosions(std::size_t maxCount, float durationSec, float sizeOfScreenWidth);

    void reset(float screenWidth);
    void spawn(float x, float y, std::mt19937 &rng);   // reuses the oldest if full
    void update(float dt);

    const std::vector<ExplosionState> &explosions() const { return m_explosions; }
    float size() const { return m_size; }
    float duration() const { return m_duration; }   // seconds

private:
    std::vector<ExplosionState> m_explosions;
    float m_duration;
    float m_sizeOfScreenWidth;
    float m_size = 0.0f;
};

#endif //MY_CHATROOM_EXPLOSION_H
