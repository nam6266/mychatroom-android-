#ifndef MY_CHATROOM_POWERUP_H
#define MY_CHATROOM_POWERUP_H

#include <cstddef>
#include <vector>

struct PowerUpState {
    float x = 0.0f;
    float y = 0.0f;
    float prevX = 0.0f;   // position at the start of the tick, for interpolation
    float prevY = 0.0f;
    bool active = false;
};

// Weapon-upgrade orbs dropped by destroyed meteors. They fall slowly and are
// collected by touching them. All units are pixels and seconds.
// Only the logic thread touches PowerUps.
class PowerUps {
public:
    explicit PowerUps(std::size_t maxCount);

    void reset(float screenWidth, float screenHeight);
    void beginTick();

    // Drops a new orb at (x, y). Does nothing if the pool is full.
    void spawn(float x, float y);

    // Moves orbs down and removes the ones that left the screen.
    void update(float dt);

    // Collects every orb touching the player's capsule hitbox
    // (segment x, [yTop, yBottom] widened by radius). Returns how many.
    int collect(float x, float yTop, float yBottom, float radius);

    int activeCount() const;
    const std::vector<PowerUpState> &powerUps() const { return m_powerUps; }
    float size() const { return m_size; }

private:
    std::vector<PowerUpState> m_powerUps;
    float m_screenHeight = 0.0f;
    float m_size = 0.0f;
    float m_speed = 0.0f;   // pixels per second, downwards
};

#endif //MY_CHATROOM_POWERUP_H
