package com.example.christmasgiftfinder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import com.example.christmasgiftfinder.ui.theme.BrightRed
import com.example.christmasgiftfinder.ui.theme.Evergreen
import com.example.christmasgiftfinder.ui.theme.Gold
import com.example.christmasgiftfinder.ui.theme.LeafGreen
import com.example.christmasgiftfinder.ui.theme.Snow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Subtle snowflakes at fixed pseudo-random positions (no animation). */
@Composable
fun Snowfall(modifier: Modifier = Modifier.fillMaxSize(), color: Color = Snow, seed: Int = 7, count: Int = 26) {
    Canvas(modifier) {
        val rnd = Random(seed)
        repeat(count) { i ->
            val c = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
            val r = (2f + rnd.nextFloat() * 5f) * density
            if (i % 4 == 0) {
                drawSnowflake(c, r * 1.8f, color.copy(alpha = 0.55f))
            } else {
                drawCircle(color.copy(alpha = 0.35f + rnd.nextFloat() * 0.3f), r * 0.6f, c)
            }
        }
    }
}

private fun DrawScope.drawSnowflake(center: Offset, radius: Float, color: Color) {
    val stroke = (radius * 0.18f).coerceAtLeast(1f)
    for (k in 0 until 3) {
        val a = Math.PI * k / 3.0
        val dx = (cos(a) * radius).toFloat()
        val dy = (sin(a) * radius).toFloat()
        drawLine(color, Offset(center.x - dx, center.y - dy), Offset(center.x + dx, center.y + dy), stroke, StrokeCap.Round)
    }
}

fun DrawScope.drawStar(center: Offset, outer: Float, color: Color) {
    val path = Path()
    val inner = outer * 0.45f
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = -Math.PI / 2 + i * Math.PI / 5
        val p = Offset(center.x + (cos(a) * r).toFloat(), center.y + (sin(a) * r).toFloat())
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, color)
}

fun DrawScope.drawGiftBox(topLeft: Offset, boxSize: Float, body: Color, ribbon: Color) {
    val lid = boxSize * 0.24f
    val bodyTop = topLeft.y + lid
    val corner = CornerRadius(boxSize * 0.06f)
    // body + vertical ribbon
    drawRoundRect(body, Offset(topLeft.x + boxSize * 0.05f, bodyTop), Size(boxSize * 0.9f, boxSize - lid), corner)
    drawRect(ribbon, Offset(topLeft.x + boxSize * 0.43f, bodyTop), Size(boxSize * 0.14f, boxSize - lid))
    // lid + ribbon
    drawRoundRect(body, topLeft, Size(boxSize, lid), corner)
    drawRect(ribbon, Offset(topLeft.x + boxSize * 0.43f, topLeft.y), Size(boxSize * 0.14f, lid))
    // bow
    val bow = boxSize * 0.14f
    val bc = Offset(topLeft.x + boxSize / 2, topLeft.y)
    drawCircle(ribbon, bow, Offset(bc.x - bow * 0.9f, bc.y - bow * 0.4f))
    drawCircle(ribbon, bow, Offset(bc.x + bow * 0.9f, bc.y - bow * 0.4f))
    drawCircle(ribbon.copy(alpha = 0.85f), bow * 0.6f, bc)
}

/** Draws a friendly Christmas tree inside the (0,0)-(w,h) box of the current drawing scope. */
fun DrawScope.drawTree(w: Float, h: Float) {
    val cx = w / 2
    drawRect(Color(0xFF6D4C41), Offset(cx - w * 0.06f, h * 0.88f), Size(w * 0.12f, h * 0.12f))
    // three tiers, bottom one first so upper tiers overlap it
    listOf(
        Triple(0.34f, 1.0f, Color(0xFF388E3C)),
        Triple(0.2f, 0.82f, LeafGreen),
        Triple(0.06f, 0.62f, Evergreen),
    ).forEach { (topFrac, halfWidthFrac, color) ->
        val top = h * topFrac
        val bottom = h * (topFrac + 0.36f).coerceAtMost(0.9f)
        val half = w * halfWidthFrac / 2
        val p = Path().apply {
            moveTo(cx, top)
            lineTo(cx + half, bottom)
            lineTo(cx - half, bottom)
            close()
        }
        drawPath(p, color)
    }
    drawStar(Offset(cx, h * 0.06f), w * 0.09f, Gold)
    listOf(
        Offset(0.46f, 0.3f) to BrightRed, Offset(0.58f, 0.46f) to Gold, Offset(0.38f, 0.52f) to Snow,
        Offset(0.66f, 0.66f) to BrightRed, Offset(0.42f, 0.72f) to Gold, Offset(0.5f, 0.2f) to Snow,
        Offset(0.28f, 0.76f) to BrightRed, Offset(0.74f, 0.78f) to Snow,
    ).forEach { (f, c) -> drawCircle(c, w * 0.03f, Offset(w * f.x, h * f.y)) }
}

/** Gift boxes + tree + stars + snow: the welcome illustration. */
@Composable
fun WelcomeScene(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // snow ground
        drawRoundRect(Snow, Offset(-w * 0.1f, h * 0.88f), Size(w * 1.2f, h * 0.3f), CornerRadius(w * 0.3f))
        // tree
        val treeH = h * 0.9f
        val treeW = treeH * 0.78f
        translate(w / 2 - treeW / 2, 0f) { drawTree(treeW, treeH) }
        // gift boxes
        val ground = h * 0.92f
        val b1 = h * 0.28f
        drawGiftBox(Offset(w * 0.5f - treeW * 0.72f, ground - b1), b1, BrightRed, Gold)
        val b2 = h * 0.2f
        drawGiftBox(Offset(w * 0.5f + treeW * 0.5f, ground - b2), b2, Evergreen, Snow)
        val b3 = h * 0.16f
        drawGiftBox(Offset(w * 0.5f + treeW * 0.28f, ground - b3), b3, Gold, BrightRed)
        // little stars
        drawStar(Offset(w * 0.12f, h * 0.2f), h * 0.05f, Gold)
        drawStar(Offset(w * 0.88f, h * 0.12f), h * 0.04f, Gold)
        drawStar(Offset(w * 0.8f, h * 0.45f), h * 0.03f, Snow)
    }
}

/** Small multi-colour confetti used behind the surprise gift. */
@Composable
fun Confetti(modifier: Modifier = Modifier.fillMaxSize(), seed: Int = 3) {
    Canvas(modifier) {
        val rnd = Random(seed)
        val colors = listOf(BrightRed, Gold, LeafGreen, Color(0xFF42A5F5), Snow)
        repeat(36) {
            val c = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
            val s = (3f + rnd.nextFloat() * 4f) * density
            val color = colors[rnd.nextInt(colors.size)]
            when (it % 3) {
                0 -> drawCircle(color, s * 0.5f, c)
                1 -> drawRect(color, c, Size(s, s * 0.5f))
                else -> drawStar(c, s, color)
            }
        }
    }
}
