package com.example.mychatroom.servies

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat

class NotificationPermissionManager(private val context: Context) {

    private var hasBeenRequestedThisSession = false

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissionIfRequired(
        launcher: ManagedActivityResultLauncher<String, Boolean>
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (!hasNotificationPermission() && !hasBeenRequestedThisSession) {
            hasBeenRequestedThisSession = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}