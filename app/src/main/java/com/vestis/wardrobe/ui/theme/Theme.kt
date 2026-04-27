package com.vestis.wardrobe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand amber
val Amber700 = Color(0xFFBA7517)
val Amber400 = Color(0xFFEF9F27)
val AmberContainer = Color(0xFFFAEEDA)
val AmberContainerDark = Color(0xFF2A1800)

private val LightColorScheme = lightColorScheme(
    primary = Amber700,
    onPrimary = Color.White,
    primaryContainer = AmberContainer,
    onPrimaryContainer = Color(0xFF3E1F00),
    secondary = Color(0xFF6D5E40),
    onSecondary = Color.White,
    background = Color(0xFFFFFBF6),
    surface = Color(0xFFFFFBF6),
    surfaceVariant = Color(0xFFF5EFEA),
    outline = Color(0xFFD4C4B0),
    error = Color(0xFFBA1A1A)
)

private val DarkColorScheme = darkColorScheme(
    primary = Amber400,
    onPrimary = Color(0xFF3E1F00),
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFFD9C4A0),
    onSecondary = Color(0xFF3A2E18),
    background = Color(0xFF1A1410),
    surface = Color(0xFF1A1410),
    surfaceVariant = Color(0xFF251E18),
    outline = Color(0xFF4A3F33),
    error = Color(0xFFFFB4AB)
)

@Composable
fun VestisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
