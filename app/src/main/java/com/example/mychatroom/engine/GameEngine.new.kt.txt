package com.example.mychatroom.engine

/**
 * Kotlin side of the C++ game engine.
 *
 * The C++ side runs the game rules on its own thread at [ticksPerSecond].
 * The UI calls [readFrame] once per rendered frame to get the latest state,
 * and sends input with [movePlayerBy] / [restart]. All methods must be called
 * from the main thread.
 *
 * Call [close] when the game is no longer needed (GameViewModel.onCleared does it).
 */
class GameEngine(
    ticksPerSecond: Int = 60,
    val maxEnemies: Int = 20,
) : AutoCloseable {

    private var handle: Long = nativeCreate(ticksPerSecond, maxEnemies)

    /** Reused every frame, so rendering allocates nothing. */
    val frame = GameFrame(maxEnemies)

    fun start() = withHandle { nativeStart(it) }

    fun setPaused(paused: Boolean) = withHandle { nativeSetPaused(it, paused) }

    /** Size of the game canvas in pixels. Starts a new round when it changes. */
    fun setScreen(width: Float, height: Float) = withHandle { nativeSetScreen(it, width, height) }

    /** Moves the rocket horizontally by [dx] pixels (e.g. a drag delta). */
    fun movePlayerBy(dx: Float) = withHandle { nativeMovePlayerBy(it, dx) }

    fun restart() = withHandle { nativeRestart(it) }

    /** Copies the latest, smoothly interpolated game state into [frame]. */
    fun readFrame(): GameFrame {
        withHandle { frame.length = nativeGetSnapshot(it, frame.data) }
        return frame
    }

    override fun close() {
        val h = handle
        if (h != 0L) {
            handle = 0L
            nativeDestroy(h) // stops and joins the C++ logic thread
        }
    }

    private inline fun withHandle(block: (Long) -> Unit) {
        val h = handle
        if (h != 0L) block(h)
    }

    private external fun nativeCreate(ticksPerSecond: Int, maxEnemies: Int): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeStart(handle: Long)
    private external fun nativeSetPaused(handle: Long, paused: Boolean)
    private external fun nativeSetScreen(handle: Long, width: Float, height: Float)
    private external fun nativeMovePlayerBy(handle: Long, dx: Float)
    private external fun nativeRestart(handle: Long)
    private external fun nativeGetSnapshot(handle: Long, out: FloatArray): Int

    companion object {
        init {
            System.loadLibrary("mychatroom")
        }
    }
}

/**
 * One frame of game state. Layout matches GameLoop::writeSnapshot in C++:
 * [state, score, playerX, playerY, playerSize, enemySize, enemyCount, x0, y0, x1, y1, ...]
 * Positions are sprite centres in pixels.
 */
class GameFrame(maxEnemies: Int) {
    internal val data = FloatArray(HEADER_SIZE + maxEnemies * 2)
    internal var length = 0

    val state: Int get() = data[0].toInt()
    val isGameOver: Boolean get() = state == STATE_GAME_OVER
    val isPaused: Boolean get() = state == STATE_PAUSED
    val score: Int get() = data[1].toInt()
    val playerX: Float get() = data[2]
    val playerY: Float get() = data[3]
    val playerSize: Float get() = data[4]
    val enemySize: Float get() = data[5]
    val enemyCount: Int get() = if (length > 0) data[6].toInt() else 0

    /** False until the engine has a screen size and has run its first tick. */
    val isReady: Boolean get() = length > 0 && playerSize > 0f

    fun enemyX(index: Int): Float = data[HEADER_SIZE + index * 2]
    fun enemyY(index: Int): Float = data[HEADER_SIZE + index * 2 + 1]

    companion object {
        const val STATE_RUNNING = 0
        const val STATE_PAUSED = 1
        const val STATE_GAME_OVER = 2
        const val HEADER_SIZE = 7
    }
}
