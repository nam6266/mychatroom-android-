package com.example.mychatroom.screen

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
                    if (frame.isGameOver != gameOver) gameOver = frame.isGameOver
                    frameTimeNanos = now
                }
            }
        }
    }

    val background = ImageBitmap.imageResource(R.drawable.background)
    val rocket = ImageBitmap.imageResource(R.drawable.rocket)
    val meteor = ImageBitmap.imageResource(R.drawable.metero)

    Box(modifier.fillMaxSize()) {
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
            drawSprite(rocket, frame.playerX, frame.playerY, frame.playerSize)
        }

        Text(
            text = "Score: $score",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp),
        )

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
