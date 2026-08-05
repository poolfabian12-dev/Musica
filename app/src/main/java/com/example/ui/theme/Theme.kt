package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElegantPrimary,
    onPrimary = ElegantOnPrimary,
    primaryContainer = ElegantPrimaryContainer,
    onPrimaryContainer = ElegantOnPrimaryContainer,
    secondary = ElegantSecondary,
    onSecondary = ElegantOnSecondary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextVariant,
    tertiary = ElegantTertiary,
    onTertiary = ElegantOnTertiary
)

private val LightColorScheme = lightColorScheme(
    primary = ElegantPrimaryContainer,
    onPrimary = Color.White,
    primaryContainer = ElegantOnPrimaryContainer,
    onPrimaryContainer = ElegantOnPrimary,
    secondary = ElegantSecondary,
    onSecondary = ElegantOnSecondary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextVariant,
    tertiary = ElegantTertiary,
    onTertiary = ElegantOnTertiary
)

@Composable
fun MusicaCristianaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // Force elegant dark theme by default for audio app

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
