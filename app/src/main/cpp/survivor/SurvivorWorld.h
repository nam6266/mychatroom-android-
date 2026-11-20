#ifndef MY_CHATROOM_SURVIVORWORLD_H
#define MY_CHATROOM_SURVIVORWORLD_H

#include <cstddef>
#include <random>
#include <vector>
#include "SurvivorEnemy.h"
#include "SurvivorBullet.h"
#include "SurvivorOrb.h"
#include "Explosion.h"

// Space Survivor: game rules only (no threads, no JNI), like GameWorld for
// Alien Shooter. The world is an endless plane in pixels; the camera follows
// the ship, so the ship's world position is also where the camera looks.
//
// Angles are radians, 0 = facing up the screen, growing clockwise
// (screen y points down), so the renderer can rotate the sprite directly.
//
// Progression: every killed alien drops an EXP orb. Level n needs 5 x n EXP.
// Each level-up offers one upgrade and the game waits (isChoosingUpgrade())
// until chooseUpgrade() is called: first the laser goes from level 1 to 6,
// then the ultimate replaces the aimed shots (see kUltimateStyle in the .cpp).
class SurvivorWorld {
public:
    enum class Upgrade : int { None = 0, Laser = 1, Ultimate = 2 };
    // What the ultimate looks like. Barrage is used now; Beams is kept for later.
    enum class UltimateStyle : int { None = 0, Barrage = 1, Beams = 2 };
    static constexpr int kBeamCount = 4;

    SurvivorWorld(std::size_t maxEnemies, std::size_t maxBullets, std::size_t maxBlasts,
                  std::size_t maxOrbs);

    // Sizes depend on the screen; a new size starts a new round.
    void setScreen(float width, float height);
    void reset();                       // new round: ship at (0, 0), no enemies
    void beginTick();                   // remember previous state for interpolation
    void setJoystick(float x, float y); // -1..1 each, from the on-screen stick
    void update(float dt);
    // The player picked the offered upgrade on the level-up card. Anything
    // other than offeredUpgrade() is ignored.
    void chooseUpgrade(Upgrade upgrade);

    bool hasScreen() const { return m_width > 0.0F && m_height > 0.0F; }
    bool isGameOver() const { return m_gameOver; }
    int kills() const { return m_kills; }
    float timeSurvived() const { return m_time; }

    float playerX() const { return m_x; }
    float playerY() const { return m_y; }
    float playerPrevX() const { return m_prevX; }
    float playerPrevY() const { return m_prevY; }
    float playerAngle() const { return m_angle; }
    float playerPrevAngle() const { return m_prevAngle; }
    float playerSize() const { return m_size; }
    // 1 the moment a red Nuke orb is picked up, fading to 0: a white screen flash.
    float nukeFlash() const { return m_nukeFlash; }

    // Progression
    int level() const { return m_level; }
    int exp() const { return m_exp; }
    int expNeeded() const;                       // EXP to reach the next level
    int laserLevel() const { return m_bullets.level(); }
    bool hasUltimate() const { return m_ultimate; }
    UltimateStyle ultimateStyle() const;   // None until the ultimate is taken
    bool isChoosingUpgrade() const { return m_pendingUpgrades > 0; }
    Upgrade offeredUpgrade() const;

    // Ultimate beams: kBeamCount lasers around the ship, 90 degrees apart, spinning.
    float beamAngle() const { return m_beamAngle; }   // beam 0; 0 = up, clockwise
    float prevBeamAngle() const { return m_prevBeamAngle; }
    float beamStart() const;                          // distance from the ship's centre
    float beamLength() const;
    float beamThickness() const;

    const SurvivorEnemies &enemies() const { return m_enemies; }
    const SurvivorBullets &bullets() const { return m_bullets; }
    const Explosions &blasts() const { return m_blasts; }
    const SurvivorOrbs &orbs() const { return m_orbs; }

private:
    void movePlayer(float dt);
    void autoFire(float dt);
    void bulletsHitEnemies();
    void updateBeams(float dt);      // ultimate style Beams
    void updateBarrage(float dt);    // ultimate style Barrage
    // `byNuke`: killed by the red orb itself, which then can't drop another red orb.
    void onEnemyKilled(float x, float y, bool byNuke = false);
    void detonateNuke();
    bool isOnScreen(float x, float y) const;   // inside the visible area (the camera follows the ship)
    void addExp(int amount);
    int upgradesLeft() const;   // upgrades not taken yet and not already waiting on a card

    float m_width = 0.0F;
    float m_height = 0.0F;
    float m_size = 0.0F;       // ship sprite size (px)
    float m_maxSpeed = 0.0F;   // px per second
    float m_joyX = 0.0F;
    float m_joyY = 0.0F;
    float m_x = 0.0F, m_y = 0.0F, m_prevX = 0.0F, m_prevY = 0.0F;
    float m_angle = 0.0F, m_prevAngle = 0.0F;

    SurvivorEnemies m_enemies;
    SurvivorBullets m_bullets;
    Explosions m_blasts;       // UFO destruction animations (shared class with Alien Shooter)
    SurvivorOrbs m_orbs;
    std::vector<int> m_beamHits;   // reused every beam pulse: no allocation while playing
    std::vector<float> m_nukeKills; // x, y of every alien a nuke destroyed (reused)
    float m_nukeFlash = 0.0F;
    std::mt19937 m_rng;
    float m_fireTimer = 0.0F;
    float m_time = 0.0F;       // seconds survived this round
    int m_kills = 0;
    bool m_gameOver = false;

    int m_level = 1;
    int m_exp = 0;               // EXP collected towards the next level
    int m_pendingUpgrades = 0;   // level-ups whose card hasn't been answered yet
    bool m_ultimate = false;
    float m_beamAngle = 0.0F;
    float m_prevBeamAngle = 0.0F;
    float m_beamTimer = 0.0F;
    float m_barrageAngle = 0.0F;     // direction of the first shot in the next volley
    float m_barrageTimer = 0.0F;
};

#endif //MY_CHATROOM_SURVIVORWORLD_H
