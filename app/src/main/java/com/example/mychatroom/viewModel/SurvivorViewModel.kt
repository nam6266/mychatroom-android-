package com.example.mychatroom.viewModel

import androidx.lifecycle.ViewModel
import com.example.mychatroom.engine.SurvivorEngine

/**
 * Owns the Space Survivor engine, like [GameViewModel] does for Alien Shooter:
 * it survives rotation, and the C++ thread is shut down exactly once when the
 * game screen is gone for good (leaving it returns to the Games page).
 */
class SurvivorViewModel : ViewModel() {

    val engine = SurvivorEngine(ticksPerSecond = 60).apply { start() }

    override fun onCleared() {
        engine.close()
        super.onCleared()
    }
}
