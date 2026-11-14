#include "Explosion.h"

Explosions::Explosions(std::size_t maxCount, float durationSec, float sizeOfScreenWidth)
        : m_explosions(maxCount), m_duration(durationSec), m_sizeOfScreenWidth(sizeOfScreenWidth) {}

void Explosions::reset(float screenWidth) {
    m_size = screenWidth * m_sizeOfScreenWidth;
    for (auto &e : m_explosions) {
        e = ExplosionState{};
    }
}

void Explosions::spawn(float x, float y, std::mt19937 &rng) {
    ExplosionState *slot = nullptr;
    for (auto &e : m_explosions) {
        if (!e.active) {
            slot = &e;
            break;
        }
        if ((slot == nullptr) || e.age > slot->age) {
            slot = &e;   // remember the oldest
        }
    }
    if (slot == nullptr) {
        return;
    }
    std::uniform_real_distribution<float> unit(0.0F, 1.0F);
    *slot = ExplosionState{x, y, 0.0F, unit(rng), true};
}

void Explosions::update(float dt) {
    for (auto &e : m_explosions) {
        if (!e.active) {
            continue;
        }
        e.age += dt;
        if (e.age >= m_duration) {
            e.active = false;
        }
    }
}
