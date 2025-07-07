package com.example.mychatroom

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.TextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mychatroom.screen.MainScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreen()
        }
    }
}

// this for test animation
@Preview
@Composable
fun PreviewMainScreen() {
    MainScreens()
}


@Composable
fun MainScreens() {
    val fields = listOf("Name", "Email", "Password")

    val scope = rememberCoroutineScope()
    var targetValue = 0f


    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val offsetX = this.maxWidth.value
        val offsetXAnimatables = remember {
            fields.map { Animatable(offsetX) } // Initial offset for each field
        }

        val offsetYAnimatables = remember {
            fields.map { Animatable(100f) }
        }
        Column() {
            fields.forEachIndexed { index, label ->
                // Get the specific Animatable for this field
                val offsetx = offsetXAnimatables[index]
                val offsetY = offsetYAnimatables[index]

                TextField(
                    value = "", // You'll likely want to manage state for TextField values
                    onValueChange = {},
                    label = { Text(label) },
                    modifier = Modifier
                        .width(200.dp)
                        .graphicsLayer {
                            this.translationX = offsetx.value
                            this.translationY = offsetY.value
                        }
                )
            }
            Button(onClick = {
                // 3. Launch animations for each TextField within the CoroutineScope
                offsetXAnimatables.forEachIndexed { index, animatable ->
                    scope.launch {
                        // Reset to initial position before animating in (optional, if you want to re-run it)
                        // animatable.snapTo(300f) // Uncomment if you want them to jump out then slide in again
                        delay(100L * index) // Staggered animation delay

                        Log.d("myapp", "animatable : ${animatable.label}")
                        Log.d("myapp", "animatable : ${animatable.value}")
                        Log.d("myapp", "offsetXAnimatables : $offsetXAnimatables")
                        if (animatable.value == offsetX) {
                            targetValue = 0f
                        } else {
                            targetValue = offsetX
                        }
                        animatable.animateTo(
                            targetValue = targetValue,
                            animationSpec = tween(durationMillis = 500)
                        )
                    }
                }
            }) {
                Text("Submit")
            }
        }

    }
}