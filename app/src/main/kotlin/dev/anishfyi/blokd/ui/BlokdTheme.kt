package dev.anishfyi.blokd.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BlokdOrange = Color(0xFFFF6B3D)

private val LightColors = lightColorScheme(
    primary = BlokdOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4D8),
    onPrimaryContainer = Color(0xFF351006),
    background = Color(0xFFF6F3EC),
    onBackground = Color(0xFF181A1E),
    surface = Color(0xFFFFFDF9),
    onSurface = Color(0xFF181A1E),
    surfaceVariant = Color(0xFFECE8DF),
    onSurfaceVariant = Color(0xFF68645D),
    outline = Color(0xFFD8D3C8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF895F),
    onPrimary = Color(0xFF3D1003),
    primaryContainer = Color(0xFF5A1D0A),
    onPrimaryContainer = Color(0xFFFFDBCD),
    background = Color(0xFF101114),
    onBackground = Color(0xFFF4F1EA),
    surface = Color(0xFF1A1C20),
    onSurface = Color(0xFFF4F1EA),
    surfaceVariant = Color(0xFF25272C),
    onSurfaceVariant = Color(0xFFBDB8AF),
    outline = Color(0xFF3B3D42),
)

private val BlokdTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.4.sp,
    ),
)

@Composable
fun BlokdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BlokdTypography,
        content = content,
    )
}
