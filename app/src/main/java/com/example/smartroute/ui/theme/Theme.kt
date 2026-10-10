package com.example.smartroute.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Blue900 = Color(0xFF071B4D)
val Blue800 = Color(0xFF0A2E80)
val Blue600 = Color(0xFF0B57D0)
val Blue100 = Color(0xFFE3ECFB)
val Yellow = Color(0xFFFFC928)
val Ink = Color(0xFF16233F)
val Muted = Color(0xFF6B7690)
val AppBg = Color(0xFFF4F7FD)
val Success = Color(0xFF2E9E5B)
val Warning = Color(0xFFF59E0B)
val Danger = Color(0xFFE5484D)

private val LightColors = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    secondary = Yellow,
    onSecondary = Ink,
    background = Color.White,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Blue100,
    onSurfaceVariant = Muted,
    outlineVariant = Color(0xFFDDE4F3)
)

@Composable
fun SmartRouteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content
    )
}
