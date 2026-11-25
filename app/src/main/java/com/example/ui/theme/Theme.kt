package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class RemainderrPalette(
    val isDark: Boolean,
    val background: Color,
    val diaryBackground: Color,
    val card: Color,
    val secondarySurface: Color,
    val title: Color,
    val text: Color,
    val subtext: Color,
    val border: Color,
    val icon: Color,
    val accent: Color,
    val accentLight: Color,
    val taskBlue: Color,
    val taskRed: Color,
    val taskGreen: Color,
    val delete: Color,
    val success: Color,
)

val LightPalette = RemainderrPalette(
    isDark = false,
    background = Color(0xFFF8F9FA),
    diaryBackground = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    secondarySurface = Color(0xFFF4F4F5),
    title = Color(0xFF1A1A1A),
    text = Color(0xFF404040),
    subtext = Color(0xFF8E8E93),
    border = Color(0xFFE5E5EA),
    icon = Color(0xFFA1A1A1),
    accent = Color(0xFF007AFF),
    accentLight = Color(0xFF80B1E2),
    taskBlue = Color(0xFF3A86FF),
    taskRed = Color(0xFFFF4D4D),
    taskGreen = Color(0xFF2ECC71),
    delete = Color(0xFFFF3B30),
    success = Color(0xFF34C759),
)

val DarkPalette = RemainderrPalette(
    isDark = true,
    background = Color(0xFF000000),
    diaryBackground = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    secondarySurface = Color(0xFF2C2C2E),
    title = Color(0xFFFFFFFF),
    text = Color(0xFFD2D2E2),
    subtext = Color(0xFF8E8E93),
    border = Color(0xFF38383A),
    icon = Color(0xFF636366),
    accent = Color(0xFF0A84FF),
    accentLight = Color(0xFF20364C),
    taskBlue = Color(0xFF3A86FF),
    taskRed = Color(0xFFFF4D4D),
    taskGreen = Color(0xFF2ECC71),
    delete = Color(0xFFFF453A),
    success = Color(0xFF32D74B),
)

val LocalRemainderrPalette = staticCompositionLocalOf { LightPalette }

object RemainderrTheme {
    val palette: RemainderrPalette
        @Composable get() = LocalRemainderrPalette.current
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    secondary = Color(0xFF3A86FF),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFFE5E5EA),
    error = Color(0xFFFF3B30),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    secondary = Color(0xFF3A86FF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF38383A),
    error = Color(0xFFFF453A),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun RemainderrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalRemainderrPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
