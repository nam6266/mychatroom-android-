#ifndef MY_CHATROOM_SURVIVORLOOP_H
#define MY_CHATROOM_SURVIVORLOOP_H

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <mutex>
#include <thread>
#include <vector>
#include "SurvivorWorld.h"

// Runs SurvivorWorld on its own thread at a fixed tick rate - the same design
// as GameLoop (Alien Shooter): input from the UI thread is queued under one
// mutex, the latest state is published under another, so the two threads never
// touch the world at the same time.
class SurvivorLoop {
public:
    enum State : int { Running = 0, Paused = 1, GameOver = 2, LevelUp = 3 };

    // Layout copied out by copySnapshotTo(). Positions are WORLD coordinates (px):
    //   [0] state  [1] playerX  [2] playerY  [3] playerAngle (rad, 0 = up, clockwise)
    //   [4] playerSize  [5] enemySize  [6] enemyCount  [7] bulletThickness  [8] bulletCount
    //   [9] blastSize  [10] blastCount  [11] kills  [12] seconds survived
    //   [13] level  [14] exp  [15] expNeeded  [16] laserLevel  [17] ultimate (0 none, 1 barrage, 2 beams)
    //   [18] offered upgrade (0 none, 1 laser, 2 ultimate)  [19] bulletLength
    //   [20] beamAngle  [21] beamStart  [22] beamLength  [23] beamThickness
    //   [24] orbSize  [25] orbCount  [26] nukeFlash (1 -> 0)
    //   [27..] enemy x,y,animTime,hitFlash(0/1) | bullet x,y,angle | blast x,y,progress
    //          | orb x,y,kind (0 green EXP, 1 red nuke)
    static constexpr int kHeaderSize = 27;

    SurvivorLoop(int ticksPerSecond, std::size_t maxEnemies, std::size_t maxBullets,
                 std::size_t maxBlasts, std::size_t maxOrbs);
    ~SurvivorLoop();

    SurvivorLoop(const SurvivorLoop &) = delete;
    SurvivorLoop &operator=(const SurvivorLoop &) = delete;

    void start();
    void stop();
    void setPaused(bool paused);

    // Called from the UI thread.
    void setScreen(float width, float height);
    void setJoystick(float x, float y);
    void restart();
    // The player tapped the level-up card. `upgrade` = the offered value from [18].
    void chooseUpgrade(int upgrade);

    // Called from the UI thread once per rendered frame. Returns floats written.
    int copySnapshotTo(float *out, int capacity) const;

private:
    using Clock = std::chrono::steady_clock;

    struct PendingInput {
        float width = 0.0F, height = 0.0F;
        bool hasScreen = false;
        float joyX = 0.0F, joyY = 0.0F;   // latest stick position (not summed: it's a state)
        bool restart = false;
        int upgrade = 0;                  // a level-up card was answered (0 = none)
    };

    struct Snapshot {
        int state = Running;
        float x = 0.0F, prevX = 0.0F, y = 0.0F, prevY = 0.0F;
        float angle = 0.0F, prevAngle = 0.0F;
        float size = 0.0F;
        float enemySize = 0.0F, bulletSize = 0.0F, blastSize = 0.0F, blastDuration = 1.0F;
        std::vector<float> enemies;   // x, prevX, y, prevY, age, flashing
        std::vector<float> bullets;   // x, prevX, y, prevY, angle
        std::vector<float> blasts;    // x, y, age
        std::vector<float> orbs;      // x, prevX, y, prevY, kind
        int kills = 0;
        float time = 0.0F;
        int level = 1;
        int exp = 0;
        int expNeeded = 1;
        int laserLevel = 1;
        int ultimate = 0;             // SurvivorWorld::UltimateStyle
        int offered = 0;
        float bulletLength = 0.0F;
        float beamAngle = 0.0F;
        float prevBeamAngle = 0.0F;
        float beamStart = 0.0F;
        float beamLength = 0.0F;
        float beamThickness = 0.0F;
        float orbSize = 0.0F;
        float nukeFlash = 0.0F;
        Clock::time_point tickTime{};
    };

    void run();
    void tick(float dt);
    void publish();

    const int m_ticksPerSecond;
    const Clock::duration m_tickDuration;
    SurvivorWorld m_world;   // logic thread only

    std::thread m_thread;
    std::atomic<bool> m_running{false};
    std::atomic<bool> m_paused{false};
    std::mutex m_wakeMutex;
    std::condition_variable m_wakeCv;

    std::mutex m_inputMutex;
    PendingInput m_pending;

    mutable std::mutex m_snapshotMutex;
    Snapshot m_snapshot;
};

#endif //MY_CHATROOM_SURVIVORLOOP_H
