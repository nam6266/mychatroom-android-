//
// Created by OS on 04/23/25.
//

#include "Player.h"

Player::Player(float screenLimit):m_velocity{5.0f},m_status{true},m_size{200.0f},m_screenLimit(screenLimit){
    m_positionX = m_screenLimit/2;
}

Player::~Player() {

}

void Player::updatePosition() {
    float newPositon = m_positionX + m_velocity;
    if (newPositon <= 0 || newPositon >= m_screenLimit) {
        m_velocity = -m_velocity;
    }
    m_positionX += m_velocity;
}

void Player::setScreen(float screen) {
    m_screenLimit = screen/2;
}

float Player::getPosition() {
    return m_positionX;
}

void Player::setPosition(float newPosition) {
    m_positionX += newPosition;
}

void Player::setVelocity(float newVelocity) {
    m_velocity = newVelocity;
}

