package com.example.moneymaplk.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = MoneyMapAmber,
    onPrimary = MoneyMapBlue,
    primaryContainer = MoneyMapSky,
    onPrimaryContainer = MoneyMapTextPrimary,
    secondary = MoneyMapBlueLight,
    onSecondary = Color.White,
    secondaryContainer = MoneyMapGreenSoft,
    onSecondaryContainer = MoneyMapBlue,
    tertiary = MoneyMapAmber,
    error = MoneyMapRed,
    onError = MoneyMapBlue,
    errorContainer = MoneyMapRedSoft,
    background = MoneyMapBackground,
    onBackground = MoneyMapTextPrimary,
    surface = MoneyMapSurface,
    onSurface = MoneyMapTextPrimary,
    surfaceVariant = MoneyMapSurfaceVariant,
    onSurfaceVariant = MoneyMapTextSecondary,
    outline = MoneyMapOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = MoneyMapAmber,
    onPrimary = MoneyMapBlue,
    primaryContainer = MoneyMapSky,
    onPrimaryContainer = MoneyMapTextPrimary,
    secondary = MoneyMapBlueLight,
    onSecondary = Color.White,
    secondaryContainer = MoneyMapGreenSoft,
    onSecondaryContainer = MoneyMapBlue,
    tertiary = MoneyMapGreen,
    onTertiary = MoneyMapBlue,
    error = MoneyMapRed,
    onError = MoneyMapBlue,
    errorContainer = MoneyMapRedSoft,
    onErrorContainer = Color(0xFFFFDAD6),
    background = MoneyMapBackground,
    onBackground = MoneyMapTextPrimary,
    surface = MoneyMapSurface,
    onSurface = MoneyMapTextPrimary,
    surfaceVariant = MoneyMapSurfaceVariant,
    onSurfaceVariant = MoneyMapTextSecondary,
    outline = MoneyMapOutline,
    inverseSurface = MoneyMapTextPrimary,
    inverseOnSurface = MoneyMapBlue,
    surfaceTint = MoneyMapBlueLight
)

@Composable
fun MoneyMapLKTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = MoneyMapTypography,
        shapes = MoneyMapShapes,
        content = content
    )
}
