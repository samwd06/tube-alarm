package com.example.tubealarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TubeBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF000C62),
    secondary = TubeRed,
    onSecondary = Color.White,
    error = TubeRed,
    background = Color(0xFFF6F7FB),
    surface = Color(0xFFF6F7FB),
    surfaceContainer = Color.White,
    surfaceContainerHighest = Color(0xFFE9EBF3),
)

private val DarkColorScheme = darkColorScheme(
    primary = TubeBlueLight,
    onPrimary = Color(0xFF001B9C),
    primaryContainer = Color(0xFF1B2A8A),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = TubeRedLight,
    error = TubeRedLight,
    background = Color(0xFF10131C),
    surface = Color(0xFF10131C),
    surfaceContainer = Color(0xFF1A1E2A),
    surfaceContainerHighest = Color(0xFF262B3A),
)

// Always TfL's colours, rather than ones picked from the phone's wallpaper,
// so the line colours on the route map look right next to them
@Composable
fun TubeAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
