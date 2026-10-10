package com.example.mychatroom.viewModel

import androidx.lifecycle.ViewModel
import com.example.mychatroom.engine.GameEngine

/**
 * Owns the native engine so the game survives rotation and leaving/returning
 * to the screen, and is shut down exactly once when the screen is gone for good.
 */
class GameViewModel : ViewModel() {

    val engine = GameEngine(ticksPerSecond = 60).apply { start() }

    override fun onCleared() {
        engine.close()
        super.onCleared()
    }
}
