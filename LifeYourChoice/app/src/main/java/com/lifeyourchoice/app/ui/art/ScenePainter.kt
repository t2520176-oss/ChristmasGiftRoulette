package com.lifeyourchoice.app.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.lifeyourchoice.core.model.SceneArt
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Original, code-drawn scene illustrations (no external assets, so nothing to license).
 * Each scene is painted from fractions of the available size, so it adapts to phones and tablets.
 * [phase] loops 0..1 and drives subtle animation (twinkling windows, steam, dust, sparks).
 */
@Composable
fun SceneCanvas(scene: SceneArt, modifier: Modifier = Modifier, animate: Boolean = true) {
    val phaseState: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "scene").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
            label = "phase"
        )
    } else {
        object : State<Float> { override val value: Float = 0.25f }
    }
    Canvas(modifier) {
        paintScene(scene, phaseState.value)
    }
}

fun DrawScope.paintScene(scene: SceneArt, phase: Float) {
    when (scene) {
        SceneArt.CLASSROOM -> classroom(phase)
        SceneArt.HALLWAY -> hallway(phase)
        SceneArt.BASKETBALL_COURT -> court(phase)
        SceneArt.LIVING_ROOM -> livingRoom(phase)
        SceneArt.NIGHT_CITY -> nightCity(phase)
        SceneArt.APARTMENT -> apartment(phase)
        SceneArt.OFFICE -> office(phase)
        SceneArt.WORKSHOP -> workshop(phase)
        SceneArt.HOSPITAL -> hospital(phase)
        SceneArt.COFFEE_SHOP -> coffeeShop(phase)
        SceneArt.MEETING -> meeting(phase)
        SceneArt.AIRPORT -> airport(phase)
        SceneArt.WEDDING -> wedding(phase)
        SceneArt.NEW_HOME -> newHome(phase)
        SceneArt.SMALL_BUSINESS -> smallBusiness(phase)
        SceneArt.SKYLINE -> skylineSunrise(phase)
        SceneArt.CAMPUS -> campus(phase)
        SceneArt.PARK -> park(phase)
        SceneArt.STREET -> street(phase)
        SceneArt.KITCHEN -> kitchen(phase)
        SceneArt.BEDROOM -> bedroom(phase)
        SceneArt.SCHOOL_YARD -> schoolYard(phase)
        SceneArt.RESTAURANT -> restaurant(phase)
        SceneArt.INTERVIEW_ROOM -> interviewRoom(phase)
        SceneArt.FACTORY -> factory(phase)
    }
}

/** Light of the moment: the same place feels different at dawn, in the evening, or at night. */
fun DrawScope.paintTimeOfDay(time: com.lifeyourchoice.core.cinema.TimeOfDay) {
    when (time) {
        com.lifeyourchoice.core.cinema.TimeOfDay.DAY -> {}
        com.lifeyourchoice.core.cinema.TimeOfDay.MORNING -> drawRect(Brush.verticalGradient(listOf(Color(0x33FFE2A8), Color(0x11FFE2A8))))
        com.lifeyourchoice.core.cinema.TimeOfDay.EVENING -> drawRect(Brush.verticalGradient(listOf(Color(0x44FF8A3D), Color(0x22C05A8A))))
        com.lifeyourchoice.core.cinema.TimeOfDay.NIGHT -> drawRect(Brush.verticalGradient(listOf(Color(0x880A1440), Color(0x990A1030))))
    }
}

// ---------------------------------------------------------------- helpers

private const val TWO_PI = (2 * PI).toFloat()

private fun tw(i: Int, phase: Float, speed: Float = 1f): Float =
    0.5f + 0.5f * sin((phase * speed + i * 0.137f) * TWO_PI)

private fun DrawScope.fillV(a: Color, b: Color) {
    drawRect(Brush.verticalGradient(listOf(a, b)))
}

private fun DrawScope.fillV(a: Color, b: Color, c: Color) {
    drawRect(Brush.verticalGradient(listOf(a, b, c)))
}

private fun DrawScope.box(x: Float, y: Float, w: Float, h: Float, c: Color, alpha: Float = 1f) {
    drawRect(c, Offset(x * size.width, y * size.height), Size(w * size.width, h * size.height), alpha)
}

private fun DrawScope.boxV(x: Float, y: Float, w: Float, h: Float, top: Color, bottom: Color) {
    val ox = x * size.width
    val oy = y * size.height
    val sh = h * size.height
    drawRect(Brush.verticalGradient(listOf(top, bottom), startY = oy, endY = oy + sh), Offset(ox, oy), Size(w * size.width, sh))
}

private fun DrawScope.round(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color, alpha: Float = 1f) {
    drawRoundRect(c, Offset(x * size.width, y * size.height), Size(w * size.width, h * size.height), CornerRadius(r), alpha = alpha)
}

private fun DrawScope.glow(cx: Float, cy: Float, radius: Float, c: Color) {
    val center = Offset(cx * size.width, cy * size.height)
    val r = radius * min(size.width, size.height)
    drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), center, r), r, center)
}

private fun DrawScope.dot(cx: Float, cy: Float, r: Float, c: Color, alpha: Float = 1f) {
    drawCircle(c, r * min(size.width, size.height), Offset(cx * size.width, cy * size.height), alpha)
}

private fun DrawScope.poly(c: Color, alpha: Float = 1f, vararg pts: Pair<Float, Float>) {
    val p = Path()
    pts.forEachIndexed { i, (x, y) ->
        if (i == 0) p.moveTo(x * size.width, y * size.height) else p.lineTo(x * size.width, y * size.height)
    }
    p.close()
    drawPath(p, c, alpha)
}

private fun DrawScope.polyV(top: Color, bottom: Color, y0: Float, y1: Float, vararg pts: Pair<Float, Float>) {
    val p = Path()
    pts.forEachIndexed { i, (x, y) ->
        if (i == 0) p.moveTo(x * size.width, y * size.height) else p.lineTo(x * size.width, y * size.height)
    }
    p.close()
    drawPath(p, Brush.verticalGradient(listOf(top, bottom), startY = y0 * size.height, endY = y1 * size.height))
}

private fun DrawScope.line(x0: Float, y0: Float, x1: Float, y1: Float, c: Color, width: Float = 2f, alpha: Float = 1f) {
    drawLine(c, Offset(x0 * size.width, y0 * size.height), Offset(x1 * size.width, y1 * size.height), width, alpha = alpha)
}

private fun DrawScope.stars(count: Int, phase: Float, maxY: Float = 0.5f, seed: Int = 7) {
    val rnd = Random(seed)
    for (i in 0 until count) {
        val x = rnd.nextFloat()
        val y = rnd.nextFloat() * maxY
        val r = 0.0016f + rnd.nextFloat() * 0.0028f
        dot(x, y, r, Color.White, 0.25f + 0.7f * tw(i, phase, 1f))
    }
}

/** A row of buildings with lit windows. Windows flicker gently with [phase]. */
private fun DrawScope.buildings(
    seed: Int, baseY: Float, minH: Float, maxH: Float, body: Color, windows: Color,
    phase: Float, lit: Float = 0.42f, minW: Float = 0.05f, maxW: Float = 0.11f
) {
    val rnd = Random(seed)
    val w = size.width
    val h = size.height
    var x = -0.02f * w
    var n = 0
    while (x < w) {
        val bw = (minW + rnd.nextFloat() * (maxW - minW)) * w
        val bh = (minH + rnd.nextFloat() * (maxH - minH)) * h
        val top = baseY * h - bh
        drawRect(body, Offset(x, top), Size(bw, bh + 3f))
        val cols = max(2, (bw / (0.017f * w)).toInt())
        val rows = max(1, (bh / (0.026f * h)).toInt())
        val cw = bw / (cols + 1)
        val ch = bh / (rows + 1)
        for (r in 0 until rows) for (c in 0 until cols) {
            if (rnd.nextFloat() < lit) {
                val a = if ((r * 7 + c * 3 + n) % 5 == 0) 0.55f + 0.45f * tw(r + c + n, phase, 2f) else 0.9f
                drawRect(windows, Offset(x + cw * (c + 0.55f), top + ch * (r + 0.45f)), Size(cw * 0.45f, ch * 0.5f), a)
            }
        }
        x += bw + 0.004f * w
        n++
    }
}

private fun DrawScope.treeBlob(cx: Float, baseY: Float, r: Float, trunk: Color, leaves: Color, leaves2: Color) {
    box(cx - r * 0.07f, baseY - r * 0.9f, r * 0.14f, r * 0.9f, trunk)
    dot(cx, baseY - r * 1.15f, r * 0.62f, leaves)
    dot(cx - r * 0.38f, baseY - r * 0.95f, r * 0.45f, leaves2)
    dot(cx + r * 0.4f, baseY - r * 0.98f, r * 0.45f, leaves2)
}

