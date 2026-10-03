package com.example.christmasgiftroulette.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ChristmasColors {
    val Red = Color(0xFFC62828)
    val RedBright = Color(0xFFE53935)
    val Green = Color(0xFF1B6B3A)
    val GreenBright = Color(0xFF2E8B4F)
    val Gold = Color(0xFFF2B33D)
    val GoldLight = Color(0xFFFFD66B)
    val Cream = Color(0xFFFFF6E3)
    val CreamDark = Color(0xFFF3E4C4)
    val Navy = Color(0xFF0B1B3F)
    val NavyMid = Color(0xFF16306B)
    val Burgundy = Color(0xFF7B1E3A)
    val Wood = Color(0xFF8D5524)
    val WoodDark = Color(0xFF5C3317)
    val Ink = Color(0xFF2B1A12)
}

private val ColorScheme = lightColorScheme(
    primary = ChristmasColors.Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410002),
    secondary = ChristmasColors.Green,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8EBD2),
    onSecondaryContainer = Color(0xFF00210E),
    tertiary = ChristmasColors.Gold,
    onTertiary = ChristmasColors.Ink,
    background = ChristmasColors.Navy,
    onBackground = Color.White,
    surface = ChristmasColors.Cream,
    onSurface = ChristmasColors.Ink,
    surfaceVariant = ChristmasColors.CreamDark,
    onSurfaceVariant = Color(0xFF4F3B2E),
    surfaceContainerHighest = ChristmasColors.CreamDark,
    surfaceContainerHigh = ChristmasColors.CreamDark,
    surfaceContainer = ChristmasColors.Cream,
    surfaceContainerLow = ChristmasColors.Cream,
    outline = Color(0xFF8A6F5C),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val Serif = FontFamily.Serif

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

@Composable
fun ChristmasTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, typography = AppTypography, shapes = AppShapes, content = content)
}
