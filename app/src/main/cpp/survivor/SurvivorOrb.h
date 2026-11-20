#ifndef MY_CHATROOM_SURVIVORORB_H
#define MY_CHATROOM_SURVIVORORB_H

#include <cstddef>
#include <vector>

enum class OrbKind : int { Exp = 0, Nuke = 1 };

struct SurvivorOrbState {
    OrbKind kind = OrbKind::Exp;
    float x = 0.0F;
    float y = 0.0F;
    float prevX = 0.0F;   // last tick, for smooth drawing
    float prevY = 0.0F;
    float age = 0.0F;     // seconds on the ground: the oldest one is recycled when the pool is full
    bool active = false;
};

// Orbs for Space Survivor, as a fixed pool. Every killed alien drops a green
// EXP orb; once in a while a red Nuke orb too. Orbs lie still until the ship
// comes close; then they fly to it and are collected. World coordinates,
// pixels, seconds.
class SurvivorOrbs {
public:
    struct Collected {
        int exp = 0;     // green orbs picked up this tick
        int nukes = 0;   // red orbs picked up this tick
    };

    explicit SurvivorOrbs(std::size_t maxCount);

    void reset(float screenWidth);
    void beginTick();
    void spawn(float x, float y, OrbKind kind = OrbKind::Exp);
    // Pulls nearby orbs towards the player and collects the ones that reach it.
    Collected update(float dt, float playerX, float playerY);
    bool hasNukeOnGround() const;

    const std::vector<SurvivorOrbState> &orbs() const { return m_orbs; }
    float size() const { return m_size; }   // drawn diameter

private:
    std::vector<SurvivorOrbState> m_orbs;
    float m_size = 0.0F;
    float m_magnetRange = 0.0F;   // orbs closer than this fly to the ship
    float m_flySpeed = 0.0F;
    float m_collectRange = 0.0F;
};

#endif //MY_CHATROOM_SURVIVORORB_H
