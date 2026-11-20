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
    // The ultimate barrage keeps ~70 shots in the air (100 per second, each lives ~0.7 s).
    val maxBullets: Int = 128,
    val maxBlasts: Int = 12,
    val maxOrbs: Int = 150,
) : AutoCloseable {

    private var handle: Long = nativeCreate(ticksPerSecond, maxEnemies, maxBullets, maxBlasts, maxOrbs)

    /** Reused every frame, so rendering allocates nothing. */
    val frame = SurvivorFrame(maxEnemies, maxBullets, maxBlasts, maxOrbs)

    fun start() = withHandle { nativeStart(it) }

    fun setPaused(paused: Boolean) = withHandle { nativeSetPaused(it, paused) }

    /** Size of the game canvas in pixels. */
    fun setScreen(width: Float, height: Float) = withHandle { nativeSetScreen(it, width, height) }

    /** Joystick position, each axis -1..1 (x right, y down). (0, 0) = released. */
    fun setJoystick(x: Float, y: Float) = withHandle { nativeSetJoystick(it, x, y) }

    fun restart() = withHandle { nativeRestart(it) }

    /** The player tapped the level-up card: pass [SurvivorFrame.offeredUpgrade]. */
    fun chooseUpgrade(upgrade: Int) = withHandle { nativeChooseUpgrade(it, upgrade) }

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
        maxOrbs: Int,
    ): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeStart(handle: Long)
    private external fun nativeSetPaused(handle: Long, paused: Boolean)
    private external fun nativeSetScreen(handle: Long, width: Float, height: Float)
    private external fun nativeSetJoystick(handle: Long, x: Float, y: Float)
    private external fun nativeRestart(handle: Long)
    private external fun nativeChooseUpgrade(handle: Long, upgrade: Int)
    private external fun nativeGetSnapshot(handle: Long, out: FloatArray): Int

    companion object {
        init {
            System.loadLibrary("mychatroom")
        }
    }
}

/**
 * One frame of Space Survivor. Layout matches SurvivorLoop::copySnapshotTo:
 * [state, playerX, playerY, playerAngle, playerSize, enemySize, enemyCount,
 *  bulletThickness, bulletCount, blastSize, blastCount, kills, secondsSurvived,
 *  level, exp, expNeeded, laserLevel, ultimate, offeredUpgrade, bulletLength,
 *  beamAngle, beamStart, beamLength, beamThickness, orbSize, orbCount, nukeFlash,
 *  enemy x,y,animTime,hitFlash ..., bullet x,y,angle ..., blast x,y,progress ..., orb x,y,kind ...]
 * Positions are WORLD coordinates in pixels; the screen centres the camera on the player.
 */
class SurvivorFrame(maxEnemies: Int, maxBullets: Int, maxBlasts: Int, maxOrbs: Int) {
    internal val data = FloatArray(
        HEADER_SIZE + maxEnemies * 4 + maxBullets * 3 + maxBlasts * 3 + maxOrbs * 3
    )
    internal var length = 0

    val isPaused: Boolean get() = data[0].toInt() == STATE_PAUSED
    val isGameOver: Boolean get() = data[0].toInt() == STATE_GAME_OVER
    /** A level-up card is waiting for the player; the round is frozen meanwhile. */
    val isLevelUp: Boolean get() = data[0].toInt() == STATE_LEVEL_UP
    val playerX: Float get() = data[1]
    val playerY: Float get() = data[2]
    /** Radians, 0 = facing up, clockwise. */
    val playerAngle: Float get() = data[3]
    val playerSize: Float get() = data[4]
    val enemySize: Float get() = data[5]
    val enemyCount: Int get() = if (length > 0) data[6].toInt() else 0
    /** Drawn thickness of a laser shot (grows with [laserLevel]). */
    val bulletThickness: Float get() = data[7]
    val bulletCount: Int get() = if (length > 0) data[8].toInt() else 0
    val blastSize: Float get() = data[9]
    val blastCount: Int get() = if (length > 0) data[10].toInt() else 0
    val kills: Int get() = data[11].toInt()
    val secondsSurvived: Float get() = data[12]

    // Progression
    val level: Int get() = data[13].toInt()
    val exp: Int get() = data[14].toInt()
    val expNeeded: Int get() = data[15].toInt()
    /** 1..6: which laser sprite to draw. */
    val laserLevel: Int get() = data[16].toInt().coerceIn(1, 6)
    /** [ULTIMATE_NONE], [ULTIMATE_BARRAGE] (shots in every direction) or [ULTIMATE_BEAMS]. */
    val ultimateStyle: Int get() = data[17].toInt()
    /** What the level-up card offers: [UPGRADE_NONE], [UPGRADE_LASER] or [UPGRADE_ULTIMATE]. */
    val offeredUpgrade: Int get() = data[18].toInt()
    val bulletLength: Float get() = data[19]

    // Ultimate style BEAMS only: 4 beams, 90 degrees apart, around the ship.
    /** Beam 0's direction, radians, 0 = up, clockwise. */
    val beamAngle: Float get() = data[20]
    val beamStart: Float get() = data[21]
    val beamLength: Float get() = data[22]
    val beamThickness: Float get() = data[23]

    val orbSize: Float get() = data[24]
    val orbCount: Int get() = if (length > 0) data[25].toInt() else 0
    /** 1 the moment a red Nuke orb is picked up, fading to 0: draw a white flash. */
    val nukeFlash: Float get() = data[26]

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

    // Orbs: 3 floats each, last.
    private fun orbBase(i: Int) = blastBase(blastCount) + i * 3
    fun orbX(i: Int): Float = data[orbBase(i)]
    fun orbY(i: Int): Float = data[orbBase(i) + 1]
    /** True for the red Nuke orb (clears the screen), false for a green EXP orb. */
    fun orbIsNuke(i: Int): Boolean = data[orbBase(i) + 2] > 0.5f

    companion object {
        const val STATE_PAUSED = 1
        const val STATE_GAME_OVER = 2
        const val STATE_LEVEL_UP = 3
        const val UPGRADE_NONE = 0
        const val UPGRADE_LASER = 1
        const val UPGRADE_ULTIMATE = 2
        const val ULTIMATE_NONE = 0
        const val ULTIMATE_BARRAGE = 1
        const val ULTIMATE_BEAMS = 2
        const val HEADER_SIZE = 27
        const val BEAM_COUNT = 4
    }
}
