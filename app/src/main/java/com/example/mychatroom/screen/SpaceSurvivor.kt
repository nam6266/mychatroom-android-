package com.example.mychatroom.screen

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import com.example.mychatroom.engine.SurvivorFrame
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    var level by remember { mutableIntStateOf(1) }
    var expProgress by remember { mutableFloatStateOf(0f) }   // 0..1 towards the next level
    var laserLevel by remember { mutableIntStateOf(1) }
    // What the level-up card offers right now (SurvivorFrame.UPGRADE_*), NONE = no card.
    var offered by remember { mutableIntStateOf(SurvivorFrame.UPGRADE_NONE) }
    var isPaused by remember { mutableStateOf(false) }   // paused by the player (pause button)
    // The lifecycle observer below is created once, so it reads the latest value through this.
    val pausedByPlayer by rememberUpdatedState(isPaused)

    fun setPausedByPlayer(paused: Boolean) {
        isPaused = paused
        if (paused) engine.setJoystick(0f, 0f)   // don't keep flying with the old stick value on resume
        engine.setPaused(paused)
    }

    // Pause the C++ thread when the app goes to the background or the screen leaves.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, engine) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // Coming back from the background: stay paused if the player had paused.
                Lifecycle.Event.ON_RESUME -> engine.setPaused(pausedByPlayer)
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
                if (frame.level != level) level = frame.level
                val progress = if (frame.expNeeded > 0) frame.exp.toFloat() / frame.expNeeded else 0f
                if (progress != expProgress) expProgress = progress
                if (frame.laserLevel != laserLevel) laserLevel = frame.laserLevel
                val card = if (frame.isLevelUp) frame.offeredUpgrade else SurvivorFrame.UPGRADE_NONE
                if (card != offered) offered = card
                frameTimeNanos = now
            }
        }
    }

    val ship = ImageBitmap.imageResource(R.drawable.player_ship)
    // The UFO sheets from Alien Shooter (drawing helpers in AlienSprites.kt).
    val alienHover = ImageBitmap.imageResource(R.drawable.alien_hover)
    val alienHit = ImageBitmap.imageResource(R.drawable.alien_hit)
    val alienBoom = ImageBitmap.imageResource(R.drawable.alien_boom)
    // Laser shots, level 1..6 (index 0..5): each one thicker than the last.
    val lasers = listOf(
        R.drawable.laser_lv1, R.drawable.laser_lv2, R.drawable.laser_lv3,
        R.drawable.laser_lv4, R.drawable.laser_lv5, R.drawable.laser_lv6,
    ).map { ImageBitmap.imageResource(it) }

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
                        if (engine.frame.isGameOver) {
                            setPausedByPlayer(false)
                            engine.restart()   // tap to play again
                        }
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

            // EXP orbs first, so aliens fly over them.
            for (i in 0 until frame.orbCount) {
                val x = frame.orbX(i) - cameraX
                val y = frame.orbY(i) - cameraY
                if (!onScreen(x, y)) continue
                if (frame.orbIsNuke(i)) {
                    drawOrb(x, y, frame.orbSize * 1.4f, NukeCore, NukeGlow)   // a bit bigger: easy to spot
                } else {
                    drawOrb(x, y, frame.orbSize, OrbCore, OrbGlow)
                }
            }
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
            val shot = lasers[frame.laserLevel - 1]
            for (i in 0 until frame.bulletCount) {
                drawLaser(
                    image = shot,
                    centerX = frame.bulletX(i) - cameraX,
                    centerY = frame.bulletY(i) - cameraY,
                    length = frame.bulletLength,
                    thickness = frame.bulletThickness,
                    degrees = frame.bulletAngle(i) * 180f / PI.toFloat(),
                )
            }

            // Ultimate style BEAMS (kept for later): 4 yellow beams spinning around the ship.
            // The BARRAGE style needs nothing extra here: its shots are ordinary bullets.
            if (frame.ultimateStyle == SurvivorFrame.ULTIMATE_BEAMS) {
                for (k in 0 until SurvivorFrame.BEAM_COUNT) {
                    val a = frame.beamAngle + k * (2f * PI.toFloat() / SurvivorFrame.BEAM_COUNT)
                    val mid = frame.beamStart + frame.beamLength / 2f
                    drawLaser(
                        image = lasers[5],
                        centerX = center.x + sin(a) * mid,     // 0 = up, clockwise: x = sin, y = -cos
                        centerY = center.y - cos(a) * mid,
                        length = frame.beamLength,
                        thickness = frame.beamThickness,
                        degrees = a * 180f / PI.toFloat(),
                    )
                }
            }

            // The ship, rotated to face where it's flying (0 = up, clockwise).
            drawShip(ship, center, frame.playerSize, degrees = frame.playerAngle * 180f / PI.toFloat())

            // Red orb picked up: the whole screen flashes white, then fades.
            if (frame.nukeFlash > 0f) {
                drawRect(Color.White.copy(alpha = 0.7f * frame.nukeFlash))
            }
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
            Text(
                text = "Lv $level",
                color = OrbCore,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            ExpBar(progress = expProgress)
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

        if (!gameOver && offered == SurvivorFrame.UPGRADE_NONE) {
            PauseButton(
                paused = isPaused,
                onClick = { setPausedByPlayer(!isPaused) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            )
        }

        if (isPaused && !gameOver) {
            PausedOverlay(onResume = { setPausedByPlayer(false) })
        }

        if (offered != SurvivorFrame.UPGRADE_NONE && !isPaused && !gameOver) {
            LevelUpCard(
                offered = offered,
                laserLevel = laserLevel,
                lasers = lasers,
                onChoose = {
                    engine.chooseUpgrade(offered)
                    offered = SurvivorFrame.UPGRADE_NONE   // hide at once; the next frame confirms
                },
            )
        }
    }
}

