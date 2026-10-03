package com.example.christmasgiftroulette.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import com.example.christmasgiftroulette.ui.theme.ChristmasColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Snowflake(val x: Float, val y: Float, val radius: Float, val speed: Int, val drift: Float, val phase: Float)

private val lightColors = listOf(ChristmasColors.RedBright, ChristmasColors.Gold, ChristmasColors.GreenBright, Color(0xFF4FC3F7))

/**
 * Festive snowy-navy backdrop with falling snow and a twinkling string of lights. Everything is
 * drawn in two Canvas layers driven by infinite transitions — cheap enough for ordinary phones.
 */
@Composable
fun ChristmasBackground(
    modifier: Modifier = Modifier,
    showLights: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val flakes = remember {
        val r = Random(2024)
        List(38) {
            Snowflake(
                x = r.nextFloat(),
                y = r.nextFloat(),
                radius = 1.5f + r.nextFloat() * 3.5f,
                speed = r.nextInt(1, 4),
                drift = 0.01f + r.nextFloat() * 0.02f,
                phase = r.nextFloat(),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "background")
    val snowT = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "snow",
    )
    val twinkle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle",
    )

    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(
                    listOf(Color(0xFF07122D), ChristmasColors.Navy, ChristmasColors.NavyMid),
                ),
            )
            // soft warm glow near the bottom, like distant windows
            drawCircle(
                Brush.radialGradient(
                    listOf(ChristmasColors.Gold.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height),
                    radius = size.maxDimension * 0.6f,
                ),
                radius = size.maxDimension * 0.6f,
                center = Offset(size.width * 0.5f, size.height),
            )
            val t = snowT.value
            flakes.forEach { f ->
                val progress = (f.y + t * f.speed) % 1f
                val x = (f.x + sin(2f * PI.toFloat() * (t * f.speed + f.phase)) * f.drift) * size.width
                drawCircle(Color.White.copy(alpha = 0.55f), f.radius * (size.minDimension / 700f).coerceIn(0.8f, 1.8f), Offset(x, progress * size.height))
            }
        }
        if (showLights) {
            Canvas(Modifier.fillMaxSize()) {
                val bulbs = ((size.width / 70f).toInt()).coerceIn(8, 28)
                fun wireY(i: Int) = if (i % 2 == 0) 6f else 22f
                val wire = Path()
                for (i in 0..bulbs) {
                    val x = size.width * i / bulbs
                    if (i == 0) wire.moveTo(x, wireY(i)) else wire.lineTo(x, wireY(i))
                }
                drawPath(wire, Color(0xFF1B3A2A), style = Stroke(width = 3f))
                for (i in 0..bulbs) {
                    val x = size.width * i / bulbs
                    val y = wireY(i) + 8f
                    val color = lightColors[i % lightColors.size]
                    val alpha = 0.55f + 0.45f * sin(2f * PI.toFloat() * (twinkle.value + i / 3f))
                    drawCircle(color.copy(alpha = 0.25f * alpha), 14f, Offset(x, y))
                    drawCircle(color.copy(alpha = alpha.coerceIn(0.3f, 1f)), 6f, Offset(x, y))
                }
            }
        }
        content()
    }
}
