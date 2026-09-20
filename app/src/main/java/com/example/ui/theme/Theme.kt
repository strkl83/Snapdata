package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SnapYellow,
    onPrimary = Color.Black,
    primaryContainer = SnapYellowContainer,
    onPrimaryContainer = SnapYellowOnContainer,
    secondary = SnapCyan,
    onSecondary = Color.Black,
    tertiary = SnapPurple,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted,
    error = AccentError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD6D300),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFFF9A6),
    onPrimaryContainer = Color(0xFF242300),
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    tertiary = Color(0xFF8E24AA),
    onTertiary = Color.White,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1A1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1E),
    surfaceVariant = Color(0xFFEEF0F4),
    onSurfaceVariant = Color(0xFF5A5A66),
    outline = Color(0xFF8E8E9A),
    error = AccentError,
    onError = Color.White
)

@Composable
fun SnapArchiveTheme(
    darkTheme: Boolean = true, // Default to sleek dark theme for Snapchat media focus
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