/** Thin bar under the level: how much EXP is collected towards the next level. */
@Composable
private fun ExpBar(progress: Float) {
    Box(
        Modifier
            .padding(top = 4.dp)
            .width(120.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.15f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .background(OrbCore),
        )
    }
}

/**
 * Shown when the player levels up. The C++ round is frozen until the card is
 * tapped. Today there is one choice (the next laser level, then the ultimate);
 * more upgrade types can be added as more cards in the same Column later.
 */
@Composable
private fun LevelUpCard(
    offered: Int,
    laserLevel: Int,
    lasers: List<ImageBitmap>,
    onChoose: () -> Unit,
) {
    val isUltimate = offered == SurvivorFrame.UPGRADE_ULTIMATE
    val preview = if (isUltimate) lasers[5] else lasers[laserLevel.coerceAtMost(5)]   // the NEXT level's sprite
    val title = if (isUltimate) "Ultimate laser" else "Laser Lv ${laserLevel + 1}"
    val description = if (isUltimate) {
        "A non-stop storm of lasers in every direction around your ship. Replaces your aimed shots."
    } else {
        "Thicker laser: more damage, faster fire and a bigger hitbox."
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { } },   // block the game underneath
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Level up!",
                color = OrbCore,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1740))
                    .clickable(onClick = onChoose)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    bitmap = preview,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(preview.width.toFloat() / preview.height),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = "Tap to choose",
                    color = OrbCore,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** Round, see-through button in the corner: two bars while playing, a play arrow while paused. */
@Composable
private fun PauseButton(paused: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .background(Color.White.copy(alpha = 0.15f), CircleShape),
    ) {
        if (paused) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color.White)
        } else {
            // The pause icon is not in the core icon set, so draw its two bars.
            Row {
                PauseBar()
                Box(Modifier.width(5.dp))
                PauseBar()
            }
        }
    }
}

@Composable
private fun PauseBar() {
    Box(
        Modifier
            .size(width = 5.dp, height = 18.dp)
            .background(Color.White, RoundedCornerShape(2.dp))
    )
}

/** Dims the game and resumes on any tap. It sits on top, so the joystick can't be used while paused. */
@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) { detectTapGestures { onResume() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Paused",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "Tap anywhere to resume",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
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
private val BoltCore = Color(0xFFFFF59D)   // pale yellow: the kill counter
private val OrbCore = Color(0xFF39FF6A)    // EXP green: orbs, level text, EXP bar
private val OrbGlow = Color(0x5539FF6A)
private val NukeCore = Color(0xFFFF3B3B)   // red Nuke orb
private val NukeGlow = Color(0x66FF3B3B)

/**
 * Draws a laser sprite [length] long and [thickness] thick, centred on
 * (centerX, centerY), with its glowing tip pointing along [degrees]
 * (0 = up, clockwise). The laser images point LEFT, so they get an extra
 * 90-degree turn first.
 */
private fun DrawScope.drawLaser(
    image: ImageBitmap,
    centerX: Float,
    centerY: Float,
    length: Float,
    thickness: Float,
    degrees: Float,
) {
    rotate(degrees = degrees + 90f, pivot = Offset(centerX, centerY)) {
        drawImage(
            image = image,
            dstOffset = IntOffset((centerX - length / 2f).roundToInt(), (centerY - thickness / 2f).roundToInt()),
            dstSize = IntSize(length.roundToInt().coerceAtLeast(1), thickness.roundToInt().coerceAtLeast(1)),
        )
    }
}

/** An orb: soft glow, bright core, small highlight. [size] = diameter. */
private fun DrawScope.drawOrb(x: Float, y: Float, size: Float, core: Color, glow: Color) {
    val c = Offset(x, y)
    drawCircle(glow, radius = size * 0.9f, center = c)
    drawCircle(core, radius = size * 0.45f, center = c)
    drawCircle(Color.White.copy(alpha = 0.8f), radius = size * 0.15f, center = Offset(x - size * 0.15f, y - size * 0.15f))
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
