#ifndef MY_CHATROOM_GAMELOOP_H
#define MY_CHATROOM_GAMELOOP_H

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstddef>
#include <mutex>
#include <thread>
#include <vector>
#include "GameWorld.h"

// Runs GameWorld on its own thread at a fixed tick rate.
//
//   UI thread (Kotlin, ~60 fps)          logic thread (this class)
//   ---------------------------          --------------------------
//   setScreen / movePlayerBy / restart -> queued, applied at the next tick
//   writeSnapshot()                    <- latest published state, interpolated
//
// The two threads never share GameWorld: input goes through m_inputMutex and
// output through m_snapshotMutex, so each lock is held for microseconds.
class GameLoop {
public:
    enum State : int { Running = 0, Paused = 1, GameOver = 2 };

    // Layout written by writeSnapshot():
    //   [0] state  [1] score  [2] playerX  [3] playerY  [4] playerSize
    //   [5] enemySize  [6] enemyCount  [7] bulletSize  [8] bulletCount
    //   [9] powerUpSize  [10] powerUpCount  [11] weaponLevel
    //   [12] explosionSize  [13] explosionCount
    //   [14] alienSize  [15] alienCount  [16] alienBlastSize  [17] alienBlastCount
    //   [18] laserTimeLeft (s, 0 = off)  [19] laserTopY  [20] laserWidth
    //   [21..] enemy x,y | bullet x,y | power-up x,y,kind(0 gun, 1 laser) |
    //          explosion x,y,progress,seed | alien x,y,animTime,hitFlash(0/1) |
    //          alien blast x,y,progress,seed
    static constexpr int kHeaderSize = 21;

    GameLoop(int ticksPerSecond, std::size_t maxEnemies, std::size_t maxBullets,
             std::size_t maxPowerUps, std::size_t maxExplosions, std::size_t maxAliens,
             std::size_t maxAlienBlasts);
    ~GameLoop();

    GameLoop(const GameLoop &) = delete;
    GameLoop &operator=(const GameLoop &) = delete;

    void start();
    void stop();
    void setPaused(bool paused);

    // Called from the UI thread.
    void setScreen(float width, float height);
    void movePlayerBy(float dx);
    void restart();

    // Called from the UI thread once per rendered frame. Returns floats written.
    int writeSnapshot(float *out, int capacity) const;

private:
    using Clock = std::chrono::steady_clock;

    struct PendingInput {
        float dx = 0.0F;
        float width = 0.0F;
        float height = 0.0F;
        bool hasScreen = false;
        bool restart = false;
    };

    struct Snapshot {
        int state = Running;
        int score = 0;
        float playerX = 0.0F, playerPrevX = 0.0F, playerY = 0.0F, playerSize = 0.0F;
        float enemySize = 0.0F;
        std::vector<float> enemies;   // x, prevX, y, prevY per active enemy
        float bulletSize = 0.0F;
        std::vector<float> bullets;   // x, prevX, y, prevY per active bullet
        float powerUpSize = 0.0F;
        std::vector<float> powerUps;  // x, prevX, y, prevY, kind per active power-up
        int weaponLevel = 1;
        float explosionSize = 0.0F;
        std::vector<float> explosions;   // x, y, age, seed per active explosion
        float explosionDuration = 1.0F;
        float alienSize = 0.0F;
        std::vector<float> aliens;       // x, prevX, y, prevY, age, flashing per active alien
        float alienBlastSize = 0.0F;
        std::vector<float> alienBlasts;  // x, y, age, seed per active alien blast
        float alienBlastDuration = 1.0F;
        float laserTimeLeft = 0.0F;
        float laserTopY = 0.0F;
        float laserWidth = 0.0F;
        Clock::time_point tickTime{};
    };

    void run();
    void tick(float dt);
    void publish();

    const int m_ticksPerSecond;
    const Clock::duration m_tickDuration;
    GameWorld m_world;                     // logic thread only

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

#endif //MY_CHATROOM_GAMELOOP_H