private fun DrawScope.vignette(strength: Float = 0.55f) {
    drawRect(Brush.radialGradient(
        listOf(Color.Transparent, Color(0xFF020510).copy(alpha = strength)),
        center = Offset(size.width / 2, size.height * 0.45f), radius = max(size.width, size.height) * 0.75f
    ))
}

// ---------------------------------------------------------------- scenes

private fun DrawScope.classroom(phase: Float) {
    fillV(Color(0xFF56698F), Color(0xFFB7A58C))
    // blackboard
    box(0.05f, 0.10f, 0.46f, 0.30f, Color(0xFF5A3B22))
    box(0.065f, 0.12f, 0.43f, 0.26f, Color(0xFF1E3A33))
    line(0.1f, 0.2f, 0.34f, 0.2f, Color.White, 3f, 0.55f)
    line(0.1f, 0.26f, 0.42f, 0.26f, Color.White, 3f, 0.45f)
    line(0.1f, 0.32f, 0.28f, 0.32f, Color.White, 3f, 0.4f)
    // windows with sunlight
    for (i in 0 until 3) {
        val x = 0.58f + i * 0.14f
        boxV(x, 0.08f, 0.11f, 0.42f, Color(0xFF9CCBFF), Color(0xFFEAF5FF))
        box(x + 0.052f, 0.08f, 0.006f, 0.42f, Color(0xFF39455F))
        box(x, 0.28f, 0.11f, 0.006f, Color(0xFF39455F))
        poly(Color.White, 0.10f + 0.04f * tw(i, phase),
            x to 0.5f, (x + 0.11f) to 0.5f, (x - 0.30f) to 0.98f, (x - 0.46f) to 0.98f)
    }
    // floor
    boxV(0f, 0.66f, 1f, 0.34f, Color(0xFF6A5039), Color(0xFF2E2218))
    // desks (silhouettes)
    for (row in 0 until 3) {
        val y = 0.66f + row * 0.1f
        val s = 1f + row * 0.18f
        for (i in 0 until 4) {
            val x = 0.04f + i * 0.25f - row * 0.01f
            round(x, y, 0.20f * s, 0.035f * s, 6f, Color(0xFF1A2745))
            box(x + 0.02f * s, y + 0.035f * s, 0.012f * s, 0.07f * s, Color(0xFF111A30))
            box(x + 0.16f * s, y + 0.035f * s, 0.012f * s, 0.07f * s, Color(0xFF111A30))
        }
    }
    // dust in the light
    for (i in 0 until 16) {
        val rnd = Random(i + 3)
        val x = 0.45f + rnd.nextFloat() * 0.5f
        val y = ((rnd.nextFloat() + phase * (0.2f + rnd.nextFloat() * 0.3f)) % 1f) * 0.8f + 0.1f
        dot(x, y, 0.003f, Color.White, 0.25f + 0.4f * tw(i, phase, 2f))
    }
    vignette(0.35f)
}

private fun DrawScope.hallway(phase: Float) {
    val vx = 0.5f; val vy = 0.44f
    val bw = 0.16f; val bh = 0.2f // back wall half-size
    fillV(Color(0xFF1B2748), Color(0xFF2A3A63))
    // ceiling, floor, walls
    poly(Color(0xFF223055), 1f, 0f to 0f, 1f to 0f, (vx + bw) to (vy - bh), (vx - bw) to (vy - bh))
    polyV(Color(0xFF445A8C), Color(0xFF1B2543), vy, 1f, (vx - bw) to (vy + bh), (vx + bw) to (vy + bh), 1f to 1f, 0f to 1f)
    box(vx - bw, vy - bh, bw * 2, bh * 2, Color(0xFF8AA6D6))
    // lockers on both walls
    val n = 9
    for (i in 0 until n) {
        val t0 = i / n.toFloat(); val t1 = (i + 1) / n.toFloat()
        fun lx(t: Float, left: Boolean) = if (left) 0f + (vx - bw) * t else 1f - (1f - (vx + bw)) * t
        fun ytop(t: Float) = 0f + (vy - bh) * t
        fun ybot(t: Float) = 1f - (1f - (vy + bh)) * t
        val c = if (i % 2 == 0) Color(0xFF2F5FAE) else Color(0xFF2A4F94)
        poly(c, 1f, lx(t0, true) to ytop(t0), lx(t1, true) to ytop(t1), lx(t1, true) to ybot(t1), lx(t0, true) to ybot(t0))
        poly(c, 1f, lx(t0, false) to ytop(t0), lx(t1, false) to ytop(t1), lx(t1, false) to ybot(t1), lx(t0, false) to ybot(t0))
        // locker slits
        val mx0 = lx((t0 + t1) / 2, true); val mx1 = lx((t0 + t1) / 2, false)
        line(mx0, ytop((t0 + t1) / 2) + 0.03f, mx0, ybot((t0 + t1) / 2) - 0.03f, Color(0xFF0F1B38), 2f, 0.7f)
        line(mx1, ytop((t0 + t1) / 2) + 0.03f, mx1, ybot((t0 + t1) / 2) - 0.03f, Color(0xFF0F1B38), 2f, 0.7f)
    }
    // ceiling lights
    for (i in 0 until 5) {
        val t = (i + 0.5f) / 5f
        val y = 0f + (vy - bh) * t * 0.9f + 0.01f
        val half = (0.09f * (1f - t * 0.75f))
        box(vx - half, y, half * 2, 0.012f * (1f - t * 0.6f) + 0.004f, Color.White, 0.75f + 0.2f * tw(i, phase))
        glow(vx, y + 0.02f, 0.18f * (1f - t * 0.6f), Color(0x55BFD8FF))
    }
    glow(vx, vy, 0.28f, Color(0x66FFFFFF))
    vignette(0.5f)
}

private fun DrawScope.court(phase: Float) {
    fillV(Color(0xFF1A2A5E), Color(0xFF7A4A8E), Color(0xFFFF8A4C))
    glow(0.3f, 0.55f, 0.55f, Color(0xAAFFC27A))
    dot(0.3f, 0.52f, 0.07f, Color(0xFFFFE2A8))
    buildings(21, 0.6f, 0.06f, 0.22f, Color(0xFF14183A), Color(0xFFFFC27A), phase, 0.3f)
    // fence
    for (i in 0..24) line(i / 24f, 0.56f, i / 24f, 0.64f, Color(0xFF0C1230), 2f, 0.6f)
    line(0f, 0.575f, 1f, 0.575f, Color(0xFF0C1230), 2f, 0.6f)
    // floor
    boxV(0f, 0.62f, 1f, 0.38f, Color(0xFFC07C3E), Color(0xFF7A4520))
    line(0.05f, 0.72f, 0.95f, 0.72f, Color.White, 3f, 0.55f)
    drawArc(Color.White.copy(alpha = 0.5f), 200f, 140f, false,
        Offset(size.width * 0.28f, size.height * 0.72f), Size(size.width * 0.44f, size.height * 0.34f), style = Stroke(3f))
    // hoop
    box(0.80f, 0.18f, 0.012f, 0.50f, Color(0xFF1B2447))
    box(0.70f, 0.16f, 0.14f, 0.12f, Color(0xFFEFF3FF), 0.95f)
    box(0.735f, 0.215f, 0.07f, 0.04f, Color(0xFFD9442E), 0.0f)
    drawArc(Color(0xFFFF6A2E), 0f, 360f, false, Offset(size.width * 0.72f, size.height * 0.27f), Size(size.width * 0.09f, size.height * 0.025f), style = Stroke(5f))
    for (i in 0..4) line(0.725f + i * 0.019f, 0.282f, 0.74f + i * 0.012f, 0.35f, Color.White, 1.5f, 0.6f)
    // ball
    dot(0.42f, 0.86f, 0.035f, Color(0xFFE5732B))
    line(0.385f, 0.86f, 0.455f, 0.86f, Color(0xFF3A1A0A), 1.5f)
    vignette(0.35f)
}

