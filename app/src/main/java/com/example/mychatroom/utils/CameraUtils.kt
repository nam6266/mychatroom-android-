package com.example.mychatroom.utils

import android.content.Context

class CameraUtils(val context: Context) {
    fun hasLocationPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}