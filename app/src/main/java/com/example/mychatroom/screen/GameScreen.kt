package com.example.mychatroom.screen

import androidx.compose.ui.graphics.FilterQuality
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
fun GameScreen(
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
                drawUpgradeOrb(frame.powerUpX(i), frame.powerUpY(i), frame.powerUpSize, frameTimeNanos)
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

// ---- UFO alien sprite sheets (res/drawable-nodpi), one row of equal cells each ----
// Every cell has the saucer's bottom-centre ("anchor") at the same spot, so the
// explosion lines up exactly with where the UFO was.
private const val ALIEN_ART_WIDTH = 44f        // UFO width in sheet pixels = alienSize on screen
private const val HOVER_FRAMES = 16
private const val HOVER_CELL_W = 54
private const val HOVER_CELL_H = 41
private const val HOVER_ANCHOR_X = 27
private const val HOVER_ANCHOR_Y = 40
private const val HOVER_FPS = 12f              // 16 frames -> loops every ~1.3 s
private const val BOOM_FRAMES = 22
private const val BOOM_CELL_W = 78
private const val BOOM_CELL_H = 59
private const val BOOM_ANCHOR_X = 39
private const val BOOM_ANCHOR_Y = 58

/** Draws cell [index] of a one-row sheet with its top-left at (left, top), scaled by [scale]. */
private fun DrawScope.drawCell(
    sheet: ImageBitmap, cellW: Int, cellH: Int, index: Int, left: Float, top: Float, scale: Float,
) {
    drawImage(
        image = sheet,
        srcOffset = IntOffset(index * cellW, 0),
        srcSize = IntSize(cellW, cellH),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize((cellW * scale).roundToInt(), (cellH * scale).roundToInt()),
        filterQuality = FilterQuality.None,   // pixel art: crisp pixels, no blur
    )
}

/** UFO alien [index], playing its hover loop; (x, y) is the centre of the hover cell. */
private fun DrawScope.drawAlien(sheet: ImageBitmap, frame: GameFrame, index: Int) {
    val scale = frame.alienSize / ALIEN_ART_WIDTH
    val cell = (frame.alienAnimTime(index) * HOVER_FPS).toInt().mod(HOVER_FRAMES)
    drawCell(
        sheet, HOVER_CELL_W, HOVER_CELL_H, cell,
        left = frame.alienX(index) - HOVER_CELL_W * scale / 2f,
        top = frame.alienY(index) - HOVER_CELL_H * scale / 2f,
        scale = scale,
    )
}

/** UFO destruction [index]: 22 frames, anchored where the UFO's saucer was. */
private fun DrawScope.drawAlienBlast(sheet: ImageBitmap, frame: GameFrame, index: Int) {
    val scale = frame.alienBlastSize / ALIEN_ART_WIDTH
    val cell = (frame.alienBlastProgress(index) * BOOM_FRAMES).toInt().coerceIn(0, BOOM_FRAMES - 1)
    // saucer anchor on screen, same as in the hover sheet
    val anchorX = frame.alienBlastX(index) + (HOVER_ANCHOR_X - HOVER_CELL_W / 2f) * scale
    val anchorY = frame.alienBlastY(index) + (HOVER_ANCHOR_Y - HOVER_CELL_H / 2f) * scale
    drawCell(
        sheet, BOOM_CELL_W, BOOM_CELL_H, cell,
        left = anchorX - BOOM_ANCHOR_X * scale,
        top = anchorY - BOOM_ANCHOR_Y * scale,
        scale = scale,
    )
}

private val OrbCore = Color(0xFFFF8A80)   // light red
private val OrbEdge = Color(0xFFD50000)   // deep red
private val OrbGlow = Color(0xFFFF1744)   // red halo

/**
 * Weapon upgrade: a pulsing red glow, a shaded red ball and a white "+".
 * [size] is the orb's diameter; [timeNanos] drives the pulse.
 */
private fun DrawScope.drawUpgradeOrb(centerX: Float, centerY: Float, size: Float, timeNanos: Long) {
    val radius = size / 2f
    val center = Offset(centerX, centerY)
    // 0..1, about 1.5 pulses per second
    val pulse = 0.5f + 0.5f * sin(timeNanos / 1_000_000_000.0 * 2.0 * PI * 1.5).toFloat()

    // a) halo that breathes in and out
    val glowRadius = radius * (2.0f + 0.4f * pulse)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(OrbGlow.copy(alpha = 0.35f + 0.25f * pulse), Color.Transparent),
            center = center,
            radius = glowRadius,
        ),
        radius = glowRadius,
        center = center,
    )
    // b) ball, lit from the top-left so it looks round
    drawCircle(
        brush = Brush.radialGradient(
            listOf(OrbCore, OrbEdge),
            center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f),
            radius = radius * 1.3f,
        ),
        radius = radius,
        center = center,
    )
    // c) white "+"
    val arm = radius * 0.5f
    val stroke = radius * 0.24f
    drawLine(Color.White, Offset(centerX - arm, centerY), Offset(centerX + arm, centerY), stroke, StrokeCap.Round)
    drawLine(Color.White, Offset(centerX, centerY - arm), Offset(centerX, centerY + arm), stroke, StrokeCap.Round)
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
