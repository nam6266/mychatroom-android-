#ifndef MY_CHATROOM_ALIEN_H
#define MY_CHATROOM_ALIEN_H

#include <cstddef>
#include <random>
#include <vector>

struct AlienState {
    float x = 0.0F;
    float y = 0.0F;
    float prevX = 0.0F;     // position at the start of the tick, for interpolation
    float prevY = 0.0F;
    float age = 0.0F;       // seconds alive: drives the path and the hover animation
    // flight path (picked at spawn, so every alien flies a little differently)
    float centerX = 0.0F;   // middle of its side-to-side sway
    float swayAmp = 0.0F;   // pixels left/right of centerX
    float swayFreq = 0.0F;  // sways per second
    float bobAmp = 0.0F;    // pixels up/down, makes it loop instead of only zig-zag
    float phase = 0.0F;
    float entryY = 0.0F;
    float fallSpeed = 0.0F; // pixels per second: slow, it drifts down while weaving
    int hp = 0;             // hits left before it's destroyed (10-15 at spawn)
    float hitFlash = 0.0F;  // seconds left of the orange "I've been hit" flash
    bool active = false;
};

// What a bullet did to an alien.
enum class AlienHit { None, Damaged, Destroyed };

// Flying UFO aliens: they swoop in from the top and weave across the screen in
// figure-8 loops while slowly drifting down. Each one takes 10-15 hits.
// All units are pixels and seconds. Only the logic thread touches Aliens.
class Aliens {
public:
    explicit Aliens(std::size_t maxCount);

    void reset(float screenWidth, float screenHeight);
    void beginTick();
    void update(float dt, std::mt19937 &rng);   // moves aliens, spawns new ones

    // Bullet hit test (bullet path = segment x, [yTop, yBottom]). The first
    // alien it touches loses 1 hp and flashes; at 0 hp it's destroyed and its
    // position is reported (for the explosion).
    AlienHit hitBySegment(float x, float yTop, float yBottom, float radius,
                          float *hitX = nullptr, float *hitY = nullptr);

    // Laser support. The beam is the vertical strip x +/- halfWidth above
    // gunY. Returns the index of the target nearest the gun (lowest on screen,
    // since the beam goes up) that touches the strip, or -1; *contactY is the
    // bottom of its hitbox, where the beam should stop.
    int firstInBeam(float x, float halfWidth, float gunY, float *contactY) const;
    // One hit on alien `index`: -1 hp and a flash, destroyed at 0 hp (then its
    // position is reported for the explosion). Used by bullets and the laser.
    AlienHit damage(int index, float *hitX, float *hitY);

    // True if an alien touches the player's capsule hitbox.
    bool collidesWith(float x, float yTop, float yBottom, float radius) const;

    const std::vector<AlienState> &aliens() const { return m_aliens; }
    float size() const { return m_size; }   // drawn width of the UFO

private:
    bool touches(const AlienState &a, float x, float yTop, float yBottom, float radius) const;
    void spawn(AlienState &a, std::mt19937 &rng) const;
    void place(AlienState &a) const;        // position from the path at a.age

    std::vector<AlienState> m_aliens;
    float m_screenWidth = 0.0F;
    float m_screenHeight = 0.0F;
    float m_size = 0.0F;
    float m_spawnTimer = 0.0F;   // seconds until the next alien
};

#endif //MY_CHATROOM_ALIEN_H
