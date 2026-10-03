package com.example.christmasgiftroulette.ui.roulette

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.example.christmasgiftroulette.model.GiftItem
import com.example.christmasgiftroulette.ui.components.drawSparkle
import com.example.christmasgiftroulette.ui.theme.ChristmasColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private data class SegmentStyle(val fill: Color, val text: Color)

// Alternating Christmas palette: red, green, gold, burgundy, cream, dark blue.
private val palette = listOf(
    SegmentStyle(ChristmasColors.Red, Color.White),
    SegmentStyle(ChristmasColors.Green, Color.White),
    SegmentStyle(ChristmasColors.Gold, ChristmasColors.Ink),
    SegmentStyle(ChristmasColors.Burgundy, Color.White),
    SegmentStyle(ChristmasColors.Cream, ChristmasColors.Ink),
    SegmentStyle(ChristmasColors.NavyMid, Color.White),
)

/** Palette index for segment [i] of [n]; avoids the last segment matching the first one. */
private fun paletteIndex(i: Int, n: Int): Int {
    val base = i % palette.size
    return if (n > 1 && i == n - 1 && base == 0) 4 else base
}

private fun trimName(name: String, limit: Int): String =
    if (name.length <= limit) name else name.take(limit - 1).trimEnd() + "…"

/**
 * The roulette. Segments, borders, labels and rim lights are drawn with Canvas; the whole disc is
 * rotated on the GPU through [rotationProvider] (read inside graphicsLayer, so animation frames do
 * not recompose anything). The number of segments always equals `gifts.size`.
 */
