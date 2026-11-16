package com.example.mychatroom.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Portrait shape used when the screen is wider than tall (9:16, a typical phone). */
private const val PortraitAspect = 9f / 16f

/**
 * Wraps a game so it is always played in portrait.
 *
 * 1. Asks Android to rotate the screen to portrait while the game is open, and
 *    gives the old setting back when the player leaves the game.
 * 2. Backup for when Android ignores that request (split-screen, desktop
 *    windows, or large screens on newer Android): if the space is still wider
 *    than tall, the game is drawn in a 9:16 area in the middle, with black bars
 *    on the sides. The game only ever sees a portrait-shaped screen.
 */
@Composable
fun PortraitGame(content: @Composable () -> Unit) {
    LockPortrait()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val frame = if (maxWidth > maxHeight) {
            Modifier.fillMaxHeight().aspectRatio(PortraitAspect)   // landscape: letterbox
        } else {
            Modifier.fillMaxSize()                                 // portrait: whole screen
        }
        Box(frame) { content() }
    }
}

@Composable
private fun LockPortrait() {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity) {
        val previous = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        onDispose {
            // While the activity is only being re-created for the rotation itself,
            // keep the lock; restoring here would rotate back and forth forever.
            if (!activity.isChangingConfigurations) {
                activity.requestedOrientation = previous
            }
        }
    }
}

/** Compose's LocalContext can be a wrapper around the Activity; unwrap until we find it. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
