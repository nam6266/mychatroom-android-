package com.example.mychatroom.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mychatroom.R
import com.example.mychatroom.engine.GameEngine
import com.example.mychatroom.viewModel.GameViewModel
import kotlinx.coroutines.delay
import java.io.InputStream
import kotlin.math.sqrt
import androidx.core.graphics.scale


@Composable
fun GameScreen(gameViewModel: GameViewModel = viewModel()) {

    val engine = remember { GameEngine() }
    var playerPosition by remember { mutableStateOf(Offset(engine.getPosition(), 0.0f)) }
    var enemyPositions by remember { mutableStateOf<List<Offset>>(emptyList()) }

    var isRuning by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }

    val enemyBitmap = createBitmapFromPng(LocalContext.current, R.drawable.metero)
    val enemyImgBitMap = enemyBitmap?.let { scaleBitmap(it, 200, 200) }

    val playerBitmap = createBitmapFromPng(LocalContext.current, R.drawable.rocket)
    val playerImgBitmap = playerBitmap?.let { scaleBitmap(it, 200, 200) }

    val backgroundBitmap = createBitmapFromPng(LocalContext.current, R.drawable.background)
    var scaledBackground: ImageBitmap? = null

    LaunchedEffect(Unit) {
        while (isRuning) {
            playerPosition = playerPosition.copy(x = engine.getPosition())
            val isCollision = engine.updateEnemy(playerPosition.x)
            val enemyPosition = engine.getEnemy()
            enemyPositions = enemyPosition.map { Offset(it[0], it[1]) }

            if (isCollision) {
                isRuning = false
                showDialog = true
            }

            delay(16)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { size ->
                engine.setScreen(size.width.toFloat(), size.height.toFloat())
                if (playerImgBitmap != null) {
                    playerPosition = playerPosition.copy(y = size.height.toFloat() - playerImgBitmap.height.toFloat()/ 2)
                }
                scaledBackground = backgroundBitmap?.let { bitmap ->
                    scaleBitmap(bitmap, size.width, size.height) // Match canvas size
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    engine.update(dragAmount.x)
                    change.consume()
                }
            }) {
        drawIntoCanvas {
            scaledBackground?.let {
                drawImage(
                    image = scaledBackground!!,
                    topLeft = Offset.Zero
                )
            }
        }

        for (enemy in enemyPositions) {
            if (enemyImgBitMap != null) {
                drawImage(
                    image = enemyImgBitMap,
                    topLeft = Offset(
                        enemy.x - enemyImgBitMap.width / 2,
                        enemy.y - enemyImgBitMap.height / 2
                    )
                )
                drawCircle(Color.White, radius = 20.0f, center= enemy)
            }
        }
        if (playerImgBitmap != null) {
            drawImage(
                image = playerImgBitmap,
                topLeft = Offset(
                    playerPosition.x - playerImgBitmap.width / 2,
                    playerPosition.y - playerImgBitmap.height / 2
                )
            )
            drawCircle(Color.Red, radius = 20.0f, center= playerPosition)
        }

    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            },
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "You Lose",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

fun createBitmapFromPng(context: Context, resourceId: Int): Bitmap? {
    return try {
        val inputStream: InputStream = context.resources.openRawResource(resourceId)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun scaleBitmap(
    bitmap: Bitmap,
    newWidth: Int,
    newHeight: Int,
    filter: Boolean = true
): ImageBitmap {
    val scaledBitmap = bitmap.scale(newWidth, newHeight, filter)
    return scaledBitmap.asImageBitmap()
}