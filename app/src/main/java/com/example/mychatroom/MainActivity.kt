package com.example.mychatroom

import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import com.example.mychatroom.anim.HeartBeatAnimation
import com.example.mychatroom.screen.Main
import com.example.mychatroom.splash.SplashScreenDecorator
import com.example.mychatroom.splash.splash
import com.example.mychatroom.ui.theme.MyChatRoomTheme
import com.example.mychatroom.ui.theme.SplashScreenDecoratorTheme
import com.example.mychatroom.welcome.AnimatedWelcomeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {

    var splashScreen: SplashScreenDecorator? = null


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

//        showSplash()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SplashScreenDecoratorTheme {
                Main()
            }
        }

//        lifecycleScope.launch {
//            // delay os splash screen
//            delay(1.seconds)
//            splashScreen?.shouldKeepOnScreen = false
//            // delay custom splash screen
//            delay(3.seconds)
//            splashScreen?.dismiss()
//        }
//    }

//    private fun showSplash() {
//
//        val exitDuration = 800L
//        val fadeDurationOffset = 200L
//
//        splashScreen = splash {
//            content {
//                exitAnimationDuration = exitDuration
//                composeViewFadeDurationOffset = fadeDurationOffset
//                SplashScreenDecoratorTheme {
//                    HeartBeatAnimation(
//                        isVisible = isVisible.value,
//                        exitAnimationDuration = exitAnimationDuration.milliseconds,
//                        onStartExitAnimation = { startExitAnimation() }
//                    )
//                }
//            }
//        }
//    }
//
//    override fun onDestroy() {
//        splashScreen = null
//        super.onDestroy()
//    }
    }
}
