package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4599FF),
    onPrimary = Color(0xFF001D3D),
    primaryContainer = Color(0xFF103766),
    onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = PulseCoralAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4D1926),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = PulseEmeraldSuccess,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF3E4042)
)

private val LightColorScheme = lightColorScheme(
    primary = PulseBluePrimary,
    onPrimary = Color.White,
    primaryContainer = PulseBlueLight,
    onPrimaryContainer = PulseBlueDark,
    secondary = PulseCoralAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE5EC),
    onSecondaryContainer = Color(0xFF800F2F),
    tertiary = PulseEmeraldSuccess,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = Color(0xFFCED0D4)
)

val PulseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PulseSocialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PulseShapes,
        content = content
    )
}
