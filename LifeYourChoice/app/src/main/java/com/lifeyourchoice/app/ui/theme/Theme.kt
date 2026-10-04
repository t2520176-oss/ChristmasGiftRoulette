package com.lifeyourchoice.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.lifeyourchoice.core.model.Stat

/** Dark navy cinematic palette with blue highlights and gold accents. */
object Ly {
    val Navy950 = Color(0xFF040913)
    val Navy900 = Color(0xFF070E1F)
    val Navy800 = Color(0xFF0B1630)
    val Navy700 = Color(0xFF102144)
    val Navy600 = Color(0xFF17305F)
    val Panel = Color(0xFF0C1A38)
    val Blue = Color(0xFF2F7BFF)
    val BlueSoft = Color(0xFF6FA8FF)
    val BlueLine = Color(0xFF2B5BB8)
    val Gold = Color(0xFFFFC83D)
    val GoldDeep = Color(0xFFE29A00)
    val Text = Color(0xFFEAF1FF)
    val TextDim = Color(0xFF9FB2D6)
    val Good = Color(0xFF45D483)
    val Bad = Color(0xFFFF5C6C)

    fun statColor(s: Stat): Color = when (s) {
        Stat.HEALTH -> Color(0xFF3DD16F)
        Stat.KNOWLEDGE -> Color(0xFF3B8CFF)
        Stat.DISCIPLINE -> Color(0xFFF2B632)
        Stat.CONFIDENCE -> Color(0xFFFF5470)
        Stat.REPUTATION -> Color(0xFFB65CFF)
        Stat.FAMILY -> Color(0xFFFF8A3D)
        Stat.FRIENDSHIP -> Color(0xFF39B7FF)
        Stat.MONEY -> Color(0xFF7ED957)
        Stat.HAPPINESS -> Color(0xFFFF5FA8)
        Stat.ENERGY -> Color(0xFFFFD23F)
        Stat.CAREER -> Color(0xFF5CE1E6)
    }

    fun statGlyph(s: Stat): String = when (s) {
        Stat.HEALTH -> "✚"
        Stat.KNOWLEDGE -> "✦"
        Stat.DISCIPLINE -> "◆"
        Stat.CONFIDENCE -> "♥"
        Stat.REPUTATION -> "★"
        Stat.FAMILY -> "⌂"
        Stat.FRIENDSHIP -> "●"
        Stat.MONEY -> "$"
        Stat.HAPPINESS -> "☺"
        Stat.ENERGY -> "⚡"
        Stat.CAREER -> "▲"
    }
}

@Composable
fun LifeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ly.Blue,
            onPrimary = Color.White,
            secondary = Ly.Gold,
            background = Ly.Navy900,
            surface = Ly.Navy800,
            onSurface = Ly.Text,
            onBackground = Ly.Text
        ),
        content = content
    )
}