private fun DrawScope.livingRoom(phase: Float) {
    fillV(Color(0xFF1A2038), Color(0xFF2B2438))
    // window with the night city
    box(0.58f, 0.10f, 0.34f, 0.42f, Color(0xFF3B2C20))
    boxV(0.595f, 0.12f, 0.31f, 0.38f, Color(0xFF0B1642), Color(0xFF28438A))
    buildings(8, 0.5f, 0.05f, 0.18f, Color(0xFF0A1236), Color(0xFFFFD27A), phase, 0.5f, 0.04f, 0.08f)
    box(0.745f, 0.12f, 0.008f, 0.38f, Color(0xFF3B2C20))
    // curtains
    poly(Color(0xFF5B2E52), 0.9f, 0.55f to 0.08f, 0.62f to 0.08f, 0.60f to 0.55f, 0.55f to 0.55f)
    poly(Color(0xFF5B2E52), 0.9f, 0.95f to 0.08f, 0.88f to 0.08f, 0.90f to 0.55f, 0.95f to 0.55f)
    // frames
    round(0.07f, 0.16f, 0.12f, 0.16f, 6f, Color(0xFF4B3A2C)); box(0.085f, 0.18f, 0.09f, 0.12f, Color(0xFF3C5A9A))
    round(0.23f, 0.2f, 0.1f, 0.12f, 6f, Color(0xFF4B3A2C)); box(0.243f, 0.22f, 0.074f, 0.08f, Color(0xFFB77A4C))
    // floor and rug
    boxV(0f, 0.7f, 1f, 0.3f, Color(0xFF2A2030), Color(0xFF120F1C))
    round(0.15f, 0.84f, 0.7f, 0.10f, 40f, Color(0xFF3B4C84), 0.6f)
    // sofa
    round(0.10f, 0.52f, 0.62f, 0.2f, 40f, Color(0xFF3F3466))
    round(0.06f, 0.62f, 0.7f, 0.17f, 30f, Color(0xFF50427F))
    round(0.04f, 0.58f, 0.12f, 0.22f, 30f, Color(0xFF3A2F5E))
    round(0.70f, 0.58f, 0.12f, 0.22f, 30f, Color(0xFF3A2F5E))
    round(0.2f, 0.60f, 0.12f, 0.08f, 14f, Color(0xFFE0A54A)); round(0.5f, 0.60f, 0.12f, 0.08f, 14f, Color(0xFF4EC2C9))
    // lamp with warm glow
    glow(0.88f, 0.55f, 0.5f, Color(0x66FFC46B).copy(alpha = 0.35f + 0.08f * tw(0, phase)))
    box(0.875f, 0.52f, 0.008f, 0.34f, Color(0xFF14101C))
    poly(Color(0xFFFFD58A), 1f, 0.84f to 0.52f, 0.92f to 0.52f, 0.90f to 0.44f, 0.86f to 0.44f)
    vignette(0.5f)
}

private fun DrawScope.nightCity(phase: Float) {
    fillV(Color(0xFF040A24), Color(0xFF16276B), Color(0xFF7A4A94))
    stars(90, phase, 0.45f)
    glow(0.76f, 0.2f, 0.28f, Color(0x55BFD8FF))
    dot(0.76f, 0.2f, 0.055f, Color(0xFFEAF2FF))
    dot(0.78f, 0.19f, 0.05f, Color(0xFF16276B), 0.55f)
    buildings(31, 0.78f, 0.12f, 0.45f, Color(0xFF0D173F), Color(0xFF8FB4FF), phase, 0.3f, 0.04f, 0.09f)
    buildings(32, 0.86f, 0.10f, 0.38f, Color(0xFF0A1233), Color(0xFFFFD27A), phase, 0.38f, 0.05f, 0.10f)
    buildings(33, 0.96f, 0.06f, 0.26f, Color(0xFF060B22), Color(0xFFFFE2A8), phase, 0.45f, 0.06f, 0.12f)
    boxV(0f, 0.93f, 1f, 0.07f, Color(0xFF0A1030), Color(0xFF05081A))
    for (i in 0 until 5) {
        val x = 0.1f + i * 0.2f
        box(x, 0.84f, 0.004f, 0.1f, Color(0xFF03050F))
        glow(x, 0.84f, 0.12f, Color(0x88FFD27A))
    }
    vignette(0.35f)
}

private fun DrawScope.apartment(phase: Float) {
    fillV(Color(0xFF111A33), Color(0xFF1D2749))
    // big window with city
    box(0.08f, 0.10f, 0.50f, 0.46f, Color(0xFF2A3560))
    boxV(0.095f, 0.12f, 0.47f, 0.42f, Color(0xFF0B1642), Color(0xFF2B4C9E))
    buildings(5, 0.54f, 0.08f, 0.26f, Color(0xFF091033), Color(0xFFFFD27A), phase, 0.5f, 0.04f, 0.09f)
    box(0.32f, 0.12f, 0.008f, 0.42f, Color(0xFF2A3560))
    // poster
    box(0.68f, 0.14f, 0.18f, 0.26f, Color(0xFF304B8C)); box(0.70f, 0.17f, 0.14f, 0.08f, Color(0xFFFFC83D), 0.8f)
    // floor
    boxV(0f, 0.72f, 1f, 0.28f, Color(0xFF1B2140), Color(0xFF0C1022))
    // desk + laptop glow
    round(0.52f, 0.62f, 0.44f, 0.04f, 4f, Color(0xFF3A2D26))
    box(0.55f, 0.66f, 0.012f, 0.22f, Color(0xFF2A201B)); box(0.92f, 0.66f, 0.012f, 0.22f, Color(0xFF2A201B))
    glow(0.72f, 0.56f, 0.32f, Color(0x663FB6FF))
    poly(Color(0xFF9AD7FF), 1f, 0.64f to 0.5f, 0.80f to 0.5f, 0.82f to 0.62f, 0.62f to 0.62f)
    box(0.60f, 0.62f, 0.24f, 0.01f, Color(0xFF1B2447))
    // bed
    round(0.04f, 0.66f, 0.42f, 0.1f, 16f, Color(0xFF2F4E8F))
    round(0.04f, 0.62f, 0.12f, 0.08f, 20f, Color(0xFFE8EEFF), 0.85f)
    box(0.04f, 0.76f, 0.42f, 0.06f, Color(0xFF1A2347))
    // lamp
    glow(0.95f, 0.45f, 0.35f, Color(0x55FFC46B).copy(alpha = 0.3f + 0.1f * tw(1, phase)))
    dot(0.95f, 0.45f, 0.012f, Color(0xFFFFE2A8))
    vignette(0.5f)
}

private fun DrawScope.office(phase: Float) {
    fillV(Color(0xFF2A3A63), Color(0xFF1B2646))
    // glass wall: daytime skyline
    boxV(0.04f, 0.06f, 0.92f, 0.48f, Color(0xFF7FB6FF), Color(0xFFDCEBFF))
    dot(0.82f, 0.18f, 0.05f, Color(0xFFFFF3C4)); glow(0.82f, 0.18f, 0.2f, Color(0x55FFF3C4))
    buildings(41, 0.54f, 0.08f, 0.30f, Color(0xFF5876AC), Color(0xFFDDEBFF), phase, 0.25f, 0.04f, 0.09f)
    buildings(42, 0.54f, 0.04f, 0.18f, Color(0xFF3E5A8F), Color(0xFFBFD8FF), phase, 0.3f, 0.05f, 0.1f)
    for (i in 1..4) box(0.04f + i * 0.184f, 0.06f, 0.006f, 0.48f, Color(0xFF1B2646))
    // ceiling light panels
    for (i in 0 until 3) { box(0.12f + i * 0.3f, 0.01f, 0.2f, 0.02f, Color.White, 0.8f) }
    // floor
    boxV(0f, 0.62f, 1f, 0.38f, Color(0xFF1E2A4A), Color(0xFF0D1329))
    // desks with monitors
    for (i in 0 until 3) {
        val x = 0.04f + i * 0.32f
        round(x, 0.66f, 0.28f, 0.035f, 4f, Color(0xFF9FB2D6))
        box(x + 0.02f, 0.70f, 0.012f, 0.14f, Color(0xFF0F1632)); box(x + 0.25f, 0.70f, 0.012f, 0.14f, Color(0xFF0F1632))
        box(x + 0.06f, 0.52f, 0.16f, 0.11f, Color(0xFF0B122A))
        box(x + 0.07f, 0.53f, 0.14f, 0.09f, Color(0xFF8CC4FF), 0.55f + 0.3f * tw(i, phase))
        box(x + 0.13f, 0.63f, 0.02f, 0.03f, Color(0xFF0B122A))
    }
    // plant
    dot(0.955f, 0.62f, 0.03f, Color(0xFF2E8B57)); dot(0.935f, 0.65f, 0.025f, Color(0xFF3AA66A))
    box(0.945f, 0.68f, 0.022f, 0.06f, Color(0xFF4B3A2C))
    vignette(0.4f)
}

