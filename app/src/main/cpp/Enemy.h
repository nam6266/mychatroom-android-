//
// Created by OS on 04/23/25.
//

#ifndef MY_CHATROOM_ENEMY_H
#define MY_CHATROOM_ENEMY_H
#include <vector>
#include <iostream>
#include <random>

static int enemyCreated = 0;
static int spawnRate = 20;
class Enemy {

public:
    explicit Enemy(size_t count);
    ~Enemy();
private:
    float m_size;
    std::vector<float> m_positionX;
    std::vector<float> m_positionY;
    float m_screenWidth;
    float m_screenHeight;
    float m_velocity;
    int m_spawn;
    size_t m_enemyNumber;
    std::random_device m_rd;
public:
    void setVelocity(float velocity);
    void setScreen(float width, float height);
    bool updateEnemy(float playerPosition);
    size_t getEnemyNumber();
    float getEnemyPositionx(int index);
    float getEnemyPositiony(int index);
};


#endif //MY_CHATROOM_ENEMY_H
