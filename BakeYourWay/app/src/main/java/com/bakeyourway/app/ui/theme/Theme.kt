package com.bakeyourway.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Warm, baking-inspired palette: cream backgrounds, coral/pink accent, caramel and butter highlights.
val Cream = Color(0xFFFFF8F0)
val CreamDark = Color(0xFFFCEFE4)
val Coral = Color(0xFFD93A5E)
val CoralSoft = Color(0xFFFFE1E8)
val Caramel = Color(0xFFB8672E)
val CaramelSoft = Color(0xFFFFEBD2)
val Butter = Color(0xFFFFF1C7)
val ButterDark = Color(0xFF7A5A00)
val Sage = Color(0xFF3F7D4E)
val SageSoft = Color(0xFFE6F3E8)
val SkySoft = Color(0xFFE4F0FA)
val Cocoa = Color(0xFF3B2A22)
val CocoaMuted = Color(0xFF6F5B51)
val Outline = Color(0xFFE9D8CD)

private val BakeColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = CoralSoft,
    onPrimaryContainer = Cocoa,
    secondary = Caramel,
    onSecondary = Color.White,
    secondaryContainer = CaramelSoft,
    onSecondaryContainer = Cocoa,
    tertiary = Sage,
    onTertiary = Color.White,
    tertiaryContainer = SageSoft,
    onTertiaryContainer = Cocoa,
    background = Cream,
    onBackground = Cocoa,
    surface = Color.White,
    onSurface = Cocoa,
    surfaceVariant = CreamDark,
    onSurfaceVariant = CocoaMuted,
    outline = Outline,
    outlineVariant = Outline,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFBF7),
    surfaceContainer = CreamDark,
    surfaceContainerHigh = Color(0xFFFAEBE0),
    surfaceContainerHighest = Color(0xFFF7E6DA),
)

private val BakeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun BakeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BakeColors, shapes = BakeShapes, content = content)
}
