#include "GameLoop.h"
#include <algorithm>

namespace {
    // If the thread falls this far behind (debugger, app in background),
    // drop the missed ticks instead of fast-forwarding the game.
    constexpr auto kMaxLag = std::chrono::milliseconds(250);

    float lerp(float from, float to, float t) { return from + (to - from) * t; }
}

GameLoop::GameLoop(int ticksPerSecond, std::size_t maxEnemies)
        : m_ticksPerSecond(std::max(1, ticksPerSecond)),
          m_tickDuration(std::chrono::duration_cast<Clock::duration>(
                  std::chrono::duration<double>(1.0 / std::max(1, ticksPerSecond)))),
          m_world(maxEnemies) {
    m_snapshot.enemies.reserve(maxEnemies * 4);
}

GameLoop::~GameLoop() {
    stop();
}

void GameLoop::start() {
    if (m_running.exchange(true)) return;   // already running
    m_thread = std::thread(&GameLoop::run, this);
}

void GameLoop::stop() {
    {
        std::lock_guard<std::mutex> lock(m_wakeMutex);
        if (!m_running.exchange(false)) return;
    }
    m_wakeCv.notify_all();
    if (m_thread.joinable()) m_thread.join();
}

void GameLoop::setPaused(bool paused) {
    {
        std::lock_guard<std::mutex> lock(m_wakeMutex);
        m_paused = paused;
    }
    m_wakeCv.notify_all();
}

void GameLoop::setScreen(float width, float height) {
    std::lock_guard<std::mutex> lock(m_inputMutex);
    m_pending.width = width;
    m_pending.height = height;
    m_pending.hasScreen = true;
}

void GameLoop::movePlayerBy(float dx) {
    std::lock_guard<std::mutex> lock(m_inputMutex);
    m_pending.dx += dx;   // several drag events between ticks add up
}

void GameLoop::restart() {
    std::lock_guard<std::mutex> lock(m_inputMutex);
    m_pending.restart = true;
    m_pending.dx = 0.0f;
}

void GameLoop::run() {
    const float dt = 1.0f / static_cast<float>(m_ticksPerSecond);
    auto next = Clock::now();

    while (m_running) {
        if (m_paused) {
            publish();   // so the UI sees the Paused state
            std::unique_lock<std::mutex> lock(m_wakeMutex);
            m_wakeCv.wait(lock, [this] { return !m_paused || !m_running; });
            next = Clock::now();
            continue;
        }

        tick(dt);

        next += m_tickDuration;
        if (Clock::now() - next > kMaxLag) next = Clock::now();

        std::unique_lock<std::mutex> lock(m_wakeMutex);
        m_wakeCv.wait_until(lock, next, [this] { return !m_running || m_paused; });
    }
}

void GameLoop::tick(float dt) {
    PendingInput input;
    {
        std::lock_guard<std::mutex> lock(m_inputMutex);
        input = m_pending;
        m_pending = PendingInput{};
    }

    m_world.beginTick();
    if (input.hasScreen) m_world.setScreen(input.width, input.height);
    if (input.restart) m_world.reset();
    if (input.dx != 0.0f) m_world.movePlayerBy(input.dx);
    m_world.update(dt);

    publish();
}

void GameLoop::publish() {
    std::lock_guard<std::mutex> lock(m_snapshotMutex);
    Snapshot &s = m_snapshot;

    s.state = m_paused ? Paused : (m_world.isGameOver() ? GameOver : Running);
    s.score = m_world.score();

    const Player &player = m_world.player();
    s.playerX = player.x();
    s.playerPrevX = player.prevX();
    s.playerY = player.y();
    s.playerSize = m_world.hasScreen() ? player.size() : 0.0f;

    const Enemy &enemy = m_world.enemy();
    s.enemySize = enemy.size();
    s.enemies.clear();   // keeps capacity: no allocation per tick
    for (const auto &e : enemy.enemies()) {
        if (!e.active) continue;
        s.enemies.push_back(e.x);
        s.enemies.push_back(e.prevX);
        s.enemies.push_back(e.y);
        s.enemies.push_back(e.prevY);
    }
    s.tickTime = Clock::now();
}

int GameLoop::writeSnapshot(float *out, int capacity) const {
    if (capacity < kHeaderSize) return 0;

    std::lock_guard<std::mutex> lock(m_snapshotMutex);
    const Snapshot &s = m_snapshot;

    // The UI draws between the last two ticks: alpha = how far we are into
    // the current tick. This keeps motion smooth even when the screen's
    // refresh rate and the tick rate don't line up.
    float alpha = 1.0f;
    if (s.state == Running) {
        const auto sinceTick = Clock::now() - s.tickTime;
        alpha = std::chrono::duration<float>(sinceTick).count() /
                std::chrono::duration<float>(m_tickDuration).count();
        alpha = std::clamp(alpha, 0.0f, 1.0f);
    }

    const int maxEnemies = (capacity - kHeaderSize) / 2;
    const int enemyCount = std::min(static_cast<int>(s.enemies.size() / 4), maxEnemies);

    out[0] = static_cast<float>(s.state);
    out[1] = static_cast<float>(s.score);
    out[2] = lerp(s.playerPrevX, s.playerX, alpha);
    out[3] = s.playerY;
    out[4] = s.playerSize;
    out[5] = s.enemySize;
    out[6] = static_cast<float>(enemyCount);

    float *dst = out + kHeaderSize;
    for (int i = 0; i < enemyCount; ++i) {
        const float *e = &s.enemies[i * 4];
        *dst++ = lerp(e[1], e[0], alpha);   // x
        *dst++ = lerp(e[3], e[2], alpha);   // y
    }
    return kHeaderSize + enemyCount * 2;
}
