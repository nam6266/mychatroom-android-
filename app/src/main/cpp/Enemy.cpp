//
// Created by OS on 04/23/25.
//

#include "Enemy.h"
#include <android/log.h>

#define LOG_TAG "namLog"


Enemy::Enemy(size_t count):m_size(20.0f),m_velocity(10.0f),m_enemyNumber(count), m_spawn{spawnRate} {
    m_positionX.resize(count);
    m_positionY.resize(count);
}


Enemy::~Enemy() {

}

void Enemy::setVelocity(float velocity) {
    m_velocity = velocity;
}

void Enemy::setScreen(float width, float height) {
    m_screenWidth = width;
    m_screenHeight = height;
}

bool Enemy::updateEnemy(float playerPosition) {

    std::mt19937 gen(m_rd());

    m_spawn++;

    if (enemyCreated < m_enemyNumber && m_spawn >= spawnRate) {
        enemyCreated++;

        int numColumns = 50;
        float columnWidth = static_cast<float>(m_screenWidth) / numColumns;

        std::uniform_int_distribution<> distr(0, numColumns - 1);
        int columnIndex = distr(gen);

        m_positionX[enemyCreated] = columnWidth * columnIndex + columnWidth / 2;
        m_positionY[enemyCreated] = 0;

        m_spawn = 0; // reset spawn counter
    }

    size_t i = 0;
    while(i < enemyCreated) {
        float playerX = playerPosition;
        float playerY = m_screenHeight - 20.0f;
//        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "This is a debug log with value playerX: %f", playerX);
//        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "This is a debug log with value playerY: %f", playerY);
        if(m_positionY[i] + m_velocity >= m_screenHeight - 400.0f) {
            float dx = m_positionX[i] - playerX;
            float dy = m_positionY[i] - playerY;
            float distance = std::sqrt(dx * dx + dy * dy);
           // __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "This is a debug log with value playerY: %f", distance);
            bool collision = distance < 150.0f;
            if(collision) {
                return collision;
            }
        }
        if (m_positionY[i] + m_velocity < m_screenHeight) {
            m_positionY[i] += m_velocity;

        } else {
            int numColumns = 50;
            float columnWidth = static_cast<float>(m_screenWidth) / numColumns;

            std::uniform_int_distribution<> distr(0, numColumns - 1);
            int columnIndex = distr(gen);

            m_positionX[enemyCreated] = columnWidth * columnIndex + columnWidth / 2;
            m_positionY[i] = 0;
        }
        i++;
    }
    return false;
}

size_t Enemy::getEnemyNumber() {
    return m_enemyNumber;
}

float Enemy::getEnemyPositionx(int index) {
    return m_positionX[index];
}

float Enemy::getEnemyPositiony(int index) {
    return m_positionY[index];
}


