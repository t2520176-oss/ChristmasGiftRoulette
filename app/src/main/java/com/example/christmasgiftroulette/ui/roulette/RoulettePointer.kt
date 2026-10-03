package com.example.christmasgiftroulette.ui.roulette

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.christmasgiftroulette.ui.theme.ChristmasColors

/** Fixed gold triangle pointing DOWN at the wheel. It never rotates. */
@Composable
fun RoulettePointer(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.04f, h * 0.06f)
            lineTo(w * 0.96f, h * 0.06f)
            lineTo(w * 0.5f, h * 0.96f)
            close()
        }
        // soft drop shadow
        drawPath(
            Path().apply {
                moveTo(w * 0.08f, h * 0.12f)
                lineTo(w * 1.00f, h * 0.12f)
                lineTo(w * 0.54f, h * 1.00f)
                close()
            },
            Color.Black.copy(alpha = 0.35f),
        )
        drawPath(path, Brush.verticalGradient(listOf(ChristmasColors.GoldLight, ChristmasColors.Gold, Color(0xFFB57F12))))
        drawPath(path, Color.White.copy(alpha = 0.9f), style = Stroke(width = w * 0.05f))
        drawCircle(ChristmasColors.Red, w * 0.11f, Offset(w * 0.5f, h * 0.30f))
        drawCircle(Color.White.copy(alpha = 0.6f), w * 0.04f, Offset(w * 0.47f, h * 0.27f))
    }
}
