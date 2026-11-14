package com.example.mychatroom.engine

/**
 * Kotlin side of the Space Survivor C++ engine (SurvivorLoop), built the same
 * way as [GameEngine]: the C++ side runs the rules on its own thread, the UI
 * sends input and reads one [SurvivorFrame] per rendered frame.
 * All methods must be called from the main thread; call [close] when done.
 */
class SurvivorEngine(
    ticksPerSecond: Int = 60,
    // Pool sizes: lots of room for a big swarm; bullets/blasts measured well below these.
    val maxEnemies: Int = 120,
    val maxBullets: Int = 16,
    val maxBlasts: Int = 12,
) : AutoCloseable {

    private var handle: Long = nativeCreate(ticksPerSecond, maxEnemies, maxBullets, maxBlasts)

    /** Reused every frame, so rendering allocates nothing. */
    val frame = SurvivorFrame(maxEnemies, maxBullets, maxBlasts)

    fun start() = withHandle { nativeStart(it) }

    fun setPaused(paused: Boolean) = withHandle { nativeSetPaused(it, paused) }

    /** Size of the game canvas in pixels. */
    fun setScreen(width: Float, height: Float) = withHandle { nativeSetScreen(it, width, height) }

    /** Joystick position, each axis -1..1 (x right, y down). (0, 0) = released. */
    fun setJoystick(x: Float, y: Float) = withHandle { nativeSetJoystick(it, x, y) }

    fun restart() = withHandle { nativeRestart(it) }

    /** Copies the latest, smoothly interpolated state into [frame]. */
    fun readFrame(): SurvivorFrame {
        withHandle { frame.length = nativeGetSnapshot(it, frame.data) }
        return frame
    }

    override fun close() {
        val h = handle
        if (h != 0L) {
            handle = 0L
            nativeDestroy(h)   // stops and joins the C++ logic thread
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
        maxBlasts: Int,
    ): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeStart(handle: Long)
    private external fun nativeSetPaused(handle: Long, paused: Boolean)
    private external fun nativeSetScreen(handle: Long, width: Float, height: Float)
    private external fun nativeSetJoystick(handle: Long, x: Float, y: Float)
    private external fun nativeRestart(handle: Long)
    private external fun nativeGetSnapshot(handle: Long, out: FloatArray): Int

    companion object {
        init {
            System.loadLibrary("mychatroom")
        }
    }
}

/**
 * One frame of Space Survivor. Layout matches SurvivorLoop::writeSnapshot:
 * [state, playerX, playerY, playerAngle, playerSize, enemySize, enemyCount,
 *  bulletSize, bulletCount, blastSize, blastCount, kills, secondsSurvived,
 *  enemy x,y,animTime,hitFlash ..., bullet x,y,angle ..., blast x,y,progress ...]
 * Positions are WORLD coordinates in pixels; the screen centres the camera on the player.
 */
class SurvivorFrame(maxEnemies: Int, maxBullets: Int, maxBlasts: Int) {
    internal val data = FloatArray(HEADER_SIZE + maxEnemies * 4 + maxBullets * 3 + maxBlasts * 3)
    internal var length = 0

    val isPaused: Boolean get() = data[0].toInt() == STATE_PAUSED
    val isGameOver: Boolean get() = data[0].toInt() == STATE_GAME_OVER
    val playerX: Float get() = data[1]
    val playerY: Float get() = data[2]
    /** Radians, 0 = facing up, clockwise. */
    val playerAngle: Float get() = data[3]
    val playerSize: Float get() = data[4]
    val enemySize: Float get() = data[5]
    val enemyCount: Int get() = if (length > 0) data[6].toInt() else 0
    val bulletSize: Float get() = data[7]
    val bulletCount: Int get() = if (length > 0) data[8].toInt() else 0
    val blastSize: Float get() = data[9]
    val blastCount: Int get() = if (length > 0) data[10].toInt() else 0
    val kills: Int get() = data[11].toInt()
    val secondsSurvived: Float get() = data[12]

    /** False until the engine has a screen size and has run its first tick. */
    val isReady: Boolean get() = length > 0 && playerSize > 0f

    // Enemies: 4 floats each, right after the header.
    private fun enemyBase(i: Int) = HEADER_SIZE + i * 4
    fun enemyX(i: Int): Float = data[enemyBase(i)]
    fun enemyY(i: Int): Float = data[enemyBase(i) + 1]
    fun enemyAnimTime(i: Int): Float = data[enemyBase(i) + 2]
    fun enemyFlashing(i: Int): Boolean = data[enemyBase(i) + 3] > 0.5f

    // Bullets: 3 floats each, after the enemies.
    private fun bulletBase(i: Int) = enemyBase(enemyCount) + i * 3
    fun bulletX(i: Int): Float = data[bulletBase(i)]
    fun bulletY(i: Int): Float = data[bulletBase(i) + 1]
    /** Flight direction, radians, 0 = up, clockwise. */
    fun bulletAngle(i: Int): Float = data[bulletBase(i) + 2]

    // Blasts: 3 floats each, last.
    private fun blastBase(i: Int) = bulletBase(bulletCount) + i * 3
    fun blastX(i: Int): Float = data[blastBase(i)]
    fun blastY(i: Int): Float = data[blastBase(i) + 1]
    fun blastProgress(i: Int): Float = data[blastBase(i) + 2]

    companion object {
        const val STATE_PAUSED = 1
        const val STATE_GAME_OVER = 2
        const val HEADER_SIZE = 13
    }
}
