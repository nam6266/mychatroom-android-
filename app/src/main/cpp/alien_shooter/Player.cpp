//
// Created by OS on 04/23/25.
//

#include "Player.h"
#include <algorithm>

namespace {
    constexpr float kSizeOfScreenWidth = 0.18F;     // sprite is 18% of the screen width
    // Hitbox = a capsule (vertical bar with round ends) fitted to
    // drawable-nodpi/player_ship.webp by testing against the real pixels.
    // The ship is wide and fills its square, so the best fit is nearly a
    // circle. Values are fractions of m_size, from the sprite centre
    // (negative = up). For the old thin rocket.png use 0.12 / -0.35 / 0.16.
    constexpr float kHitRadiusOfSize = 0.46F;
    constexpr float kHitTopOfSize = -0.10F;
    constexpr float kHitBottomOfSize = 0.10F;
    constexpr float kBottomMarginOfHeight = 0.04F;  // gap between the rocket and the bottom edge
}

void Player::reset(float screenWidth, float screenHeight) {
    m_screenWidth = screenWidth;
    m_size = screenWidth * kSizeOfScreenWidth;
    m_x = screenWidth * 0.5F;
    m_y = screenHeight - screenHeight * kBottomMarginOfHeight - m_size * 0.5F;
    m_prevX = m_x;
}

void Player::beginTick() {
    m_prevX = m_x;
}

void Player::moveBy(float dx) {
    m_x += dx;
    clampToScreen();
}

float Player::hitRadius() const {
    return m_size * kHitRadiusOfSize;
}

float Player::hitTop() const {
    return m_y + m_size * kHitTopOfSize;
}

float Player::hitBottom() const {
    return m_y + m_size * kHitBottomOfSize;
}

void Player::clampToScreen() {
    const float half = m_size * 0.5F;
    m_x = std::clamp(m_x, half, std::max(half, m_screenWidth - half));
}
