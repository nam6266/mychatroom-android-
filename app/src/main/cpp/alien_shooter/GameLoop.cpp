#include "GameLoop.h"
#include <algorithm>

namespace {
    // If the thread falls this far behind (debugger, app in background),
    // drop the missed ticks instead of fast-forwarding the game.
    constexpr auto kMaxLag = std::chrono::milliseconds(250);

    float lerp(float from, float to, float t) { return from + (to - from) * t; }
}

GameLoop::GameLoop(int ticksPerSecond, std::size_t maxEnemies, std::size_t maxBullets,
                   std::size_t maxPowerUps, std::size_t maxExplosions, std::size_t maxAliens,
                   std::size_t maxAlienBlasts)
        : m_ticksPerSecond(std::max(1, ticksPerSecond)),
          m_tickDuration(std::chrono::duration_cast<Clock::duration>(
                  std::chrono::duration<double>(1.0 / std::max(1, ticksPerSecond)))),
          m_world(maxEnemies, maxBullets, maxPowerUps, maxExplosions, maxAliens, maxAlienBlasts) {
    m_snapshot.aliens.reserve(maxAliens * 6);
    m_snapshot.alienBlasts.reserve(maxAlienBlasts * 4);
    m_snapshot.explosions.reserve(maxExplosions * 4);
    m_snapshot.enemies.reserve(maxEnemies * 4);
    m_snapshot.bullets.reserve(maxBullets * 4);
    m_snapshot.powerUps.reserve(maxPowerUps * 5);
}

GameLoop::~GameLoop() {
    stop();
}

void GameLoop::start() {
    if (m_running.exchange(true)) {
        return;   // already running
    }
    m_thread = std::thread(&GameLoop::run, this);
}

void GameLoop::stop() {
    {
        std::lock_guard<std::mutex> lock(m_wakeMutex);
        if (!m_running.exchange(false)) {
            return;
        }
    }
    m_wakeCv.notify_all();
    if (m_thread.joinable()) {
        m_thread.join();
    }
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
    m_pending.dx = 0.0F;
}

void GameLoop::run() {
    const float dt = 1.0F / static_cast<float>(m_ticksPerSecond);
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
        if (Clock::now() - next > kMaxLag) {
            next = Clock::now();
        }

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
    if (input.hasScreen) {
        m_world.setScreen(input.width, input.height);
    }
    if (input.restart) {
        m_world.reset();
    }
    if (input.dx != 0.0F) {
        m_world.movePlayerBy(input.dx);
    }
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
    s.playerSize = m_world.hasScreen() ? player.size() : 0.0F;

    const Enemy &enemy = m_world.enemy();
    s.enemySize = enemy.size();
    s.enemies.clear();   // keeps capacity: no allocation per tick
    for (const auto &e : enemy.enemies()) {
        if (!e.active) {
            continue;
        }
        s.enemies.push_back(e.x);
        s.enemies.push_back(e.prevX);
        s.enemies.push_back(e.y);
        s.enemies.push_back(e.prevY);
    }

    const Bullets &bullets = m_world.bullets();
    s.bulletSize = bullets.size();
    s.bullets.clear();
    for (const auto &b : bullets.bullets()) {
        if (!b.active) {
            continue;
        }
        s.bullets.push_back(b.x);
        s.bullets.push_back(b.prevX);
        s.bullets.push_back(b.y);
        s.bullets.push_back(b.prevY);
    }

    const PowerUps &powerUps = m_world.powerUps();
    s.powerUpSize = powerUps.size();
    s.powerUps.clear();
    for (const auto &p : powerUps.powerUps()) {
        if (!p.active) {
            continue;
        }
        s.powerUps.push_back(p.x);
        s.powerUps.push_back(p.prevX);
        s.powerUps.push_back(p.y);
        s.powerUps.push_back(p.prevY);
        s.powerUps.push_back(static_cast<float>(p.kind));
    }
    s.weaponLevel = m_world.weaponLevel();

    const Explosions &explosions = m_world.explosions();
    s.explosionSize = explosions.size();
    s.explosions.clear();
    for (const auto &e : explosions.explosions()) {
        if (!e.active) {
            continue;
        }
        s.explosions.push_back(e.x);
        s.explosions.push_back(e.y);
        s.explosions.push_back(e.age);
        s.explosions.push_back(e.seed);
    }
    s.explosionDuration = explosions.duration();

    const Aliens &aliens = m_world.aliens();
    s.alienSize = aliens.size();
    s.aliens.clear();
    for (const auto &a : aliens.aliens()) {
        if (!a.active) {
            continue;
        }
        s.aliens.push_back(a.x);
        s.aliens.push_back(a.prevX);
        s.aliens.push_back(a.y);
        s.aliens.push_back(a.prevY);
        s.aliens.push_back(a.age);
        s.aliens.push_back(a.hitFlash > 0.0F ? 1.0F : 0.0F);
    }

    const Explosions &alienBlasts = m_world.alienBlasts();
    s.alienBlastSize = alienBlasts.size();
    s.alienBlastDuration = alienBlasts.duration();
    s.alienBlasts.clear();
    for (const auto &e : alienBlasts.explosions()) {
        if (!e.active) {
            continue;
        }
        s.alienBlasts.push_back(e.x);
        s.alienBlasts.push_back(e.y);
        s.alienBlasts.push_back(e.age);
        s.alienBlasts.push_back(e.seed);
    }
    // Hide the beam after game over (the ship is gone).
    s.laserTimeLeft = m_world.isGameOver() ? 0.0F : m_world.laserTimeLeft();
    s.laserTopY = m_world.laserTopY();
    s.laserWidth = m_world.laserWidth();
    s.tickTime = Clock::now();
}

