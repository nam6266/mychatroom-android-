package com.example.mychatroom.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BrandVioletLight,
    onPrimary = Color(0xFF160B4A),
    primaryContainer = Color(0xFF3B2C99),
    onPrimaryContainer = Color(0xFFE5DEFF),
    secondary = BrandPinkLight,
    onSecondary = Color(0xFF3D0021),
    secondaryContainer = Color(0xFF6B1D45),
    onSecondaryContainer = Color(0xFFFFD9E6),
    tertiary = BrandTealLight,
    onTertiary = Color(0xFF00201E),
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    surfaceContainerLowest = NightBackground,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceVariant,
    surfaceContainerHighest = NightSurfaceHigh,
    outline = NightOutline,
    outlineVariant = NightSurfaceHigh,
)

private val LightColorScheme = lightColorScheme(
    primary = BrandViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF1E0F66),
    secondary = BrandPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E6),
    onSecondaryContainer = Color(0xFF3D0021),
    tertiary = BrandTeal,
    onTertiary = Color.White,
    background = DawnBackground,
    onBackground = DawnOnSurface,
    surface = DawnSurface,
    onSurface = DawnOnSurface,
    surfaceVariant = DawnSurfaceVariant,
    onSurfaceVariant = DawnOnSurfaceVariant,
    surfaceContainerLowest = DawnSurface,
    surfaceContainerLow = DawnBackground,
    surfaceContainer = DawnSurfaceVariant,
    surfaceContainerHigh = DawnSurfaceVariant,
    surfaceContainerHighest = DawnSurfaceHigh,
    outline = DawnOutline,
    outlineVariant = DawnSurfaceHigh,
)

const val DEFAULT_PADDING = 44

@Composable
fun MyChatRoomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the app keeps its own brand colours instead of the wallpaper's.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun SplashScreenDecoratorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MyChatRoomTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
