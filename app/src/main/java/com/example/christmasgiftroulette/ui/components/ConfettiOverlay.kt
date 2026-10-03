package com.example.christmasgiftroulette.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.christmasgiftroulette.ui.theme.ChristmasColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Confetto(val x: Float, val y: Float, val speed: Int, val spin: Int, val width: Float, val color: Color, val phase: Float)

/** Lightweight falling confetti: 48 rectangles animated by a single looping float. Ignores touches. */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val pieces = remember {
        val r = Random(7)
        val palette = listOf(ChristmasColors.RedBright, ChristmasColors.Gold, ChristmasColors.GreenBright, Color.White, Color(0xFF4FC3F7))
        List(48) {
            Confetto(r.nextFloat(), r.nextFloat(), r.nextInt(1, 3), r.nextInt(1, 4), 8f + r.nextFloat() * 8f, palette[r.nextInt(palette.size)], r.nextFloat())
        }
    }
    val t = rememberInfiniteTransition(label = "confetti").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing), RepeatMode.Restart),
        label = "confettiT",
    )
    Canvas(modifier.fillMaxSize()) {
        val scale = (size.minDimension / 600f).coerceIn(0.8f, 1.6f)
        pieces.forEach { p ->
            val progress = (p.y + t.value * p.speed) % 1f
            val x = (p.x + 0.02f * sin(2f * PI.toFloat() * (t.value * p.speed + p.phase))) * size.width
            val y = progress * (size.height + 40f) - 20f
            rotate(degrees = t.value * 360f * p.spin + p.phase * 360f, pivot = Offset(x, y)) {
                drawRect(p.color, Offset(x - p.width * scale / 2f, y - p.width * scale / 4f), Size(p.width * scale, p.width * scale / 2f))
            }
        }
    }
}