private fun DrawScope.workshop(phase: Float) {
    fillV(Color(0xFF17130F), Color(0xFF2B2219))
    // pegboard with tools
    box(0.05f, 0.1f, 0.6f, 0.36f, Color(0xFF3A2E22))
    for (r in 0 until 4) for (c in 0 until 11) dot(0.08f + c * 0.052f, 0.14f + r * 0.085f, 0.004f, Color(0xFF17130F), 0.8f)
    // hammer, wrench, saw (simple shapes)
    box(0.12f, 0.16f, 0.015f, 0.2f, Color(0xFF8A5A2B)); box(0.09f, 0.15f, 0.07f, 0.04f, Color(0xFF9AA4B8))
    box(0.28f, 0.17f, 0.012f, 0.2f, Color(0xFFB7C0D4)); dot(0.286f, 0.17f, 0.022f, Color(0xFFB7C0D4)); dot(0.286f, 0.17f, 0.011f, Color(0xFF3A2E22))
    poly(Color(0xFFC7CEDD), 1f, 0.42f to 0.2f, 0.58f to 0.2f, 0.58f to 0.3f, 0.42f to 0.26f)
    box(0.40f, 0.18f, 0.03f, 0.1f, Color(0xFF8A5A2B))
    // workbench
    boxV(0.0f, 0.64f, 1f, 0.07f, Color(0xFF7A5230), Color(0xFF4A301B))
    box(0.04f, 0.71f, 0.04f, 0.29f, Color(0xFF2A1D12)); box(0.92f, 0.71f, 0.04f, 0.29f, Color(0xFF2A1D12))
    // machine / anvil
    round(0.62f, 0.5f, 0.28f, 0.14f, 8f, Color(0xFF2D3B5A)); box(0.66f, 0.42f, 0.1f, 0.08f, Color(0xFF3C4E78))
    // hanging lamp
    line(0.35f, 0f, 0.35f, 0.28f, Color(0xFF0B0907), 3f)
    poly(Color(0xFF2A2018), 1f, 0.30f to 0.34f, 0.40f to 0.34f, 0.37f to 0.28f, 0.33f to 0.28f)
    glow(0.35f, 0.45f, 0.55f, Color(0x66FFB45A).copy(alpha = 0.35f + 0.08f * tw(2, phase)))
    // sparks
    for (i in 0 until 14) {
        val rnd = Random(i + 11)
        val t = (phase * (0.8f + rnd.nextFloat()) + rnd.nextFloat()) % 1f
        val x = 0.7f + (rnd.nextFloat() - 0.4f) * 0.2f * t
        val y = 0.5f - t * 0.25f
        dot(x, y, 0.004f, Color(0xFFFFA23A), (1f - t) * 0.95f)
    }
    vignette(0.55f)
}

private fun DrawScope.hospital(phase: Float) {
    fillV(Color(0xFF6D8DA3), Color(0xFFB9D3DE))
    // window with curtains
    box(0.06f, 0.1f, 0.34f, 0.42f, Color(0xFF9AB2C2)); boxV(0.075f, 0.12f, 0.31f, 0.38f, Color(0xFFBFE0FF), Color(0xFFF2F9FF))
    poly(Color(0xFF4F8C9E), 0.9f, 0.05f to 0.09f, 0.16f to 0.09f, 0.12f to 0.54f, 0.05f to 0.54f)
    poly(Color(0xFF4F8C9E), 0.9f, 0.41f to 0.09f, 0.30f to 0.09f, 0.34f to 0.54f, 0.41f to 0.54f)
    // floor
    boxV(0f, 0.7f, 1f, 0.3f, Color(0xFF8EA6B4), Color(0xFF4E6272))
    // bed
    round(0.40f, 0.52f, 0.5f, 0.09f, 10f, Color(0xFFE8F1F6))
    round(0.40f, 0.57f, 0.5f, 0.1f, 10f, Color(0xFF3F9AA8))
    round(0.42f, 0.47f, 0.13f, 0.08f, 14f, Color.White)
    box(0.42f, 0.67f, 0.012f, 0.18f, Color(0xFF3B4A57)); box(0.88f, 0.67f, 0.012f, 0.18f, Color(0xFF3B4A57))
    // monitor stand + heartbeat
    box(0.62f, 0.30f, 0.26f, 0.15f, Color(0xFF0D1B24))
    box(0.63f, 0.31f, 0.24f, 0.13f, Color(0xFF06140F))
    val p = Path()
    val x0 = 0.64f * size.width; val x1 = 0.86f * size.width; val y0 = 0.375f * size.height
    var x = x0
    p.moveTo(x, y0)
    val shift = phase * (x1 - x0)
    while (x < x1) {
        val u = ((x - x0 + shift) % (x1 - x0)) / (x1 - x0)
        val dy = when {
            u in 0.40f..0.44f -> -0.05f * size.height * ((u - 0.40f) / 0.04f)
            u in 0.44f..0.48f -> 0.055f * size.height * ((u - 0.44f) / 0.04f) - 0.05f * size.height
            u in 0.48f..0.52f -> 0.02f * size.height * (1f - (u - 0.48f) / 0.04f)
            else -> 0f
        }
        p.lineTo(x, y0 + dy)
        x += 3f
    }
    drawPath(p, Color(0xFF4CFF9A), style = Stroke(3f))
    // IV pole
    box(0.935f, 0.2f, 0.006f, 0.55f, Color(0xFF3B4A57)); round(0.915f, 0.2f, 0.045f, 0.07f, 8f, Color(0xFFDCEFFF), 0.8f)
    glow(0.5f, 0.05f, 0.5f, Color(0x44FFFFFF))
    vignette(0.45f)
}

private fun DrawScope.coffeeShop(phase: Float) {
    fillV(Color(0xFF2A1B14), Color(0xFF4A2F20))
    // rainy window with bokeh
    box(0.05f, 0.10f, 0.52f, 0.46f, Color(0xFF1A120D))
    boxV(0.065f, 0.12f, 0.49f, 0.42f, Color(0xFF0E1A3C), Color(0xFF2E3F80))
    for (i in 0 until 24) {
        val rnd = Random(i + 50)
        val c = listOf(Color(0xFFFFC27A), Color(0xFF7FB6FF), Color(0xFFFF7A9A), Color(0xFFFFE2A8))[i % 4]
        dot(0.08f + rnd.nextFloat() * 0.45f, 0.14f + rnd.nextFloat() * 0.36f, 0.014f + rnd.nextFloat() * 0.02f, c, 0.25f + 0.25f * tw(i, phase))
    }
    for (i in 0 until 10) {
        val rnd = Random(i + 80)
        val t = (phase * 2f + rnd.nextFloat()) % 1f
        line(0.08f + rnd.nextFloat() * 0.45f, 0.12f + t * 0.4f, 0.08f + rnd.nextFloat() * 0.0f + 0.002f, 0.12f + t * 0.4f + 0.03f, Color.White, 1.5f, 0.0f)
    }
    // pendant lamps
    for (i in 0 until 3) {
        val x = 0.66f + i * 0.14f
        line(x, 0f, x, 0.16f, Color(0xFF0D0805), 2f)
        poly(Color(0xFFE5A24A), 1f, (x - 0.035f) to 0.2f, (x + 0.035f) to 0.2f, (x + 0.02f) to 0.15f, (x - 0.02f) to 0.15f)
        glow(x, 0.22f, 0.22f, Color(0x88FFC46B).copy(alpha = 0.3f + 0.1f * tw(i, phase)))
    }
    // counter
    boxV(0.56f, 0.44f, 0.44f, 0.14f, Color(0xFF6A4630), Color(0xFF3A2519))
    // floor
    boxV(0f, 0.7f, 1f, 0.3f, Color(0xFF2B1D15), Color(0xFF120B07))
    // table + cups with steam
    round(0.18f, 0.70f, 0.52f, 0.035f, 8f, Color(0xFF7A5238))
    box(0.43f, 0.735f, 0.02f, 0.2f, Color(0xFF2B1D15))
    round(0.28f, 0.64f, 0.07f, 0.06f, 8f, Color(0xFFEFE6D8)); round(0.52f, 0.64f, 0.07f, 0.06f, 8f, Color(0xFFEFE6D8))
    for (i in 0 until 2) for (k in 0 until 3) {
        val x = if (i == 0) 0.315f else 0.555f
        val t = (phase * 1.3f + k * 0.33f + i * 0.2f) % 1f
        dot(x + 0.01f * sin((t + k) * TWO_PI), 0.62f - t * 0.1f, 0.009f, Color.White, (1f - t) * 0.35f)
    }
    vignette(0.5f)
}

