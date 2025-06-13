package com.example.mychatroom.engine

class GameEngine {
    companion object {
        init {
            System.loadLibrary("mychatroom")
        }
    }

    external fun update(position: Float)
    external fun getPosition(): Float
    external fun getNumber(): Int
    external fun setScreen(height: Float, witdh: Float)
    external fun updateEnemy(playerPosition: Float) : Boolean
    external fun getEnemy(): Array<FloatArray>
}