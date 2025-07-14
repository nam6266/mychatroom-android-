package com.example.mychatroom.engine.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

class Enemy(color : Color, size : Float, initialPosition: Offset) {
    private val color: Color = color
    private val size : Float = size
    private var position = Offset(0.0f,0.0f)

    fun update(updatePosition: Offset) {
        position += updatePosition
    }
    fun setPosition(newPositon : Offset) {
        position = newPositon
    }
    fun getPosition() : Offset {
        return position
    }

    fun getColor() : Color {
        return color
    }

    fun getSize() : Float {
        return size
    }
}