int GameLoop::writeSnapshot(float *out, int capacity) const {
    if (capacity < kHeaderSize) {
        return 0;
    }

    std::lock_guard<std::mutex> lock(m_snapshotMutex);
    const Snapshot &s = m_snapshot;

    // The UI draws between the last two ticks: alpha = how far we are into
    // the current tick. This keeps motion smooth even when the screen's
    // refresh rate and the tick rate don't line up.
    float alpha = 1.0F;
    if (s.state == Running) {
        const auto sinceTick = Clock::now() - s.tickTime;
        alpha = std::chrono::duration<float>(sinceTick).count() /
                std::chrono::duration<float>(m_tickDuration).count();
        alpha = std::clamp(alpha, 0.0F, 1.0F);
    }

    // Each group is limited by the room (in floats) left in `out`.
    int left = capacity - kHeaderSize;
    auto take = [&left](std::size_t available, int floatsEach) {
        const int n = std::min(static_cast<int>(available), left / floatsEach);
        left -= n * floatsEach;
        return n;
    };
    const int enemyCount = take(s.enemies.size() / 4, 2);
    const int bulletCount = take(s.bullets.size() / 4, 2);
    const int powerUpCount = take(s.powerUps.size() / 5, 3);
    const int explosionCount = take(s.explosions.size() / 4, 4);
    const int alienCount = take(s.aliens.size() / 6, 4);
    const int alienBlastCount = take(s.alienBlasts.size() / 4, 4);

    out[0] = static_cast<float>(s.state);
    out[1] = static_cast<float>(s.score);
    out[2] = lerp(s.playerPrevX, s.playerX, alpha);
    out[3] = s.playerY;
    out[4] = s.playerSize;
    out[5] = s.enemySize;
    out[6] = static_cast<float>(enemyCount);
    out[7] = s.bulletSize;
    out[8] = static_cast<float>(bulletCount);
    out[9] = s.powerUpSize;
    out[10] = static_cast<float>(powerUpCount);
    out[11] = static_cast<float>(s.weaponLevel);
    out[12] = s.explosionSize;
    out[13] = static_cast<float>(explosionCount);
    out[14] = s.alienSize;
    out[15] = static_cast<float>(alienCount);
    out[16] = s.alienBlastSize;
    out[17] = static_cast<float>(alienBlastCount);
    out[18] = s.laserTimeLeft;
    out[19] = s.laserTopY;
    out[20] = s.laserWidth;

    float *dst = out + kHeaderSize;
    for (int i = 0; i < enemyCount; ++i) {
        const float *e = &s.enemies[static_cast<std::size_t>(i * 4)];
        *dst++ = lerp(e[1], e[0], alpha);   // x
        *dst++ = lerp(e[3], e[2], alpha);   // y
    }
    for (int i = 0; i < bulletCount; ++i) {
        const float *b = &s.bullets[static_cast<std::size_t>(i * 4)];
        *dst++ = lerp(b[1], b[0], alpha);   // x
        *dst++ = lerp(b[3], b[2], alpha);   // y
    }
    for (int i = 0; i < powerUpCount; ++i) {
        const float *p = &s.powerUps[static_cast<std::size_t>(i * 5)];
        *dst++ = lerp(p[1], p[0], alpha);   // x
        *dst++ = lerp(p[3], p[2], alpha);   // y
        *dst++ = p[4];                      // kind: 0 gun upgrade, 1 laser
    }
    // Explosions don't move, but their animation is advanced to "now" so it
    // plays smoothly between ticks (also after game over).
    const float sinceTick = std::chrono::duration<float>(Clock::now() - s.tickTime).count();
    const float extraAge = s.state == Paused ? 0.0F
            : std::min(sinceTick, std::chrono::duration<float>(m_tickDuration).count());
    for (int i = 0; i < explosionCount; ++i) {
        const float *e = &s.explosions[static_cast<std::size_t>(i * 4)];
        *dst++ = e[0];                                                           // x
        *dst++ = e[1];                                                           // y
        *dst++ = std::min(1.0F, (e[2] + extraAge) / s.explosionDuration);       // progress
        *dst++ = e[3];                                                           // seed
    }
    for (int i = 0; i < alienCount; ++i) {
        const float *a = &s.aliens[static_cast<std::size_t>(i * 6)];
        *dst++ = lerp(a[1], a[0], alpha);   // x
        *dst++ = lerp(a[3], a[2], alpha);   // y
        *dst++ = a[4] + extraAge;           // animation time (s), smooth between ticks
        *dst++ = a[5];                      // 1 = just hit: draw the orange frames
    }
    for (int i = 0; i < alienBlastCount; ++i) {
        const float *e = &s.alienBlasts[static_cast<std::size_t>(i * 4)];
        *dst++ = e[0];
        *dst++ = e[1];
        *dst++ = std::min(1.0F, (e[2] + extraAge) / s.alienBlastDuration);
        *dst++ = e[3];
    }
    return static_cast<int>(dst - out);
}