private fun DrawScope.meeting(phase: Float) {
    fillV(Color(0xFF1A2455), Color(0xFFEE8B5A))
    glow(0.5f, 0.52f, 0.6f, Color(0x66FFC27A))
    buildings(61, 0.56f, 0.08f, 0.34f, Color(0xFF12194A), Color(0xFFFFC27A), phase, 0.4f, 0.04f, 0.1f)
    buildings(62, 0.56f, 0.04f, 0.2f, Color(0xFF0A0F32), Color(0xFFFFE2A8), phase, 0.45f, 0.05f, 0.11f)
    for (i in 1..4) box(i * 0.2f, 0f, 0.006f, 0.56f, Color(0xFF0A0F32), 0.7f)
    // wall screen
    box(0.28f, 0.06f, 0.44f, 0.26f, Color(0xFF05091F), 0.85f)
    for (i in 0 until 5) box(0.32f + i * 0.075f, 0.28f - (0.04f + i * 0.03f), 0.04f, 0.04f + i * 0.03f, Color(0xFF6FA8FF), 0.9f)
    line(0.32f, 0.2f, 0.68f, 0.1f, Color(0xFFFFC83D), 3f, 0.9f)
    // floor and table
    boxV(0f, 0.7f, 1f, 0.3f, Color(0xFF151B3A), Color(0xFF080B1C))
    poly(Color(0xFF2B1E2A), 1f, 0.10f to 0.66f, 0.90f to 0.66f, 1.0f to 0.80f, 0.0f to 0.80f)
    poly(Color(0xFFFFFFFF), 0.12f, 0.10f to 0.66f, 0.90f to 0.66f, 0.95f to 0.69f, 0.05f to 0.69f)
    for (i in 0 until 5) {
        val x = 0.14f + i * 0.17f
        round(x, 0.55f, 0.09f, 0.12f, 20f, Color(0xFF0D132E))
    }
    // documents / laptop glow
    for (i in 0 until 3) { box(0.22f + i * 0.25f, 0.70f, 0.1f, 0.012f, Color(0xFFBFD8FF), 0.8f) }
    vignette(0.45f)
}

private fun DrawScope.airport(phase: Float) {
    fillV(Color(0xFF151C4A), Color(0xFFE7806A), Color(0xFFFFC27A))
    glow(0.2f, 0.55f, 0.6f, Color(0x88FFD58A))
    // tarmac
    boxV(0f, 0.54f, 1f, 0.46f, Color(0xFF1B2140), Color(0xFF0A0D1E))
    // runway lights converging
    for (i in 0 until 12) {
        val t = i / 12f
        val y = 0.56f + t * t * 0.3f
        val x1 = 0.5f - (0.1f + 0.45f * t * t)
        val x2 = 0.5f + (0.1f + 0.45f * t * t)
        dot(x1, y, 0.004f + t * 0.006f, Color(0xFFFFE08A), 0.6f + 0.4f * tw(i, phase, 2f))
        dot(x2, y, 0.004f + t * 0.006f, Color(0xFF7FB6FF), 0.6f + 0.4f * tw(i + 3, phase, 2f))
    }
    // plane
    val px = 0.58f + 0.03f * sin(phase * TWO_PI)
    val py = 0.46f
    round(px - 0.2f, py - 0.03f, 0.4f, 0.06f, 30f, Color(0xFFE8EEFF))
    poly(Color(0xFFCAD4F0), 1f, (px - 0.02f) to py, (px + 0.12f) to (py + 0.16f), (px + 0.04f) to (py + 0.16f), (px - 0.1f) to py)
    poly(Color(0xFFCAD4F0), 1f, (px - 0.17f) to (py - 0.02f), (px - 0.2f) to (py - 0.13f), (px - 0.14f) to (py - 0.13f), (px - 0.09f) to (py - 0.02f))
    box(px - 0.1f, py - 0.012f, 0.22f, 0.008f, Color(0xFF2F7BFF), 0.8f)
    // terminal frame
    box(0f, 0f, 1f, 0.07f, Color(0xFF0B1030)); box(0f, 0.07f, 0.02f, 0.5f, Color(0xFF0B1030)); box(0.98f, 0.07f, 0.02f, 0.5f, Color(0xFF0B1030))
    for (i in 1..3) box(i * 0.25f, 0.07f, 0.008f, 0.47f, Color(0xFF0B1030), 0.8f)
    for (i in 0 until 4) { box(0.1f + i * 0.25f, 0.02f, 0.14f, 0.012f, Color.White, 0.8f) }
    // floor reflections
    boxV(0f, 0.86f, 1f, 0.14f, Color(0xFF2A3466), Color(0xFF0E1230))
    glow(0.25f, 0.9f, 0.3f, Color(0x33FFC27A))
    vignette(0.35f)
}

private fun DrawScope.wedding(phase: Float) {
    fillV(Color(0xFF2A1C3A), Color(0xFF8E5A7A), Color(0xFFE3A57C))
    // bokeh string lights
    for (i in 0 until 34) {
        val rnd = Random(i + 100)
        val c = listOf(Color(0xFFFFE2A8), Color(0xFFFFB3C6), Color.White)[i % 3]
        dot(rnd.nextFloat(), 0.04f + rnd.nextFloat() * 0.4f, 0.01f + rnd.nextFloat() * 0.018f, c, 0.2f + 0.45f * tw(i, phase))
    }
    // floral arch
    val archColor = Color(0xFFE7EAF2)
    drawArc(archColor.copy(alpha = 0.95f), 180f, 180f, false, Offset(size.width * 0.28f, size.height * 0.12f), Size(size.width * 0.44f, size.height * 0.56f), style = Stroke(10f))
    box(0.275f, 0.4f, 0.012f, 0.32f, archColor); box(0.713f, 0.4f, 0.012f, 0.32f, archColor)
    for (i in 0 until 26) {
        val a = PI.toFloat() * (i / 25f)
        val cx = 0.5f - 0.22f * kotlin.math.cos(a)
        val cy = 0.40f - 0.28f * sin(a) * 0.9f + 0.05f
        val c = listOf(Color(0xFFFF8FB0), Color(0xFFFFFFFF), Color(0xFFFFC2D6), Color(0xFF8CCF9A))[i % 4]
        dot(cx, cy, 0.022f, c)
    }
    // aisle and chairs
    boxV(0f, 0.68f, 1f, 0.32f, Color(0xFF6E4B5C), Color(0xFF241522))
    poly(Color(0xFFF2D6C0), 0.9f, 0.44f to 0.68f, 0.56f to 0.68f, 0.78f to 1f, 0.22f to 1f)
    for (side in 0..1) for (row in 0 until 4) {
        val y = 0.72f + row * 0.065f
        val s = 1f + row * 0.35f
        val x = if (side == 0) 0.12f - row * 0.015f else 0.74f + row * 0.0f
        round(x, y, 0.14f * s * 0.9f, 0.03f * s, 8f, Color(0xFF1A0E1C), 0.85f)
    }
    // falling petals
    for (i in 0 until 14) {
        val rnd = Random(i + 200)
        val t = (phase * (0.5f + rnd.nextFloat() * 0.5f) + rnd.nextFloat()) % 1f
        dot(rnd.nextFloat() + 0.03f * sin((t + i) * TWO_PI), t, 0.006f, Color(0xFFFFC2D6), 0.7f)
    }
    vignette(0.4f)
}

private fun DrawScope.newHome(phase: Float) {
    fillV(Color(0xFF38589A), Color(0xFFFF9E6A), Color(0xFFFFCF86))
    glow(0.78f, 0.5f, 0.55f, Color(0x88FFD58A))
    dot(0.78f, 0.52f, 0.06f, Color(0xFFFFEFC2))
    // lawn
    boxV(0f, 0.62f, 1f, 0.38f, Color(0xFF3C6E4E), Color(0xFF14301F))
    poly(Color(0xFFD7C3A0), 0.8f, 0.45f to 0.8f, 0.55f to 0.8f, 0.7f to 1f, 0.3f to 1f)
    // house body
    box(0.18f, 0.44f, 0.46f, 0.34f, Color(0xFFD9C6A8))
    poly(Color(0xFF7A3F3F), 1f, 0.14f to 0.45f, 0.41f to 0.22f, 0.68f to 0.45f)
    box(0.52f, 0.28f, 0.05f, 0.1f, Color(0xFF5A2E2E))
    // door and windows (warm)
    box(0.37f, 0.58f, 0.08f, 0.2f, Color(0xFF3B2A5C))
    dot(0.435f, 0.69f, 0.004f, Color(0xFFFFC83D))
    for (x in listOf(0.22f, 0.50f)) {
        box(x, 0.52f, 0.1f, 0.12f, Color(0xFFFFD58A), 0.85f + 0.1f * tw(0, phase))
        box(x + 0.047f, 0.52f, 0.006f, 0.12f, Color(0xFF5A3A2A))
        box(x, 0.575f, 0.1f, 0.006f, Color(0xFF5A3A2A))
    }
    glow(0.27f, 0.58f, 0.18f, Color(0x66FFD58A)); glow(0.55f, 0.58f, 0.18f, Color(0x66FFD58A))
    // trees
    treeBlob(0.08f, 0.8f, 0.14f, Color(0xFF3A2A1D), Color(0xFF1F5A3A), Color(0xFF2A7048))
    treeBlob(0.92f, 0.82f, 0.12f, Color(0xFF3A2A1D), Color(0xFF1F5A3A), Color(0xFF2A7048))
    // picket fence
    for (i in 0 until 14) box(0.02f + i * 0.07f, 0.86f, 0.012f, 0.07f, Color(0xFFF2E8D8), 0.9f)
    box(0f, 0.88f, 1f, 0.008f, Color(0xFFF2E8D8), 0.9f)
    vignette(0.3f)
}

