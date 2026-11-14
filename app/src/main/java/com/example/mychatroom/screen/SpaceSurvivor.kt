package com.example.mychatroom.screen

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mychatroom.R
import com.example.mychatroom.viewModel.SurvivorViewModel
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Space Survivor: fly the ship around an endless starfield with an on-screen
 * joystick while UFO aliens swarm in from off-screen and chase you. The gun
 * fires on its own at the nearest alien. The camera follows the ship, so the
 * ship stays in the middle of the screen and the world moves around it.
 *
 * Same two-loop design as AlienGame: the C++ thread runs the rules at 60
 * ticks/s, and the UI loop below redraws once per display frame.
 */
@Composable
fun SpaceSurvivor(
    modifier: Modifier = Modifier,
    survivorViewModel: SurvivorViewModel = viewModel(),
) {
    val engine = survivorViewModel.engine
    // Changing this every frame redraws only the Canvas, not the whole screen.
    var frameTimeNanos by remember { mutableLongStateOf(0L) }
    var hasMoved by remember { mutableStateOf(false) }   // hide the hint once the stick is used
    // HUD values: these change rarely, so the text only recomposes when they do.
    var kills by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(0) }
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

    // UI loop: read the newest frame from C++ once per display refresh.
    LaunchedEffect(engine) {
        while (isActive) {
            withFrameNanos { now ->
                val frame = engine.readFrame()
                if (frame.kills != kills) kills = frame.kills
                val s = frame.secondsSurvived.toInt()
                if (s != seconds) seconds = s
                if (frame.isGameOver != gameOver) gameOver = frame.isGameOver
                frameTimeNanos = now
            }
        }
    }

    val ship = ImageBitmap.imageResource(R.drawable.player_ship)
    // The UFO sheets from Alien Shooter (drawing helpers in AlienSprites.kt).
    val alienHover = ImageBitmap.imageResource(R.drawable.alien_hover)
    val alienHit = ImageBitmap.imageResource(R.drawable.alien_hit)
    val alienBoom = ImageBitmap.imageResource(R.drawable.alien_boom)

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
                    detectTapGestures {
                        if (engine.frame.isGameOver) engine.restart()   // tap to play again
                    }
                }
        ) {
            frameTimeNanos   // read the state so this block redraws every frame

            drawRect(SpaceBackground)
            val frame = engine.frame
            if (!frame.isReady) return@Canvas

            // Camera: the world point at the top-left corner of the screen.
            // screen = world - camera, which puts the ship exactly in the middle.
            val cameraX = frame.playerX - size.width / 2f
            val cameraY = frame.playerY - size.height / 2f

            // Two star layers: the far one moves slower (parallax = depth).
            drawStarLayer(cameraX, cameraY, parallax = 0.4f, cell = 180f, salt = 11, maxRadius = 1.6f, alpha = 0.45f)
            drawStarLayer(cameraX, cameraY, parallax = 1.0f, cell = 260f, salt = 97, maxRadius = 2.8f, alpha = 0.9f)

            // World objects: screen position = world position - camera.
            // Anything further than `margin` outside the screen is skipped.
            val margin = frame.enemySize * 2f
            fun onScreen(x: Float, y: Float) =
                x > -margin && x < size.width + margin && y > -margin && y < size.height + margin

            for (i in 0 until frame.enemyCount) {
                val x = frame.enemyX(i) - cameraX
                val y = frame.enemyY(i) - cameraY
                if (!onScreen(x, y)) continue
                val sheet = if (frame.enemyFlashing(i)) alienHit else alienHover
                drawAlienHover(sheet, x, y, frame.enemySize, frame.enemyAnimTime(i))
            }
            for (i in 0 until frame.blastCount) {
                val x = frame.blastX(i) - cameraX
                val y = frame.blastY(i) - cameraY
                if (!onScreen(x, y)) continue
                drawAlienBoom(alienBoom, x, y, frame.blastSize, frame.blastProgress(i))
            }
            for (i in 0 until frame.bulletCount) {
                drawBolt(
                    centerX = frame.bulletX(i) - cameraX,
                    centerY = frame.bulletY(i) - cameraY,
                    width = frame.bulletSize,
                    degrees = frame.bulletAngle(i) * 180f / PI.toFloat(),
                )
            }

            // The ship, rotated to face where it's flying (0 = up, clockwise).
            drawShip(ship, center, frame.playerSize, degrees = frame.playerAngle * 180f / PI.toFloat())
        }

        // HUD: time survived and kills.
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Text(
                text = "%d:%02d".format(seconds / 60, seconds % 60),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Kills $kills",
                color = BoltCore,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (gameOver) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Game over",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "Survived %d:%02d  ·  %d kills".format(seconds / 60, seconds % 60, kills),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Tap to play again",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else if (!hasMoved) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Space Survivor",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Drag the stick to fly",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Joystick(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            onMove = { x, y ->
                if (x != 0f || y != 0f) hasMoved = true
                engine.setJoystick(x, y)
            },
        )
    }
}

