package com.example.mychatroom.screen

import kotlin.math.ceil
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.clipToBounds
import kotlin.math.sin
import kotlin.math.PI
import com.example.mychatroom.engine.GameFrame
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mychatroom.R
import com.example.mychatroom.viewModel.GameViewModel
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/** Small slack so a 16.6 ms vsync isn't skipped for being a hair early. */
private const val FRAME_TOLERANCE_NANOS = 2_000_000L

/**
 * Two loops:
 *  - Logic loop: C++ thread inside GameEngine, fixed 60 ticks/s.
 *  - UI loop: [withFrameNanos] below, paced by the display's vsync and capped
 *    at [targetFps]. Each frame it reads a snapshot and redraws the Canvas.
 */
@Composable
fun AlienGame(
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel = viewModel(),
    targetFps: Int = 60,
) {
    val engine = gameViewModel.engine

    // Changing this every frame invalidates only the Canvas draw block,
    // not the whole screen.
    var frameTimeNanos by remember { mutableLongStateOf(0L) }
    // These only change occasionally, so the Text overlays recompose rarely.
    var score by remember { mutableIntStateOf(0) }
    var weaponLevel by remember { mutableIntStateOf(1) }
    var laserSecondsLeft by remember { mutableIntStateOf(0) }   // whole seconds, for the HUD
    var gameOver by remember { mutableStateOf(false) }

    // Pause the C++ thread when the app goes to the background or the screen leaves.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, engine) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> engine.setPaused(false)
                Lifecycle.Event.ON_PAUSE -> engine.setPaused(true)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            engine.setPaused(true)
        }
    }

    // UI loop.
    LaunchedEffect(engine, targetFps) {
        val frameIntervalNanos = 1_000_000_000L / targetFps
        var lastFrameNanos = 0L
        while (isActive) {
            withFrameNanos { now ->
                // On 90/120 Hz screens this skips vsyncs to stay near targetFps.
                if (now - lastFrameNanos >= frameIntervalNanos - FRAME_TOLERANCE_NANOS) {
                    lastFrameNanos = now
                    val frame = engine.readFrame()
                    if (frame.score != score) score = frame.score
                    if (frame.weaponLevel != weaponLevel) weaponLevel = frame.weaponLevel
                    val laserSeconds = ceil(frame.laserTimeLeft).toInt()
                    if (laserSeconds != laserSecondsLeft) laserSecondsLeft = laserSeconds
                    if (frame.isGameOver != gameOver) gameOver = frame.isGameOver
                    frameTimeNanos = now
                }
            }
        }
    }

    val background = ImageBitmap.imageResource(R.drawable.background)
    val rocket = ImageBitmap.imageResource(R.drawable.player_ship)
    val meteor = ImageBitmap.imageResource(R.drawable.metero)
    // Temporary explosion sprite sheet: BLAST_FRAMES frames side by side.
    val blastSheet = ImageBitmap.imageResource(R.drawable.blast_sheet)
    // UFO alien: hover loop + its own destruction, cut from the Metal Slug sheet.
    val alienHover = ImageBitmap.imageResource(R.drawable.alien_hover)
    val alienHit = ImageBitmap.imageResource(R.drawable.alien_hit)   // orange dome, same layout
    val alienBoom = ImageBitmap.imageResource(R.drawable.alien_boom)

    // clipToBounds: meteors spawn just above the top edge and bullets leave
    // through it; without clipping they'd be drawn over the top app bar.
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { engine.setScreen(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(engine) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        engine.movePlayerBy(dragAmount)
                    }
                }
                .pointerInput(engine) {
                    detectTapGestures {
                        if (engine.frame.isGameOver) engine.restart()
                    }
                }
        ) {
            frameTimeNanos // read the state so this block redraws every UI frame

            drawImage(
                image = background,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            )

            val frame = engine.frame
            if (!frame.isReady) return@Canvas

            for (i in 0 until frame.enemyCount) {
                drawSprite(meteor, frame.enemyX(i), frame.enemyY(i), frame.enemySize)
            }
            for (i in 0 until frame.alienCount) {
                drawAlien(if (frame.alienFlashing(i)) alienHit else alienHover, frame, i)
            }
            for (i in 0 until frame.alienBlastCount) {
                drawAlienBlast(alienBoom, frame, i)
            }
            for (i in 0 until frame.explosionCount) {
                drawExplosionSprite(blastSheet, frame, i)
            }
            for (i in 0 until frame.bulletCount) {
                drawBullet(frame.bulletX(i), frame.bulletY(i), frame.bulletSize)
            }
            for (i in 0 until frame.powerUpCount) {
                drawUpgradeOrb(
                    frame.powerUpX(i), frame.powerUpY(i), frame.powerUpSize, frameTimeNanos,
                    laser = frame.powerUpIsLaser(i),
                )
            }
            if (frame.laserTimeLeft > 0f) {
                // from the ship's nose up to wherever C++ says the beam stops
                drawLaser(
                    x = frame.playerX,
                    gunY = frame.playerY - frame.playerSize / 2f,
                    topY = frame.laserTopY,
                    width = frame.laserWidth,
                    timeNanos = frameTimeNanos,
                )
            }
            drawSprite(rocket, frame.playerX, frame.playerY, frame.playerSize)
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            Text(
                text = "Score: $score",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (weaponLevel >= GameFrame.MAX_WEAPON_LEVEL) "Gun MAX" else "Gun Lv $weaponLevel",
                color = OrbCore,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            if (laserSecondsLeft > 0) {
                Text(
                    text = "LASER ${laserSecondsLeft}s",
                    color = LaserGlow,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        if (gameOver) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = "Game over",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "Tap to play again",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

private val BulletCore = Color(0xFFFFF59D)   // pale yellow
private val BulletGlow = Color(0x55FFB74D)   // soft orange halo

/** A glowing laser bolt: a soft halo plus a bright capsule [width] wide and 3x as long. */
private fun DrawScope.drawBullet(centerX: Float, centerY: Float, width: Float) {
    val length = width * 3f
    drawCircle(
        color = BulletGlow,
        radius = width * 1.6f,
        center = Offset(centerX, centerY),
    )
    drawRoundRect(
        color = BulletCore,
        topLeft = Offset(centerX - width / 2f, centerY - length / 2f),
        size = Size(width, length),
        cornerRadius = CornerRadius(width / 2f),
    )
}

private const val BLAST_FRAMES = 10          // frames in blast_sheet.webp
private const val BLAST_ART_FRACTION = 0.8f  // the blast fills 80% of each frame

/**
 * Draws explosion [index] of [frame] from the horizontal sprite sheet
 * blast_sheet.webp: its progress (0..1) picks the frame, its seed rotates it
 * so explosions don't all look the same.
 */
private fun DrawScope.drawExplosionSprite(sheet: ImageBitmap, frame: GameFrame, index: Int) {
    val centerX = frame.explosionX(index)
    val centerY = frame.explosionY(index)
    val frameWidth = sheet.width / BLAST_FRAMES
    val sheetFrame = (frame.explosionProgress(index) * BLAST_FRAMES).toInt()
        .coerceIn(0, BLAST_FRAMES - 1)
    // explosionSize is the blast's width; the frame around it is a bit bigger
    val side = (frame.explosionSize / BLAST_ART_FRACTION).roundToInt()
    rotate(degrees = frame.explosionSeed(index) * 360f, pivot = Offset(centerX, centerY)) {
        drawImage(
            image = sheet,
            srcOffset = IntOffset(sheetFrame * frameWidth, 0),
            srcSize = IntSize(frameWidth, sheet.height),
            dstOffset = IntOffset((centerX - side / 2f).roundToInt(), (centerY - side / 2f).roundToInt()),
            dstSize = IntSize(side, side),
        )
    }
}

// ---- UFO alien: drawing lives in AlienSprites.kt (shared with Space Survivor) ----
private fun DrawScope.drawAlien(sheet: ImageBitmap, frame: GameFrame, index: Int) =
    drawAlienHover(sheet, frame.alienX(index), frame.alienY(index), frame.alienSize, frame.alienAnimTime(index))

private fun DrawScope.drawAlienBlast(sheet: ImageBitmap, frame: GameFrame, index: Int) =
    drawAlienBoom(sheet, frame.alienBlastX(index), frame.alienBlastY(index), frame.alienBlastSize, frame.alienBlastProgress(index))

// ---- Laser ----
private val LaserGlow = Color(0xFF00E5FF)   // cyan
private val LaserCore = Color(0xFFE0FFFF)   // almost white

/**
 * Laser beam from (x, gunY) straight up to topY, drawn as three stacked bars:
 * a wide faint glow, a brighter middle and a thin white-hot core. It shimmers
 * (width flickers 15x a second), flares at the nose, and sparks where it hits.
 */
private fun DrawScope.drawLaser(x: Float, gunY: Float, topY: Float, width: Float, timeNanos: Long) {
    if (gunY <= topY) return
    val seconds = timeNanos / 1_000_000_000.0
    val flicker = 0.85f + 0.15f * sin(seconds * 2.0 * PI * 15.0).toFloat()
    val w = width * flicker

    fun bar(color: Color, barWidth: Float) = drawRoundRect(
        color = color,
        topLeft = Offset(x - barWidth / 2f, topY),
        size = Size(barWidth, gunY - topY),
        cornerRadius = CornerRadius(barWidth / 2f),
    )
    bar(LaserGlow.copy(alpha = 0.18f), w * 2.6f)   // outer glow
    bar(LaserGlow.copy(alpha = 0.45f), w * 1.5f)   // middle
    bar(LaserCore, w * 0.55f)                      // core

    // muzzle flare at the ship's nose
    drawCircle(LaserGlow.copy(alpha = 0.5f), w * 1.3f, Offset(x, gunY))
    drawCircle(LaserCore, w * 0.6f, Offset(x, gunY))

    // impact flare where it hits something (topY = 0 means it hit nothing)
    if (topY > 0f) {
        drawCircle(LaserGlow.copy(alpha = 0.4f), w * (0.9f + 0.4f * flicker), Offset(x, topY))
        drawCircle(Color.White, w * 0.45f, Offset(x, topY))
    }
}

// ---- Orbs: red = gun upgrade, cyan = laser ----
private val OrbCore = Color(0xFFFF8A80)   // light red
private val OrbEdge = Color(0xFFD50000)   // deep red
private val OrbGlow = Color(0xFFFF1744)   // red halo
private val LaserOrbCore = Color(0xFF84FFFF)
private val LaserOrbEdge = Color(0xFF0091EA)

/**
 * Upgrade orb: a pulsing glow, a shaded ball and a white symbol. Red with "+"
 * for a gun upgrade, cyan with an up-arrow beam for the laser.
 * [size] is the orb's diameter; [timeNanos] drives the pulse.
 */
private fun DrawScope.drawUpgradeOrb(
    centerX: Float,
    centerY: Float,
    size: Float,
    timeNanos: Long,
    laser: Boolean = false,
) {
    val glowColor = if (laser) LaserGlow else OrbGlow
    val coreColor = if (laser) LaserOrbCore else OrbCore
    val edgeColor = if (laser) LaserOrbEdge else OrbEdge
    val radius = size / 2f
    val center = Offset(centerX, centerY)
    // 0..1, about 1.5 pulses per second
    val pulse = 0.5f + 0.5f * sin(timeNanos / 1_000_000_000.0 * 2.0 * PI * 1.5).toFloat()

    // a) halo that breathes in and out
    val glowRadius = radius * (2.0f + 0.4f * pulse)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(glowColor.copy(alpha = 0.35f + 0.25f * pulse), Color.Transparent),
            center = center,
            radius = glowRadius,
        ),
        radius = glowRadius,
        center = center,
    )
    // b) ball, lit from the top-left so it looks round
    drawCircle(
        brush = Brush.radialGradient(
            listOf(coreColor, edgeColor),
            center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f),
            radius = radius * 1.3f,
        ),
        radius = radius,
        center = center,
    )
    // c) white symbol
    val arm = radius * 0.5f
    val stroke = radius * 0.24f
    if (laser) {
        // a beam with an arrow head: "fires straight up"
        drawLine(Color.White, Offset(centerX, centerY + arm), Offset(centerX, centerY - arm), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(centerX, centerY - arm), Offset(centerX - arm * 0.6f, centerY - arm * 0.3f), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(centerX, centerY - arm), Offset(centerX + arm * 0.6f, centerY - arm * 0.3f), stroke, StrokeCap.Round)
    } else {
        drawLine(Color.White, Offset(centerX - arm, centerY), Offset(centerX + arm, centerY), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(centerX, centerY - arm), Offset(centerX, centerY + arm), stroke, StrokeCap.Round)
    }
}

/** Draws [image] as a square of [spriteSize] pixels centred on (centerX, centerY). */
private fun DrawScope.drawSprite(image: ImageBitmap, centerX: Float, centerY: Float, spriteSize: Float) {
    val side = spriteSize.roundToInt()
    drawImage(
        image = image,
        dstOffset = IntOffset(
            (centerX - spriteSize / 2f).roundToInt(),
            (centerY - spriteSize / 2f).roundToInt(),
        ),
        dstSize = IntSize(side, side),
    )
}