private fun DrawScope.smallBusiness(phase: Float) {
    fillV(Color(0xFF1B2250), Color(0xFF8E5A8A), Color(0xFFFFA06A))
    buildings(71, 0.5f, 0.1f, 0.3f, Color(0xFF14183F), Color(0xFFFFD27A), phase, 0.3f, 0.06f, 0.12f)
    // facade
    box(0.08f, 0.26f, 0.84f, 0.5f, Color(0xFF3B2E4F))
    box(0.08f, 0.26f, 0.84f, 0.012f, Color(0xFF241A33))
    // sign
    round(0.22f, 0.29f, 0.56f, 0.1f, 10f, Color(0xFF0E1530))
    round(0.235f, 0.305f, 0.53f, 0.07f, 6f, Color(0xFFFFC83D), 0.92f)
    for (i in 0 until 6) box(0.27f + i * 0.075f, 0.325f, 0.045f, 0.025f, Color(0xFF0E1530), 0.85f)
    // awning stripes
    for (i in 0 until 10) {
        val c = if (i % 2 == 0) Color(0xFFD9442E) else Color(0xFFF5E9D6)
        poly(c, 1f, (0.08f + i * 0.084f) to 0.42f, (0.08f + (i + 1) * 0.084f) to 0.42f, (0.08f + (i + 1) * 0.084f + 0.012f) to 0.5f, (0.08f + i * 0.084f + 0.012f) to 0.5f)
    }
    // window display + door
    box(0.12f, 0.53f, 0.45f, 0.2f, Color(0xFFFFD58A), 0.9f)
    glow(0.35f, 0.64f, 0.35f, Color(0x88FFC46B).copy(alpha = 0.3f + 0.1f * tw(3, phase)))
    for (i in 0 until 4) { round(0.16f + i * 0.1f, 0.64f, 0.06f, 0.08f, 6f, Color(0xFF7A4A2E)); dot(0.19f + i * 0.1f, 0.62f, 0.014f, Color(0xFFE5732B)) }
    box(0.64f, 0.53f, 0.2f, 0.23f, Color(0xFF241A33)); box(0.66f, 0.55f, 0.16f, 0.19f, Color(0xFFFFE2A8), 0.65f)
    round(0.7f, 0.50f, 0.08f, 0.03f, 6f, Color(0xFF3FD18A))
    // sidewalk
    boxV(0f, 0.76f, 1f, 0.24f, Color(0xFF3A3F5A), Color(0xFF14172B))
    box(0f, 0.76f, 1f, 0.012f, Color(0xFF8A90B0), 0.5f)
    vignette(0.4f)
}

private fun DrawScope.skylineSunrise(phase: Float) {
    fillV(Color(0xFF1A3A7C), Color(0xFF8E6AA0), Color(0xFFFFB45C))
    glow(0.5f, 0.62f, 0.7f, Color(0xAAFFD28A))
    dot(0.5f, 0.6f, 0.09f, Color(0xFFFFF0C4))
    // clouds
    for (i in 0 until 4) {
        val x = ((i * 0.27f + phase * 0.15f) % 1.2f) - 0.1f
        round(x, 0.12f + i * 0.07f, 0.2f, 0.035f, 18f, Color.White, 0.18f)
    }
    buildings(91, 0.72f, 0.14f, 0.5f, Color(0xFF3B3F6E), Color(0xFFFFE2A8), phase, 0.28f, 0.04f, 0.09f)
    buildings(92, 0.82f, 0.1f, 0.38f, Color(0xFF242A55), Color(0xFFFFD27A), phase, 0.35f, 0.05f, 0.1f)
    buildings(93, 0.94f, 0.06f, 0.26f, Color(0xFF12163A), Color(0xFFFFE2A8), phase, 0.4f, 0.06f, 0.12f)
    boxV(0f, 0.93f, 1f, 0.07f, Color(0xFF0C1030), Color(0xFF05071A))
    vignette(0.3f)
}

private fun DrawScope.campus(phase: Float) {
    fillV(Color(0xFF3B78C9), Color(0xFFBFDFFF))
    for (i in 0 until 3) {
        val x = ((i * 0.4f + phase * 0.1f) % 1.3f) - 0.15f
        round(x, 0.1f + i * 0.06f, 0.22f, 0.04f, 20f, Color.White, 0.65f)
    }
    // building with pediment and columns
    box(0.18f, 0.42f, 0.64f, 0.3f, Color(0xFFE9E1CF))
    poly(Color(0xFFD8CDB4), 1f, 0.14f to 0.43f, 0.5f to 0.24f, 0.86f to 0.43f)
    dot(0.5f, 0.35f, 0.025f, Color(0xFF8A7A5A))
    for (i in 0 until 6) box(0.22f + i * 0.1f, 0.46f, 0.03f, 0.26f, Color(0xFFF7F2E6))
    box(0.15f, 0.72f, 0.7f, 0.025f, Color(0xFFCFC4AA)); box(0.12f, 0.745f, 0.76f, 0.025f, Color(0xFFBFB398))
    box(0.44f, 0.58f, 0.12f, 0.14f, Color(0xFF3A3050))
    // banner
    poly(Color(0xFFFFC83D), 1f, 0.42f to 0.46f, 0.58f to 0.46f, 0.58f to 0.56f, 0.5f to 0.53f, 0.42f to 0.56f)
    // lawn and path
    boxV(0f, 0.77f, 1f, 0.23f, Color(0xFF4F9A5E), Color(0xFF1E5A34))
    poly(Color(0xFFE5D8B8), 0.9f, 0.46f to 0.77f, 0.54f to 0.77f, 0.72f to 1f, 0.28f to 1f)
    treeBlob(0.07f, 0.8f, 0.15f, Color(0xFF4A3322), Color(0xFF2F8049), Color(0xFF3EA25F))
    treeBlob(0.93f, 0.82f, 0.14f, Color(0xFF4A3322), Color(0xFF2F8049), Color(0xFF3EA25F))
    // flying caps
    for (i in 0 until 5) {
        val t = (phase + i * 0.2f) % 1f
        val x = 0.2f + i * 0.15f
        val y = 0.9f - 0.5f * sin(t * PI.toFloat())
        poly(Color(0xFF14183A), 1f, (x - 0.03f) to y, x to (y - 0.012f), (x + 0.03f) to y, x to (y + 0.012f))
    }
    vignette(0.25f)
}

private fun DrawScope.park(phase: Float) {
    fillV(Color(0xFF4F79C8), Color(0xFFFFCF9A))
    glow(0.2f, 0.25f, 0.6f, Color(0x88FFEFC2))
    // light beams
    for (i in 0 until 3) poly(Color.White, 0.08f + 0.04f * tw(i, phase), (0.1f + i * 0.08f) to 0f, (0.18f + i * 0.08f) to 0f, (0.55f + i * 0.2f) to 0.9f, (0.35f + i * 0.2f) to 0.9f)
    // hills
    poly(Color(0xFF3A7B55), 1f, 0f to 0.58f, 0.3f to 0.48f, 0.6f to 0.56f, 1f to 0.46f, 1f to 1f, 0f to 1f)
    polyV(Color(0xFF2F6B48), Color(0xFF123022), 0.6f, 1f, 0f to 0.7f, 0.4f to 0.62f, 1f to 0.72f, 1f to 1f, 0f to 1f)
    treeBlob(0.12f, 0.66f, 0.2f, Color(0xFF3A2A1D), Color(0xFF1F6A44), Color(0xFF2E8556))
    treeBlob(0.86f, 0.64f, 0.22f, Color(0xFF3A2A1D), Color(0xFF1F6A44), Color(0xFF2E8556))
    treeBlob(0.66f, 0.6f, 0.12f, Color(0xFF3A2A1D), Color(0xFF2A7A4E), Color(0xFF3A9A66))
    // path and bench
    poly(Color(0xFFE5D2AE), 0.85f, 0.4f to 0.66f, 0.55f to 0.66f, 0.85f to 1f, 0.1f to 1f)
    box(0.52f, 0.72f, 0.22f, 0.02f, Color(0xFF5A3A22)); box(0.52f, 0.68f, 0.22f, 0.018f, Color(0xFF6A4630))
    box(0.54f, 0.74f, 0.012f, 0.06f, Color(0xFF241812)); box(0.72f, 0.74f, 0.012f, 0.06f, Color(0xFF241812))
    // leaves
    for (i in 0 until 12) {
        val rnd = Random(i + 300)
        val t = (phase * (0.4f + rnd.nextFloat() * 0.4f) + rnd.nextFloat()) % 1f
        dot(rnd.nextFloat() + 0.04f * sin((t + i) * TWO_PI), t, 0.006f, listOf(Color(0xFFE5A24A), Color(0xFFD9442E), Color(0xFF8CCF5A))[i % 3], 0.85f)
    }
    vignette(0.3f)
}

