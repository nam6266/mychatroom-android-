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
    //   [5] enemySize  [6] enemyCount  [7..] enemy x,y pairs
    static constexpr int kHeaderSize = 7;

    GameLoop(int ticksPerSecond, std::size_t maxEnemies);
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
        float dx = 0.0f;
        float width = 0.0f;
        float height = 0.0f;
        bool hasScreen = false;
        bool restart = false;
    };

    struct Snapshot {
        int state = Running;
        int score = 0;
        float playerX = 0.0f, playerPrevX = 0.0f, playerY = 0.0f, playerSize = 0.0f;
        float enemySize = 0.0f;
        std::vector<float> enemies;   // x, prevX, y, prevY per active enemy
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
