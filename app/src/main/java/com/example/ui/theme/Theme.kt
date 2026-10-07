package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightEcoScheme = lightColorScheme(
    primary = LightEcoGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F8EE),
    onPrimaryContainer = LightEcoGreenDark,
    secondary = LightBrandNavy,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = SeverityHigh,
    onError = Color.White
)

val DarkEcoScheme = darkColorScheme(
    primary = DarkEcoMint,
    onPrimary = Color(0xFF0D1117),
    primaryContainer = Color(0xFF1B3624),
    onPrimaryContainer = DarkEcoMint,
    secondary = DarkBrandWhite,
    onSecondary = Color(0xFF0D1117),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = SeverityHigh,
    onError = Color.White
)

@Composable
fun EcoScanTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkEcoScheme else LightEcoScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
