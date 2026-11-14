#ifndef MY_CHATROOM_POWERUP_H
#define MY_CHATROOM_POWERUP_H

#include <cstddef>
#include <vector>

// What an orb gives you when you touch it.
enum class PowerUpKind : int { GunUpgrade = 0, Laser = 1 };

struct PowerUpState {
    float x = 0.0F;
    float y = 0.0F;
    float prevX = 0.0F;   // position at the start of the tick, for interpolation
    float prevY = 0.0F;
    PowerUpKind kind = PowerUpKind::GunUpgrade;
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

    // Drops a new orb of `kind` at (x, y). Does nothing if the pool is full.
    void spawn(float x, float y, PowerUpKind kind);

    // Moves orbs down and removes the ones that left the screen.
    void update(float dt);

    // Collects every orb touching the player's capsule hitbox
    // (segment x, [yTop, yBottom] widened by radius) and counts them per kind.
    void collect(float x, float yTop, float yBottom, float radius,
                 int &gunUpgrades, int &lasers);

    int activeCount(PowerUpKind kind) const;   // orbs of this kind still falling
    const std::vector<PowerUpState> &powerUps() const { return m_powerUps; }
    float size() const { return m_size; }

private:
    std::vector<PowerUpState> m_powerUps;
    float m_screenHeight = 0.0F;
    float m_size = 0.0F;
    float m_speed = 0.0F;   // pixels per second, downwards
};

#endif //MY_CHATROOM_POWERUP_H