/**
 * Fixed on-screen joystick. Reports the knob position as (x, y), each -1..1
 * (x right, y down), and (0, 0) when released.
 *
 * It reacts the moment a finger lands (no "drag slop" wait), the knob follows
 * the finger but never leaves the ring, and the finger is tracked by its
 * absolute position, so pulling back after going past the edge feels natural.
 */
@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    baseRadius: Dp = 64.dp,
    knobRadius: Dp = 28.dp,
    onMove: (x: Float, y: Float) -> Unit,
) {
    val currentOnMove by rememberUpdatedState(onMove)   // always call the latest lambda
    var knob by remember { mutableStateOf(Offset.Zero) }  // knob offset from the centre, px
    val baseR = with(LocalDensity.current) { baseRadius.toPx() }
    val knobR = with(LocalDensity.current) { knobRadius.toPx() }
    val ringColor = MaterialTheme.colorScheme.primary
    val knobBrush = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )

    Canvas(
        modifier = modifier
            .size(baseRadius * 2)
            .pointerInput(baseR) {
                val centre = Offset(size.width / 2f, size.height / 2f)
                fun moveTo(finger: Offset) {
                    var v = finger - centre
                    val len = sqrt(v.x * v.x + v.y * v.y)
                    if (len > baseR) v *= baseR / len          // keep the knob inside the ring
                    knob = v
                    currentOnMove(v.x / baseR, v.y / baseR)    // -1..1 each
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    moveTo(down.position)
                    down.consume()
                    // follows this finger until it lifts
                    drag(down.id) { change ->
                        moveTo(change.position)
                        change.consume()
                    }
                    knob = Offset.Zero                         // released: spring back, stop
                    currentOnMove(0f, 0f)
                }
            }
    ) {
        drawCircle(Color.White.copy(alpha = 0.08f), radius = baseR)
        drawCircle(ringColor.copy(alpha = 0.5f), radius = baseR - 2f, style = Stroke(width = 3f))
        drawCircle(knobBrush, radius = knobR, center = center + knob)
    }
}

private val SpaceBackground = Color(0xFF070514)
private val BoltCore = Color(0xFFFFF59D)   // pale yellow, same as Alien Shooter's bullets
private val BoltGlow = Color(0x55FFB74D)   // soft orange halo

/** A glowing bolt [width] wide and 3x as long, pointing along [degrees] (0 = up, clockwise). */
private fun DrawScope.drawBolt(centerX: Float, centerY: Float, width: Float, degrees: Float) {
    val length = width * 3f
    val c = Offset(centerX, centerY)
    drawCircle(BoltGlow, radius = width * 1.6f, center = c)
    rotate(degrees = degrees, pivot = c) {
        drawRoundRect(
            color = BoltCore,
            topLeft = Offset(centerX - width / 2f, centerY - length / 2f),
            size = Size(width, length),
            cornerRadius = CornerRadius(width / 2f),
        )
    }
}

/** Draws [image] as a [spriteSize] square centred on [at], rotated by [degrees]. */
private fun DrawScope.drawShip(image: ImageBitmap, at: Offset, spriteSize: Float, degrees: Float) {
    val side = spriteSize.roundToInt()
    rotate(degrees = degrees, pivot = at) {
        drawImage(
            image = image,
            dstOffset = IntOffset((at.x - spriteSize / 2f).roundToInt(), (at.y - spriteSize / 2f).roundToInt()),
            dstSize = IntSize(side, side),
        )
    }
}

/**
 * An endless field of stars without storing any: the world is split into
 * [cell]-sized squares and each square's stars come from a hash of its
 * coordinates, so the same square always gets the same stars. Only the
 * squares on screen are drawn.
 *
 * [parallax] < 1 makes the layer move slower than the ship (further away).
 */
private fun DrawScope.drawStarLayer(
    cameraX: Float,
    cameraY: Float,
    parallax: Float,
    cell: Float,
    salt: Int,
    maxRadius: Float,
    alpha: Float,
    starsPerCell: Int = 3,
) {
    val camX = cameraX * parallax
    val camY = cameraY * parallax
    val firstCol = floor(camX / cell).toInt()
    val lastCol = floor((camX + size.width) / cell).toInt()
    val firstRow = floor(camY / cell).toInt()
    val lastRow = floor((camY + size.height) / cell).toInt()
    for (col in firstCol..lastCol) {
        for (row in firstRow..lastRow) {
            for (k in 0 until starsPerCell) {
                val seed = salt + k * 7
                val x = (col + hash01(col, row, seed)) * cell - camX
                val y = (row + hash01(col, row, seed + 1)) * cell - camY
                val shade = hash01(col, row, seed + 2)
                drawCircle(
                    color = Color.White.copy(alpha = alpha * (0.4f + 0.6f * shade)),
                    radius = maxRadius * (0.4f + 0.6f * shade),
                    center = Offset(x, y),
                )
            }
        }
    }
}

/** Stable pseudo-random number 0..1 from integer coordinates (same input = same output). */
private fun hash01(x: Int, y: Int, seed: Int): Float {
    var h = x * 374761393 + y * 668265263 + seed * 1442695041
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFFFF) / 16777215f
}
