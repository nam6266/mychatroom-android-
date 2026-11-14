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
    // Level 3 fires 3 bullets per round, so maximun should be 12 ~ 15, but keep it 24 incase of later slow the bullet speed which will spawn more bullet on the screen
    val maxBullets: Int = 24,
    val maxPowerUps: Int = 4,
    val maxExplosions: Int = 16,
    val maxAliens: Int = 4,
    val maxAlienBlasts: Int = 6,
) : AutoCloseable {

    private var handle: Long = nativeCreate(
        ticksPerSecond, maxEnemies, maxBullets, maxPowerUps, maxExplosions, maxAliens, maxAlienBlasts,
    )

    /** Reused every frame, so rendering allocates nothing. */
    val frame = GameFrame(maxEnemies, maxBullets, maxPowerUps, maxExplosions, maxAliens, maxAlienBlasts)

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

    private external fun nativeCreate(
        ticksPerSecond: Int,
        maxEnemies: Int,
        maxBullets: Int,
        maxPowerUps: Int,
        maxExplosions: Int,
        maxAliens: Int,
        maxAlienBlasts: Int,
    ): Long
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
 * [state, score, playerX, playerY, playerSize, enemySize, enemyCount, bulletSize, bulletCount,
 *  powerUpSize, powerUpCount, weaponLevel,
 *  enemy x,y ..., bullet x,y ..., power-up x,y ...]
 * Positions are sprite centres in pixels.
 */
class GameFrame(
    maxEnemies: Int,
    maxBullets: Int,
    maxPowerUps: Int,
    maxExplosions: Int,
    maxAliens: Int,
    maxAlienBlasts: Int,
) {
    internal val data = FloatArray(
        HEADER_SIZE + (maxEnemies + maxBullets) * 2 + maxPowerUps * 3 + maxExplosions * 4 +
            maxAliens * 4 + maxAlienBlasts * 4
    )
    internal var length = 0

    val state: Int get() = data[0].toInt()
    val isGameOver: Boolean get() = state == STATE_GAME_OVER
    val isPaused: Boolean get() = state == STATE_PAUSED //currently no pause for now
    val score: Int get() = data[1].toInt()
    val playerX: Float get() = data[2]
    val playerY: Float get() = data[3]
    val playerSize: Float get() = data[4]
    val enemySize: Float get() = data[5]
    val enemyCount: Int get() = if (length > 0) data[6].toInt() else 0
    val bulletSize: Float get() = data[7]
    val bulletCount: Int get() = if (length > 0) data[8].toInt() else 0
    val powerUpSize: Float get() = data[9]
    val powerUpCount: Int get() = if (length > 0) data[10].toInt() else 0
    val weaponLevel: Int get() = if (length > 0) data[11].toInt() else 1
    /** Seconds of laser left; 0 = laser off. */
    val laserTimeLeft: Float get() = if (length > 0) data[18] else 0f
    /** Where the beam ends (y in px): the first enemy it touches, or 0 = top of the screen. */
    val laserTopY: Float get() = data[19]
    val laserWidth: Float get() = data[20]
    val explosionSize: Float get() = data[12]
    val explosionCount: Int get() = if (length > 0) data[13].toInt() else 0
    val alienSize: Float get() = data[14]
    val alienCount: Int get() = if (length > 0) data[15].toInt() else 0
    val alienBlastSize: Float get() = data[16]
    val alienBlastCount: Int get() = if (length > 0) data[17].toInt() else 0

    /** False until the engine has a screen size and has run its first tick. */
    val isReady: Boolean get() = length > 0 && playerSize > 0f

    fun enemyX(index: Int): Float = data[HEADER_SIZE + index * 2]
    fun enemyY(index: Int): Float = data[HEADER_SIZE + index * 2 + 1]

    // Bullets are stored right after the enemies.
    fun bulletX(index: Int): Float = data[HEADER_SIZE + (enemyCount + index) * 2]
    fun bulletY(index: Int): Float = data[HEADER_SIZE + (enemyCount + index) * 2 + 1]

    // Power-ups come after the bullets.
    // Power-ups after the bullets, 3 floats each: x, y, kind (0 = gun upgrade, 1 = laser).
    private fun powerUpBase(index: Int) = HEADER_SIZE + (enemyCount + bulletCount) * 2 + index * 3
    fun powerUpX(index: Int): Float = data[powerUpBase(index)]
    fun powerUpY(index: Int): Float = data[powerUpBase(index) + 1]
    fun powerUpIsLaser(index: Int): Boolean = data[powerUpBase(index) + 2] > 0.5f

    // Explosions come last, 4 floats each: x, y, progress (0..1), seed (0..1).
    private fun explosionBase(index: Int) =
        powerUpBase(powerUpCount) + index * 4
    fun explosionX(index: Int): Float = data[explosionBase(index)]
    fun explosionY(index: Int): Float = data[explosionBase(index) + 1]
    fun explosionProgress(index: Int): Float = data[explosionBase(index) + 2]
    fun explosionSeed(index: Int): Float = data[explosionBase(index) + 3]

    // Aliens after the explosions, 4 floats each: x, y, animation time (s), hit flash (0/1).
    private fun alienBase(index: Int) = explosionBase(explosionCount) + index * 4
    fun alienX(index: Int): Float = data[alienBase(index)]
    fun alienY(index: Int): Float = data[alienBase(index) + 1]
    fun alienAnimTime(index: Int): Float = data[alienBase(index) + 2]
    fun alienFlashing(index: Int): Boolean = data[alienBase(index) + 3] > 0.5f

    // Alien blasts last, 4 floats each: x, y, progress (0..1), seed.
    private fun alienBlastBase(index: Int) = alienBase(alienCount) + index * 4
    fun alienBlastX(index: Int): Float = data[alienBlastBase(index)]
    fun alienBlastY(index: Int): Float = data[alienBlastBase(index) + 1]
    fun alienBlastProgress(index: Int): Float = data[alienBlastBase(index) + 2]

    companion object {
        const val STATE_RUNNING = 0
        const val STATE_PAUSED = 1
        const val STATE_GAME_OVER = 2
        const val HEADER_SIZE = 21
        const val MAX_WEAPON_LEVEL = 3   // matches GameWorld::kMaxWeaponLevel
    }
}
