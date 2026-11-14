#ifndef MY_CHATROOM_SURVIVORENEMY_H
#define MY_CHATROOM_SURVIVORENEMY_H

#include <cstddef>
#include <random>
#include <vector>
#include "SurvivorGrid.h"

struct SurvivorEnemyState {
    float x = 0.0F, y = 0.0F;           // world position (px)
    float prevX = 0.0F, prevY = 0.0F;   // last tick, for smooth drawing
    float age = 0.0F;                   // seconds alive: drives the hover animation
    float hitFlash = 0.0F;              // seconds left of the orange "hit" flash
    int hp = 0;
    bool active = false;
};

// Space Survivor's UFO aliens, as a fixed pool (same pattern as Enemy/Aliens in
// Alien Shooter). They appear just outside the screen in a random direction,
// fly slowly straight at the player, and push each other apart so they form a
// swarm instead of stacking on one spot. World coordinates, pixels, seconds.
class SurvivorEnemies {
public:
    explicit SurvivorEnemies(std::size_t maxCount);

    void reset(float screenWidth, float screenHeight);
    void beginTick();

    // Spawns new aliens off-screen around (targetX, targetY), moves every alien
    // towards it, and separates overlapping ones. `elapsed` = seconds survived,
    // used to spawn faster as the game goes on.
    void update(float dt, float targetX, float targetY, float elapsed, std::mt19937 &rng);

    // Index of the nearest alien within maxDistance of (x, y), or -1.
    int nearest(float x, float y, float maxDistance) const;
    // Index of an alien whose hitbox touches a circle at (x, y) of `radius`, or -1.
    // Uses the grid, so it only looks at aliens near (x, y). Valid after update().
    int touching(float x, float y, float radius) const;
    // One hit on alien `index`: -1 hp and a flash. Returns true if that destroyed
    // it, and then reports where it was (for the explosion).
    bool damage(int index, float *hitX, float *hitY);

    const std::vector<SurvivorEnemyState> &enemies() const { return m_enemies; }
    float size() const { return m_size; }        // drawn UFO width
    float hitRadius() const;

private:
    void spawn(SurvivorEnemyState &e, float targetX, float targetY, std::mt19937 &rng) const;
    void separate();
    void rebuildGrid(float centerX, float centerY);
    float spawnDistance() const;

    SurvivorGrid m_grid;   // where every alien is this tick; rebuilt in update()

    std::vector<SurvivorEnemyState> m_enemies;
    float m_screenWidth = 0.0F, m_screenHeight = 0.0F;
    float m_size = 0.0F;
    float m_speed = 0.0F;
    float m_spawnTimer = 0.0F;
};

#endif //MY_CHATROOM_SURVIVORENEMY_H
