package com.example.mychatroom.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mychatroom.MainActivity
import com.example.mychatroom.ui.theme.SplashScreenDecoratorTheme
import com.example.mychatroom.welcome.AnimatedWelcomeScreen

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SplashScreenDecoratorTheme {
                AnimatedWelcomeScreen(
                    onclick = {
                        // Navigate to MainActivity when user clicks
                        startActivity(Intent(this, MainActivity::class.java))
                        finish() // close WelcomeActivity so back button doesn’t return here
                    }
                )
            }
        }
    }
}