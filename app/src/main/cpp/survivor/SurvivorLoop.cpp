#include "SurvivorLoop.h"
#include <algorithm>
#include <cmath>

namespace {
    constexpr auto kMaxLag = std::chrono::milliseconds(250);
    constexpr float kPi = 3.14159265359F;

    float lerp(float from, float to, float t) { return from + (to - from) * t; }

    // Interpolate an angle the short way round (179 deg -> -179 deg is a 2 deg turn).
    float lerpAngle(float from, float to, float t) {
        float d = std::fmod(to - from + kPi, 2.0F * kPi);
        if (d < 0.0F) {
            d += 2.0F * kPi;
        }
        return from + (d - kPi) * t;
    }
}

SurvivorLoop::SurvivorLoop(int ticksPerSecond, std::size_t maxEnemies, std::size_t maxBullets,
                           std::size_t maxBlasts, std::size_t maxOrbs)
        : m_ticksPerSecond(std::max(1, ticksPerSecond)),
          m_tickDuration(std::chrono::duration_cast<Clock::duration>(
                  std::chrono::duration<double>(1.0 / std::max(1, ticksPerSecond)))),
          m_world(maxEnemies, maxBullets, maxBlasts, maxOrbs) {
    m_snapshot.enemies.reserve(maxEnemies * 6);   // no allocation while playing
    m_snapshot.bullets.reserve(maxBullets * 5);
    m_snapshot.blasts.reserve(maxBlasts * 3);
    m_snapshot.orbs.reserve(maxOrbs * 5);
}

SurvivorLoop::~SurvivorLoop() {
    stop();
}

void SurvivorLoop::start() {
    if (m_running.exchange(true)) {
        return;
    }
    m_thread = std::thread(&SurvivorLoop::run, this);
}

