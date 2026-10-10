//
// Created by OS on 04/23/25.
//

#include "Player.h"
#include <algorithm>

namespace {
    constexpr float kSizeOfScreenWidth = 0.18f;     // sprite is 18% of the screen width
    constexpr float kHitRadiusOfSize = 0.35f;       // hitbox is a bit smaller than the sprite
    constexpr float kBottomMarginOfHeight = 0.04f;  // gap between the rocket and the bottom edge
}

void Player::reset(float screenWidth, float screenHeight) {
    m_screenWidth = screenWidth;
    m_size = screenWidth * kSizeOfScreenWidth;
    m_x = screenWidth * 0.5f;
    m_y = screenHeight - screenHeight * kBottomMarginOfHeight - m_size * 0.5f;
    m_prevX = m_x;
}

void Player::beginTick() {
    m_prevX = m_x;
}

void Player::moveBy(float dx) {
    m_x += dx;
    clampToScreen();
}

void Player::moveTo(float x) {
    m_x = x;
    clampToScreen();
}

float Player::hitRadius() const {
    return m_size * kHitRadiusOfSize;
}

void Player::clampToScreen() {
    const float half = m_size * 0.5f;
    m_x = std::clamp(m_x, half, std::max(half, m_screenWidth - half));
}