@Composable
fun RouletteWheel(
    gifts: List<GiftItem>,
    rotationProvider: () -> Float,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val pointerHeight = size * 0.13f
    val wheelSize = size - pointerHeight * 0.55f

    val pulse = rememberInfiniteTransition(label = "wheelGlow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )

    Box(
        modifier.size(size).clearAndSetSemantics { },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // golden halo
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            drawCircle(
                Brush.radialGradient(
                    listOf(ChristmasColors.Gold.copy(alpha = 0.35f + 0.25f * pulse.value), Color.Transparent),
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }
        // rotating disc
        Canvas(
            Modifier
                .size(wheelSize)
                .padding(wheelSize * 0.02f)
                .graphicsLayer { rotationZ = rotationProvider() },
        ) {
            drawWheel(gifts, textMeasurer)
        }
        // static hub on top of the disc
        Canvas(Modifier.size(wheelSize).padding(wheelSize * 0.02f)) { drawHub() }
        // fixed pointer
        RoulettePointer(Modifier.align(Alignment.TopCenter).size(width = pointerHeight * 0.95f, height = pointerHeight))
    }
}

private fun DrawScope.drawWheel(gifts: List<GiftItem>, textMeasurer: TextMeasurer) {
    val n = gifts.size
    if (n == 0) return
    val c = center
    val outer = min(size.width, size.height) / 2f
    val rimWidth = outer * 0.07f
    val radius = outer - rimWidth // radius of the coloured segments
    val sweep = 360f / n
    val arcTopLeft = Offset(c.x - radius, c.y - radius)
    val arcSize = Size(radius * 2f, radius * 2f)

    // drop shadow + rim
    drawCircle(Color.Black.copy(alpha = 0.35f), outer, c + Offset(0f, outer * 0.02f))
    drawCircle(Brush.sweepGradient(listOf(ChristmasColors.GoldLight, ChristmasColors.Gold, Color(0xFFB57F12), ChristmasColors.GoldLight), center = c), outer, c)

    // segments
    for (i in 0 until n) {
        val style = palette[paletteIndex(i, n)]
        val start = -90f + i * sweep
        if (n == 1) {
            drawCircle(style.fill, radius, c)
        } else {
            drawArc(style.fill, start, sweep, useCenter = true, topLeft = arcTopLeft, size = arcSize)
            // subtle lighter sheen toward the rim for depth
            drawArc(
                Brush.radialGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.14f)),
                    center = c,
                    radius = radius,
                ),
                start, sweep, useCenter = true, topLeft = arcTopLeft, size = arcSize,
            )
        }
    }
    // separator lines
    if (n > 1) {
        for (i in 0 until n) {
            val a = ((-90f + i * sweep) * PI / 180.0).toFloat()
            drawLine(
                Color.White.copy(alpha = 0.75f),
                c,
                Offset(c.x + cos(a) * radius, c.y + sin(a) * radius),
                strokeWidth = (outer * 0.008f).coerceAtLeast(1.5f),
            )
        }
    }
    drawCircle(ChristmasColors.GoldLight, radius, c, style = Stroke(width = outer * 0.012f))

    // rim bulbs
    val bulbCount = 24
    for (i in 0 until bulbCount) {
        val a = (2.0 * PI * i / bulbCount).toFloat()
        val p = Offset(c.x + cos(a) * (outer - rimWidth / 2f), c.y + sin(a) * (outer - rimWidth / 2f))
        drawCircle(if (i % 2 == 0) Color.White else ChristmasColors.RedBright, rimWidth * 0.30f, p)
    }

    // labels
    val showAmount = n <= 14
    val labelRight = radius * 0.93f
    val maxLabelWidth = (radius * 0.66f).toInt().coerceAtLeast(1)
    val chord = 2f * (radius * 0.6f) * sin(Math.toRadians((sweep / 2f).toDouble().coerceAtMost(80.0))).toFloat()
    val maxFont = radius * 0.10f
    val nameFont = min(maxFont, chord * if (showAmount) 0.34f else 0.55f).coerceAtLeast(radius * 0.035f)
    val maxChars = if (n > 30) 8 else if (n > 16) 11 else 16

    for (i in 0 until n) {
        val gift = gifts[i]
        val style = palette[paletteIndex(i, n)]
        val centerAngle = -90f + (i + 0.5f) * sweep
        val nameLayout = textMeasurer.measure(
            text = trimName(gift.name, maxChars),
            style = TextStyle(color = style.text, fontSize = nameFont.toSp(), fontWeight = FontWeight.ExtraBold),
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            maxLines = 1,
            constraints = Constraints(maxWidth = maxLabelWidth),
        )
        val amountText = if (showAmount) gift.formattedAmount else null
        val amountLayout = amountText?.let {
            textMeasurer.measure(
                text = it,
                style = TextStyle(color = style.text, fontSize = (nameFont * 0.82f).toSp(), fontWeight = FontWeight.Bold),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                maxLines = 1,
                constraints = Constraints(maxWidth = maxLabelWidth),
            )
        }
        val totalHeight = nameLayout.size.height + (amountLayout?.size?.height ?: 0)
        rotate(degrees = centerAngle, pivot = c) {
            var y = c.y - totalHeight / 2f
            drawText(nameLayout, topLeft = Offset(c.x + labelRight - nameLayout.size.width, y))
            y += nameLayout.size.height
            if (amountLayout != null) {
                drawText(amountLayout, topLeft = Offset(c.x + labelRight - amountLayout.size.width, y))
            }
        }
    }
}

private fun DrawScope.drawHub() {
    val c = center
    val outer = min(size.width, size.height) / 2f
    val hub = outer * 0.17f
    drawCircle(Color.Black.copy(alpha = 0.3f), hub * 1.08f, c + Offset(0f, hub * 0.06f))
    drawCircle(Brush.radialGradient(listOf(ChristmasColors.GoldLight, ChristmasColors.Gold, Color(0xFFB57F12)), center = c, radius = hub), hub, c)
    drawCircle(Color.White.copy(alpha = 0.85f), hub, c, style = Stroke(width = hub * 0.08f))
    drawSparkle(c, hub * 0.62f, ChristmasColors.Red)
}
