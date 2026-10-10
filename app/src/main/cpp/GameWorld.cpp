#include "GameWorld.h"

GameWorld::GameWorld(std::size_t maxEnemies)
        : m_enemy(maxEnemies), m_rng(std::random_device{}()) {}

void GameWorld::setScreen(float width, float height) {
    if (width == m_width && height == m_height) return;
    m_width = width;
    m_height = height;
    reset();
}

void GameWorld::reset() {
    m_player.reset(m_width, m_height);
    m_enemy.reset(m_width, m_height);
    m_score = 0;
    m_gameOver = false;
}

void GameWorld::beginTick() {
    m_player.beginTick();
    m_enemy.beginTick();
}

void GameWorld::movePlayerBy(float dx) {
    if (m_gameOver || !hasScreen()) return;
    m_player.moveBy(dx);
}

void GameWorld::update(float dt) {
    if (m_gameOver || !hasScreen()) return;
    m_score += m_enemy.update(dt, m_rng);
    if (m_enemy.collidesWith(m_player.x(), m_player.y(), m_player.hitRadius())) {
        m_gameOver = true;
    }
}
