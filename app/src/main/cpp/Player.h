//
// Created by OS on 04/23/25.
//

#ifndef MY_CHATROOM_PLAYER_H
#define MY_CHATROOM_PLAYER_H


class Player {
private:
    float m_size;
    float m_positionX;
    float m_screenLimit;
    float m_velocity;
    bool m_status;

public:
    explicit Player(float screenLimit);
    ~Player();

    void updatePosition();

    float getPosition();
    void setScreen(float screen);
    void setPosition(float newPosition);
    void setVelocity(float newVelocity);
};


#endif //MY_CHATROOM_PLAYER_H