private fun DrawScope.street(phase: Float) {
    fillV(Color(0xFF1B2250), Color(0xFF7A4A8E), Color(0xFFFF9A6B))
    glow(0.5f, 0.52f, 0.5f, Color(0x88FFC27A))
    // perspective buildings left/right
    for (i in 0 until 5) {
        val t0 = i / 5f; val t1 = (i + 1) / 5f
        val lx0 = 0.5f * (1f - t0) * 0.95f; val lx1 = 0.5f * (1f - t1) * 0.95f
        val topL0 = 0.54f - (0.5f - 0.05f * i) * (1 - t0) * 0.9f
        val c = listOf(Color(0xFF14183F), Color(0xFF171C45), Color(0xFF1A204B), Color(0xFF1D2352), Color(0xFF202759))[i]
        poly(c, 1f, (0.5f - lx0 - 0.0f) to topL0 - 0.0f, (0.5f - lx1) to (0.54f - 0.28f * (1 - t1)), (0.5f - lx1) to 0.78f, (0.5f - lx0) to 0.95f)
        poly(c, 1f, (0.5f + lx0) to topL0, (0.5f + lx1) to (0.54f - 0.28f * (1 - t1)), (0.5f + lx1) to 0.78f, (0.5f + lx0) to 0.95f)
        for (w in 0 until 3) {
            val yy = 0.42f + w * 0.12f * (1 - t0 * 0.6f)
            if (yy < 0.75f) {
                dot(0.5f - lx0 * 0.78f, yy, 0.012f * (1 - t0 * 0.5f), Color(0xFFFFD27A), 0.5f + 0.4f * tw(i + w, phase))
                dot(0.5f + lx0 * 0.78f, yy, 0.012f * (1 - t0 * 0.5f), Color(0xFFFFD27A), 0.5f + 0.4f * tw(i + w + 4, phase))
            }
        }
    }
    // road
    poly(Color(0xFF1A1F3A), 1f, 0.46f to 0.78f, 0.54f to 0.78f, 1f to 1f, 0f to 1f)
    for (i in 0 until 5) {
        val t = i / 5f
        val y = 0.80f + t * t * 0.18f
        box(0.495f - 0.005f * t, y, 0.01f + 0.01f * t, 0.025f * (0.5f + t), Color(0xFFF2E8D8), 0.8f)
    }
    // lamps
    for (side in listOf(-1, 1)) for (i in 0 until 3) {
        val t = i / 3f
        val x = 0.5f + side * (0.14f + 0.3f * t * t + 0.02f)
        val y = 0.78f + t * 0.16f
        box(x, y - 0.2f * (0.5f + t), 0.004f + 0.003f * t, 0.2f * (0.5f + t), Color(0xFF0A0C1E))
        glow(x, y - 0.2f * (0.5f + t), 0.09f * (0.6f + t), Color(0xAAFFD27A))
    }
    vignette(0.4f)
}

private fun DrawScope.kitchen(phase: Float) {
    fillV(Color(0xFF3B2A1E), Color(0xFF6A4B30))
    // window with morning light
    box(0.54f, 0.1f, 0.36f, 0.4f, Color(0xFF2A1D14))
    boxV(0.565f, 0.12f, 0.31f, 0.36f, Color(0xFFBFE0FF), Color(0xFFFFF2D0))
    box(0.717f, 0.12f, 0.008f, 0.36f, Color(0xFF2A1D14)); box(0.565f, 0.29f, 0.31f, 0.008f, Color(0xFF2A1D14))
    poly(Color(0xFFFFF2C8), 0.14f + 0.05f * tw(0, phase), 0.565f to 0.48f, 0.875f to 0.48f, 0.55f to 0.98f, 0.0f to 0.98f)
    // shelves with jars
    box(0.05f, 0.24f, 0.4f, 0.012f, Color(0xFF2A1D14)); box(0.05f, 0.42f, 0.4f, 0.012f, Color(0xFF2A1D14))
    for (i in 0 until 5) { round(0.07f + i * 0.075f, 0.17f, 0.045f, 0.07f, 8f, listOf(Color(0xFFE5A24A), Color(0xFFB7C0D4), Color(0xFFD9442E), Color(0xFF8CCF5A), Color(0xFFF2E8D8))[i], 0.85f) }
    for (i in 0 until 4) { round(0.08f + i * 0.09f, 0.35f, 0.06f, 0.07f, 10f, Color(0xFF8A5A3A), 0.9f) }
    // table and plates
    boxV(0f, 0.72f, 1f, 0.28f, Color(0xFF3A281B), Color(0xFF150E09))
    round(0.1f, 0.6f, 0.8f, 0.045f, 8f, Color(0xFF8A5A3A))
    box(0.16f, 0.645f, 0.02f, 0.3f, Color(0xFF2A1D14)); box(0.82f, 0.645f, 0.02f, 0.3f, Color(0xFF2A1D14))
    round(0.25f, 0.57f, 0.15f, 0.03f, 10f, Color(0xFFF2E8D8)); round(0.55f, 0.57f, 0.15f, 0.03f, 10f, Color(0xFFF2E8D8))
    round(0.44f, 0.5f, 0.07f, 0.1f, 8f, Color(0xFF3FA6A6))
    for (k in 0 until 3) {
        val t = (phase * 1.2f + k * 0.33f) % 1f
        dot(0.475f + 0.012f * sin((t + k) * TWO_PI), 0.49f - t * 0.1f, 0.009f, Color.White, (1f - t) * 0.35f)
    }
    // pendant lamp
    line(0.3f, 0f, 0.3f, 0.2f, Color(0xFF0D0805), 2f)
    poly(Color(0xFFE5A24A), 1f, 0.26f to 0.25f, 0.34f to 0.25f, 0.32f to 0.2f, 0.28f to 0.2f)
    glow(0.3f, 0.28f, 0.3f, Color(0x88FFC46B).copy(alpha = 0.3f + 0.08f * tw(2, phase)))
    vignette(0.4f)
}


private fun DrawScope.bedroom(phase: Float) {
    fillV(Color(0xFF1A2040), Color(0xFF2A2E58))
    // window with moon
    box(0.60f, 0.10f, 0.30f, 0.38f, Color(0xFF2A3560))
    boxV(0.615f, 0.12f, 0.27f, 0.34f, Color(0xFF0B1642), Color(0xFF2B4C9E))
    stars(14, phase, 0.3f, seed = 3)
    dot(0.80f, 0.2f, 0.035f, Color(0xFFEAF2FF)); glow(0.80f, 0.2f, 0.12f, Color(0x55BFD8FF))
    box(0.748f, 0.12f, 0.006f, 0.34f, Color(0xFF2A3560))
    // posters and shelf
    box(0.08f, 0.16f, 0.14f, 0.2f, Color(0xFF304B8C)); box(0.10f, 0.19f, 0.10f, 0.06f, Color(0xFFFFC83D), 0.8f)
    box(0.28f, 0.28f, 0.22f, 0.012f, Color(0xFF3A2D26))
    for (i in 0 until 4) round(0.30f + i * 0.05f, 0.21f, 0.035f, 0.07f, 4f, listOf(Color(0xFFE5A24A), Color(0xFF3FA6A6), Color(0xFFD9442E), Color(0xFF8CCF5A))[i], 0.9f)
    // floor and bed
    boxV(0f, 0.72f, 1f, 0.28f, Color(0xFF2A2440), Color(0xFF10101E))
    round(0.04f, 0.58f, 0.46f, 0.14f, 14f, Color(0xFF2F4E8F))
    round(0.04f, 0.52f, 0.14f, 0.1f, 20f, Color(0xFFE8EEFF), 0.9f)
    box(0.04f, 0.72f, 0.46f, 0.05f, Color(0xFF1A2347))
    // desk lamp glow
    glow(0.80f, 0.60f, 0.35f, Color(0x66FFC46B).copy(alpha = 0.3f + 0.08f * tw(1, phase)))
    round(0.66f, 0.62f, 0.30f, 0.035f, 4f, Color(0xFF3A2D26))
    box(0.70f, 0.655f, 0.012f, 0.2f, Color(0xFF2A201B)); box(0.92f, 0.655f, 0.012f, 0.2f, Color(0xFF2A201B))
    vignette(0.5f)
}

