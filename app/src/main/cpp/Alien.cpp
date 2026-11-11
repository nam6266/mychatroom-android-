#include "Alien.h"
#include <algorithm>
#include <cmath>

namespace {
    constexpr float kTwoPi = 6.28318530718f;
    constexpr float kSizeOfScreenWidth = 0.16f;   // UFO width on screen
    constexpr float kHitRadiusOfSize = 0.42f;     // round-ish saucer + dome
    constexpr float kFirstSpawnSec = 5.0f;        // a few seconds of meteors first
    constexpr float kSpawnMinSec = 4.0f;          // then one every 4-7 s
    constexpr float kSpawnMaxSec = 7.0f;
    constexpr int kMaxAliveAtOnce = 3;
    constexpr int kMinHp = 10;                    // hits needed to destroy one: 10-15
    constexpr int kMaxHp = 15;
    constexpr float kHitFlashSec = 0.10f;         // orange dome after each hit
    // Path, as fractions of the screen, per second where it's a speed.
    constexpr float kFallSpeedOfHeight = 0.07f;   // ~14 s to drift across the screen
    constexpr float kSwayMinOfWidth = 0.18f;
    constexpr float kSwayMaxOfWidth = 0.32f;
    constexpr float kSwayMinFreq = 0.25f;         // one full sway every 2.5-4 s
    constexpr float kSwayMaxFreq = 0.40f;
    constexpr float kBobOfHeight = 0.05f;         // loops up and down while weaving
}

Aliens::Aliens(std::size_t maxCount) : m_aliens(maxCount) {}

void Aliens::reset(float screenWidth, float screenHeight) {
    m_screenWidth = screenWidth;
    m_screenHeight = screenHeight;
    m_size = screenWidth * kSizeOfScreenWidth;
    m_spawnTimer = kFirstSpawnSec;
    for (auto &a : m_aliens) {
        a = AlienState{};
    }
}

void Aliens::beginTick() {
    for (auto &a : m_aliens) {
        a.prevX = a.x;
        a.prevY = a.y;
    }
}

void Aliens::place(AlienState &a) const {
    const float w = kTwoPi * a.swayFreq;
    a.x = a.centerX + a.swayAmp * std::sin(w * a.age + a.phase);
    // vertical bob at twice the sway speed -> figure-8 loops
    a.y = a.entryY + a.fallSpeed * a.age + a.bobAmp * std::sin(2.0f * w * a.age + a.phase);
}

void Aliens::spawn(AlienState &a, std::mt19937 &rng) const {
    std::uniform_real_distribution<float> unit(0.0f, 1.0f);
    const float half = m_size * 0.5f;
    a = AlienState{};
    a.swayAmp = m_screenWidth * (kSwayMinOfWidth + (kSwayMaxOfWidth - kSwayMinOfWidth) * unit(rng));
    // keep the whole sway on screen
    const float minCenter = half + a.swayAmp;
    const float maxCenter = std::max(minCenter, m_screenWidth - half - a.swayAmp);
    a.centerX = minCenter + (maxCenter - minCenter) * unit(rng);
    a.swayFreq = kSwayMinFreq + (kSwayMaxFreq - kSwayMinFreq) * unit(rng);
    a.bobAmp = m_screenHeight * kBobOfHeight;
    a.phase = kTwoPi * unit(rng);
    a.fallSpeed = m_screenHeight * kFallSpeedOfHeight;
    a.entryY = -m_size;            // starts just above the top edge
    a.hp = std::uniform_int_distribution<int>(kMinHp, kMaxHp)(rng);
    a.active = true;
    place(a);
    a.prevX = a.x;                 // no interpolation streak from the old position
    a.prevY = a.y;
}

void Aliens::update(float dt, std::mt19937 &rng) {
    for (auto &a : m_aliens) {
        if (!a.active) continue;
        a.age += dt;
        a.hitFlash = std::max(0.0f, a.hitFlash - dt);
        place(a);
        if (a.y - m_size > m_screenHeight) a.active = false;   // flew off the bottom
    }

    m_spawnTimer -= dt;
    if (m_spawnTimer <= 0.0f) {
        const int alive = static_cast<int>(std::count_if(m_aliens.begin(), m_aliens.end(),
                                                         [](const AlienState &a) { return a.active; }));
        auto slot = std::find_if(m_aliens.begin(), m_aliens.end(),
                                 [](const AlienState &a) { return !a.active; });
        if (alive < kMaxAliveAtOnce && slot != m_aliens.end()) {
            spawn(*slot, rng);
            std::uniform_real_distribution<float> next(kSpawnMinSec, kSpawnMaxSec);
            m_spawnTimer = next(rng);
        } else {
            m_spawnTimer = 0.5f;   // busy sky: try again shortly
        }
    }
}

bool Aliens::touches(const AlienState &a, float x, float yTop, float yBottom, float radius) const {
    const float hitDistance = radius + m_size * kHitRadiusOfSize;
    const float closestY = std::clamp(a.y, yTop, yBottom);
    const float dx = a.x - x;
    const float dy = a.y - closestY;
    return dx * dx + dy * dy < hitDistance * hitDistance;
}

AlienHit Aliens::hitBySegment(float x, float yTop, float yBottom, float radius,
                              float *hitX, float *hitY) {
    for (auto &a : m_aliens) {
        if (!a.active || !touches(a, x, yTop, yBottom, radius)) continue;
        --a.hp;
        if (a.hp > 0) {
            a.hitFlash = kHitFlashSec;   // still flying: just flash
            return AlienHit::Damaged;
        }
        a.active = false;                // out of hp: destroyed
        if (hitX) *hitX = a.x;
        if (hitY) *hitY = a.y;
        return AlienHit::Destroyed;
    }
    return AlienHit::None;
}

bool Aliens::collidesWith(float x, float yTop, float yBottom, float radius) const {
    for (const auto &a : m_aliens) {
        if (a.active && touches(a, x, yTop, yBottom, radius)) return true;
    }
    return false;
}
