//
// Created by OS on 04/23/25.
//

#ifndef MY_CHATROOM_PLAYER_H
#define MY_CHATROOM_PLAYER_H

// The rocket at the bottom of the screen. All units are pixels.
// Only the logic thread touches a Player.
class Player {
public:
    // Puts the player bottom-centre and sizes it from the screen width.
    void reset(float screenWidth, float screenHeight);

    // Stores the current position as "previous" so the renderer can interpolate.
    void beginTick();

    // Relative move (e.g. a drag delta). Clamped to the screen.
    void moveBy(float dx);

    float x() const { return m_x; }
    float prevX() const { return m_prevX; }
    float y() const { return m_y; }
    float size() const { return m_size; }        // drawn size (square)
    // Hitbox is a capsule: the vertical segment x(), [hitTop(), hitBottom()]
    // widened by hitRadius(). Fits the tall, thin rocket far better than a circle.
    float hitRadius() const;
    float hitTop() const;
    float hitBottom() const;

private:
    void clampToScreen();

    float m_x = 0.0F;
    float m_prevX = 0.0F;
    float m_y = 0.0F;
    float m_size = 0.0F;
    float m_screenWidth = 0.0F;
};

#endif //MY_CHATROOM_PLAYER_H