private fun DrawScope.schoolYard(phase: Float) {
    fillV(Color(0xFF4A82D0), Color(0xFFCDE4FF))
    for (i in 0 until 3) {
        val x = ((i * 0.38f + phase * 0.08f) % 1.3f) - 0.15f
        round(x, 0.08f + i * 0.07f, 0.2f, 0.04f, 20f, Color.White, 0.7f)
    }
    // school building behind
    box(0.05f, 0.30f, 0.9f, 0.30f, Color(0xFFB9724E))
    for (r in 0 until 2) for (c in 0 until 9) box(0.08f + c * 0.098f, 0.35f + r * 0.12f, 0.06f, 0.07f, Color(0xFFBFE0FF), 0.85f)
    poly(Color(0xFF7A3F3F), 1f, 0.03f to 0.31f, 0.5f to 0.18f, 0.97f to 0.31f)
    box(0.46f, 0.50f, 0.08f, 0.10f, Color(0xFF3A2A5C))
    // fence, yard
    for (i in 0..30) line(i / 30f, 0.60f, i / 30f, 0.68f, Color(0xFFE8E8EE), 2f, 0.7f)
    line(0f, 0.63f, 1f, 0.63f, Color(0xFFE8E8EE), 2f, 0.7f)
    boxV(0f, 0.66f, 1f, 0.34f, Color(0xFF5CA866), Color(0xFF22582F))
    poly(Color(0xFFD9C9A5), 0.9f, 0.40f to 0.66f, 0.60f to 0.66f, 0.80f to 1f, 0.20f to 1f)
    treeBlob(0.06f, 0.80f, 0.15f, Color(0xFF4A3322), Color(0xFF2F8049), Color(0xFF3EA25F))
    treeBlob(0.94f, 0.82f, 0.13f, Color(0xFF4A3322), Color(0xFF2F8049), Color(0xFF3EA25F))
    vignette(0.25f)
}

private fun DrawScope.restaurant(phase: Float) {
    fillV(Color(0xFF2B1A22), Color(0xFF4A2A30))
    // window with evening street
    box(0.05f, 0.10f, 0.40f, 0.40f, Color(0xFF1A1018))
    boxV(0.065f, 0.12f, 0.37f, 0.36f, Color(0xFF1D2B5C), Color(0xFFEE8B5A))
    buildings(15, 0.48f, 0.05f, 0.2f, Color(0xFF12194A), Color(0xFFFFC27A), phase, 0.5f, 0.05f, 0.1f)
    // wall lamps and bottles
    for (i in 0 until 3) {
        val x = 0.58f + i * 0.14f
        poly(Color(0xFFE5A24A), 1f, (x - 0.03f) to 0.2f, (x + 0.03f) to 0.2f, (x + 0.018f) to 0.15f, (x - 0.018f) to 0.15f)
        glow(x, 0.22f, 0.2f, Color(0x88FFC46B).copy(alpha = 0.3f + 0.1f * tw(i, phase)))
        line(x, 0f, x, 0.15f, Color(0xFF0D0805), 2f)
    }
    for (i in 0 until 6) round(0.56f + i * 0.07f, 0.34f, 0.025f, 0.09f, 6f, listOf(Color(0xFF3FA6A6), Color(0xFF8A2F2F), Color(0xFFE5A24A))[i % 3], 0.8f)
    box(0.55f, 0.43f, 0.43f, 0.012f, Color(0xFF6A4630))
    boxV(0f, 0.7f, 1f, 0.3f, Color(0xFF2B1D15), Color(0xFF120B07))
    // tables with candles
    for (i in 0 until 2) {
        val x = 0.12f + i * 0.5f
        round(x, 0.68f, 0.3f, 0.03f, 6f, Color(0xFFEFE6D8))
        box(x + 0.14f, 0.71f, 0.012f, 0.22f, Color(0xFF2B1D15))
        box(x + 0.145f, 0.64f, 0.008f, 0.04f, Color(0xFFF5E9D6))
        glow(x + 0.15f, 0.63f, 0.1f, Color(0xAAFFC46B).copy(alpha = 0.5f + 0.3f * tw(i, phase, 3f)))
    }
    vignette(0.55f)
}

private fun DrawScope.interviewRoom(phase: Float) {
    fillV(Color(0xFF3A4668), Color(0xFF232C48))
    // frosted glass partition and skyline behind
    boxV(0.04f, 0.08f, 0.92f, 0.46f, Color(0xFF9FC4F2), Color(0xFFDCEBFF))
    buildings(25, 0.54f, 0.05f, 0.22f, Color(0xFF7A98C8), Color(0xFFEAF5FF), phase, 0.25f, 0.05f, 0.1f)
    for (i in 1..4) box(0.04f + i * 0.184f, 0.08f, 0.006f, 0.46f, Color(0xFF232C48))
    // wall clock and plant
    dot(0.15f, 0.62f, 0.0f, Color.White)
    dot(0.88f, 0.22f, 0.045f, Color(0xFFF2F4FA)); line(0.88f, 0.22f, 0.88f, 0.19f, Color(0xFF14183A), 2f); line(0.88f, 0.22f, 0.905f, 0.23f, Color(0xFF14183A), 2f)
    boxV(0f, 0.7f, 1f, 0.3f, Color(0xFF2A3350), Color(0xFF10152A))
    // table between two chairs
    round(0.26f, 0.68f, 0.5f, 0.04f, 6f, Color(0xFF6A4630))
    box(0.30f, 0.72f, 0.012f, 0.2f, Color(0xFF2A1D14)); box(0.72f, 0.72f, 0.012f, 0.2f, Color(0xFF2A1D14))
    box(0.46f, 0.65f, 0.1f, 0.03f, Color(0xFFF2F4FA), 0.9f)
    dot(0.06f, 0.66f, 0.03f, Color(0xFF2E8B57)); dot(0.09f, 0.69f, 0.025f, Color(0xFF3AA66A)); box(0.065f, 0.70f, 0.02f, 0.06f, Color(0xFF4B3A2C))
    for (i in 0 until 3) box(0.12f + i * 0.3f, 0.01f, 0.2f, 0.02f, Color.White, 0.8f)
    vignette(0.4f)
}

private fun DrawScope.factory(phase: Float) {
    fillV(Color(0xFF1B1F2A), Color(0xFF2D3140))
    // high windows
    for (i in 0 until 5) boxV(0.06f + i * 0.19f, 0.06f, 0.12f, 0.16f, Color(0xFF7FA6D8), Color(0xFFC9DCF2))
    // ceiling beams and crane
    box(0f, 0.26f, 1f, 0.025f, Color(0xFF3B4254)); box(0.2f, 0.22f, 0.02f, 0.06f, Color(0xFF3B4254)); box(0.7f, 0.22f, 0.02f, 0.06f, Color(0xFF3B4254))
    val cx = 0.3f + 0.3f * (0.5f + 0.5f * sin(phase * TWO_PI))
    line(cx, 0.28f, cx, 0.42f, Color(0xFF9AA4B8), 2f); box(cx - 0.02f, 0.42f, 0.04f, 0.03f, Color(0xFFFFC83D))
    // machines
    round(0.06f, 0.46f, 0.28f, 0.20f, 6f, Color(0xFF3F5A9A)); box(0.10f, 0.40f, 0.08f, 0.06f, Color(0xFF2F4A8A))
    for (i in 0 until 4) dot(0.10f + i * 0.06f, 0.56f, 0.012f, if (i % 2 == 0) Color(0xFF3FD18A) else Color(0xFFFFC83D), 0.6f + 0.4f * tw(i, phase, 2f))
    round(0.62f, 0.42f, 0.32f, 0.24f, 6f, Color(0xFF8A5A2B)); box(0.66f, 0.36f, 0.10f, 0.06f, Color(0xFF6A4420))
    // conveyor
    box(0.0f, 0.66f, 1f, 0.04f, Color(0xFF2A2F3C))
    for (i in 0 until 12) box(((i * 0.09f + phase * 0.09f) % 1.0f), 0.675f, 0.03f, 0.012f, Color(0xFF6A7390))
    boxV(0f, 0.7f, 1f, 0.3f, Color(0xFF3A3F50), Color(0xFF14161F))
    for (i in 0 until 5) box(0.05f + i * 0.2f, 0.71f, 0.08f, 0.008f, Color(0xFFFFC83D), 0.55f)
    // sparks
    for (i in 0 until 10) {
        val rnd = Random(i + 21)
        val t = (phase * (0.9f + rnd.nextFloat()) + rnd.nextFloat()) % 1f
        dot(0.78f + (rnd.nextFloat() - 0.5f) * 0.1f * t, 0.5f - t * 0.2f, 0.004f, Color(0xFFFFA23A), (1f - t) * 0.9f)
    }
    vignette(0.5f)
}