void SurvivorLoop::stop() {
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

void SurvivorLoop::setPaused(bool paused) {
    {
        std::lock_guard<std::mutex> lock(m_wakeMutex);
        m_paused = paused;
    }
    m_wakeCv.notify_all();
}

void SurvivorLoop::setScreen(float width, float height) {
    std::lock_guard<std::mutex> lock(m_inputMutex);
    m_pending.width = width;
    m_pending.height = height;
    m_pending.hasScreen = true;
}

void SurvivorLoop::setJoystick(float x, float y) {
    std::lock_guard<std::mutex> lock(m_inputMutex);
    m_pending.joyX = x;
    m_pending.joyY = y;
}

void SurvivorLoop::restart() {
    std::lock_guard<std::mutex> lock(m_inputMutex);
    m_pending.restart = true;
}

void SurvivorLoop::chooseUpgrade(int upgrade) {
    std::lock_guard<std::mutex> lock(m_inputMutex);
    m_pending.upgrade = upgrade;
}

void SurvivorLoop::run() {
    const float dt = 1.0F / static_cast<float>(m_ticksPerSecond);
    auto next = Clock::now();
    while (m_running) {
        if (m_paused) {
            publish();
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

void SurvivorLoop::tick(float dt) {
    PendingInput input;
    {
        std::lock_guard<std::mutex> lock(m_inputMutex);
        input = m_pending;
        // The stick is a *state* (where it is now), so it stays for the next
        // tick; only one-off events are cleared.
        m_pending.hasScreen = false;
        m_pending.restart = false;
        m_pending.upgrade = 0;
    }
    m_world.beginTick();
    if (input.hasScreen) {
        m_world.setScreen(input.width, input.height);
    }
    if (input.restart) {
        m_world.reset();
    }
    if (input.upgrade != 0) {
        m_world.chooseUpgrade(static_cast<SurvivorWorld::Upgrade>(input.upgrade));
    }
    m_world.setJoystick(input.joyX, input.joyY);
    m_world.update(dt);
    publish();
}

void SurvivorLoop::publish() {
    std::lock_guard<std::mutex> lock(m_snapshotMutex);
    Snapshot &s = m_snapshot;
    if (m_paused) {
        s.state = Paused;
    } else if (m_world.isGameOver()) {
        s.state = GameOver;
    } else if (m_world.isChoosingUpgrade()) {
        s.state = LevelUp;
    } else {
        s.state = Running;
    }
    s.x = m_world.playerX();
    s.prevX = m_world.playerPrevX();
    s.y = m_world.playerY();
    s.prevY = m_world.playerPrevY();
    s.angle = m_world.playerAngle();
    s.prevAngle = m_world.playerPrevAngle();
    s.size = m_world.hasScreen() ? m_world.playerSize() : 0.0F;
    s.kills = m_world.kills();
    s.time = m_world.timeSurvived();
    s.level = m_world.level();
    s.exp = m_world.exp();
    s.expNeeded = m_world.expNeeded();
    s.laserLevel = m_world.laserLevel();
    s.ultimate = static_cast<int>(m_world.ultimateStyle());
    s.offered = static_cast<int>(m_world.offeredUpgrade());
    s.beamAngle = m_world.beamAngle();
    s.prevBeamAngle = m_world.prevBeamAngle();
    s.beamStart = m_world.beamStart();
    s.beamLength = m_world.beamLength();
    s.beamThickness = m_world.beamThickness();

    s.enemySize = m_world.enemies().size();
    s.enemies.clear();
    for (const auto &e : m_world.enemies().enemies()) {
        if (!e.active) {
            continue;
        }
        s.enemies.insert(s.enemies.end(), {e.x, e.prevX, e.y, e.prevY, e.age,
                                           e.hitFlash > 0.0F ? 1.0F : 0.0F});
    }
    s.bulletSize = m_world.bullets().thickness();
    s.bulletLength = m_world.bullets().length();
    s.bullets.clear();
    for (const auto &b : m_world.bullets().bullets()) {
        if (!b.active) {
            continue;
        }
        s.bullets.insert(s.bullets.end(), {b.x, b.prevX, b.y, b.prevY,
                                           std::atan2(b.vx, -b.vy)});   // 0 = up, clockwise
    }
    s.blastSize = m_world.blasts().size();
    s.blastDuration = m_world.blasts().duration();
    s.blasts.clear();
    for (const auto &e : m_world.blasts().explosions()) {
        if (!e.active) {
            continue;
        }
        s.blasts.insert(s.blasts.end(), {e.x, e.y, e.age});
    }
    s.orbSize = m_world.orbs().size();
    s.nukeFlash = m_world.nukeFlash();
    s.orbs.clear();
    for (const auto &o : m_world.orbs().orbs()) {
        if (!o.active) {
            continue;
        }
        s.orbs.insert(s.orbs.end(), {o.x, o.prevX, o.y, o.prevY, static_cast<float>(o.kind)});
    }
    s.tickTime = Clock::now();
}

int SurvivorLoop::copySnapshotTo(float *out, int capacity) const {
    if (capacity < kHeaderSize) {
        return 0;
    }
    std::lock_guard<std::mutex> lock(m_snapshotMutex);
    const Snapshot &s = m_snapshot;
    // How far we are into the current tick (0..1): draw in between the last
    // two ticks so motion is smooth at any screen refresh rate.
    float alpha = 1.0F;
    if (s.state == Running) {
        alpha = std::chrono::duration<float>(Clock::now() - s.tickTime).count() /
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
    const int enemyCount = take(s.enemies.size() / 6, 4);
    const int bulletCount = take(s.bullets.size() / 5, 3);
    const int blastCount = take(s.blasts.size() / 3, 3);
    const int orbCount = take(s.orbs.size() / 5, 3);

    out[0] = static_cast<float>(s.state);
    out[1] = lerp(s.prevX, s.x, alpha);
    out[2] = lerp(s.prevY, s.y, alpha);
    out[3] = lerpAngle(s.prevAngle, s.angle, alpha);
    out[4] = s.size;
    out[5] = s.enemySize;
    out[6] = static_cast<float>(enemyCount);
    out[7] = s.bulletSize;
    out[8] = static_cast<float>(bulletCount);
    out[9] = s.blastSize;
    out[10] = static_cast<float>(blastCount);
    out[11] = static_cast<float>(s.kills);
    out[12] = s.time;
    out[13] = static_cast<float>(s.level);
    out[14] = static_cast<float>(s.exp);
    out[15] = static_cast<float>(s.expNeeded);
    out[16] = static_cast<float>(s.laserLevel);
    out[17] = static_cast<float>(s.ultimate);
    out[18] = static_cast<float>(s.offered);
    out[19] = s.bulletLength;
    out[20] = lerpAngle(s.prevBeamAngle, s.beamAngle, alpha);
    out[21] = s.beamStart;
    out[22] = s.beamLength;
    out[23] = s.beamThickness;
    out[24] = s.orbSize;
    out[25] = static_cast<float>(orbCount);
    out[26] = s.nukeFlash;

    // Animations (alien hover, explosions) advance to "now" between ticks.
    const float extraTime = s.state == Running
            ? std::min(std::chrono::duration<float>(Clock::now() - s.tickTime).count(),
                       std::chrono::duration<float>(m_tickDuration).count())
            : 0.0F;
    float *dst = out + kHeaderSize;
    for (int i = 0; i < enemyCount; ++i) {
        const float *e = &s.enemies[static_cast<std::size_t>(i * 6)];
        *dst++ = lerp(e[1], e[0], alpha);   // x
        *dst++ = lerp(e[3], e[2], alpha);   // y
        *dst++ = e[4] + extraTime;          // animation time
        *dst++ = e[5];                      // 1 = just hit: orange frames
    }
    for (int i = 0; i < bulletCount; ++i) {
        const float *b = &s.bullets[static_cast<std::size_t>(i * 5)];
        *dst++ = lerp(b[1], b[0], alpha);
        *dst++ = lerp(b[3], b[2], alpha);
        *dst++ = b[4];                      // flight direction
    }
    for (int i = 0; i < blastCount; ++i) {
        const float *e = &s.blasts[static_cast<std::size_t>(i * 3)];
        *dst++ = e[0];
        *dst++ = e[1];
        *dst++ = std::min(1.0F, (e[2] + extraTime) / s.blastDuration);   // progress 0..1
    }
    for (int i = 0; i < orbCount; ++i) {
        const float *o = &s.orbs[static_cast<std::size_t>(i * 5)];
        *dst++ = lerp(o[1], o[0], alpha);
        *dst++ = lerp(o[3], o[2], alpha);
        *dst++ = o[4];                      // kind
    }
    return static_cast<int>(dst - out);
}
