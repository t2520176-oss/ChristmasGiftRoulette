package com.example.christmasgiftfinder.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val ChristmasRed = Color(0xFFC62828)
val DeepRed = Color(0xFF8E1B1B)
val BrightRed = Color(0xFFE53935)
val Evergreen = Color(0xFF1B5E20)
val LeafGreen = Color(0xFF2E7D32)
val Gold = Color(0xFFF2B632)
val Snow = Color(0xFFFFFFFF)
val Cream = Color(0xFFFFF8F4)
val Ink = Color(0xFF2B2224)
val InkSoft = Color(0xFF6D6264)

private val ChristmasColors = lightColorScheme(
    primary = ChristmasRed,
    onPrimary = Snow,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = DeepRed,
    secondary = Evergreen,
    onSecondary = Snow,
    tertiary = Gold,
    onTertiary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Snow,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF6EDEA),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFD9CBC7),
    error = Color(0xFFB3261E),
)

private val ChristmasShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

/** The app is always light and festive (the design is a red/green/gold/white Christmas palette). */
@Composable
fun ChristmasTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ChristmasColors, shapes = ChristmasShapes, content = content)
}
