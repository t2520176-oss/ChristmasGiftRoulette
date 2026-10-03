package com.example.christmasgiftroulette.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.christmasgiftroulette.ui.theme.ChristmasColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A four-point sparkle star. */
internal fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val inner = radius * 0.22f
    val path = Path()
    for (i in 0 until 8) {
        val angle = (PI / 4 * i - PI / 2).toFloat()
        val r = if (i % 2 == 0) radius else inner
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

/** Gift box with a pulsing golden halo and twinkling sparkles around it. */
@Composable
fun GlowingGiftBox(size: Dp = 112.dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "glow")
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(modifier.size(size * 1.8f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val radius = this.size.minDimension / 2f
            drawCircle(
                Brush.radialGradient(
                    listOf(ChristmasColors.GoldLight.copy(alpha = 0.55f + 0.3f * pulse.value), Color.Transparent),
                    center = c,
                    radius = radius * (0.85f + 0.15f * pulse.value),
                ),
                radius = radius,
                center = c,
            )
            for (i in 0 until 6) {
                val angle = (2 * PI / 6 * i + 0.4).toFloat()
                val dist = radius * (0.72f + 0.1f * ((i % 2)))
                val phase = if (i % 2 == 0) pulse.value else 1f - pulse.value
                drawSparkle(
                    Offset(c.x + cos(angle) * dist, c.y + sin(angle) * dist),
                    radius * (0.07f + 0.07f * phase),
                    Color.White.copy(alpha = 0.5f + 0.5f * phase),
                )
            }
        }
        GiftIcon(Modifier.size(size))
    }
}
