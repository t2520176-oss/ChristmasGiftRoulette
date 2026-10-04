package com.lifeyourchoice.app.ui.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import com.lifeyourchoice.core.cinema.Emotion
import com.lifeyourchoice.core.cinema.Gesture
import com.lifeyourchoice.core.cinema.Prop
import com.lifeyourchoice.core.model.Gender
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The joint angles of a figure (degrees; 0 = arm/leg hanging straight down, +90 = pointing forward/out,
 * 180 = straight up, negative = across the body). Figures blend from pose to pose, so every gesture
 * is reusable on every character.
 */
class FigurePose {
    var rUa = 6f; var rFa = 8f
    var lUa = 6f; var lFa = 8f
    /** Both arms reach forward (hug). */
    var lForward = 0f
    /** Draw the back arm in front of the torso (crossed arms, clapping...). */
    var armsFront = 0f
    var lean = 0f
    var headTilt = 0f
    var headNod = 0f
    var shoulderDrop = 0f
    var thighL = 0f; var shinL = 0f
    var thighR = 0f; var shinR = 0f
    var bob = 0f
    var sit = 0f

    fun copyFrom(o: FigurePose) {
        rUa = o.rUa; rFa = o.rFa; lUa = o.lUa; lFa = o.lFa; lForward = o.lForward; armsFront = o.armsFront
        lean = o.lean; headTilt = o.headTilt; headNod = o.headNod; shoulderDrop = o.shoulderDrop
        thighL = o.thighL; shinL = o.shinL; thighR = o.thighR; shinR = o.shinR; bob = o.bob; sit = o.sit
    }

    /** Moves this pose towards [t] by fraction [a] (0..1). */
    fun approach(t: FigurePose, a: Float) {
        fun m(x: Float, y: Float) = x + (y - x) * a
        rUa = m(rUa, t.rUa); rFa = m(rFa, t.rFa); lUa = m(lUa, t.lUa); lFa = m(lFa, t.lFa)
        lForward = m(lForward, t.lForward); armsFront = m(armsFront, t.armsFront)
        lean = m(lean, t.lean); headTilt = m(headTilt, t.headTilt); headNod = m(headNod, t.headNod)
        shoulderDrop = m(shoulderDrop, t.shoulderDrop)
        thighL = m(thighL, t.thighL); shinL = m(shinL, t.shinL); thighR = m(thighR, t.thighR); shinR = m(shinR, t.shinR)
        bob = m(bob, t.bob); sit = m(sit, t.sit)
    }
}

/** Turns "what the actor is doing" into joint angles. [t] is seconds, [walk] the walk-cycle phase. */
private val HELD_IN_HAND = setOf(Prop.CUP, Prop.BOOK, Prop.DOCUMENTS, Prop.FLOWERS, Prop.BOX)

object PoseSolver {
    fun solve(
        gesture: Gesture, emotion: Emotion, speaking: Boolean, moving: Boolean, running: Boolean,
        seated: Boolean, t: Float, walk: Float, out: FigurePose, prop: Prop = Prop.NONE
    ) {
        val p = out
        p.rUa = 6f; p.rFa = 8f; p.lUa = 6f; p.lFa = 8f; p.lForward = 0f; p.armsFront = 0f
        p.lean = 0f; p.headTilt = 0f; p.headNod = 0f; p.shoulderDrop = 0f; p.bob = 0f
        p.thighL = 0f; p.shinL = 0f; p.thighR = 0f; p.shinR = 0f
        p.sit = if (seated) 1f else 0f

        // Posture that comes with the mood.
        when (emotion) {
            Emotion.SAD, Emotion.DISAPPOINTED -> { p.lean = 4f; p.headNod = 13f; p.shoulderDrop = 4f }
            Emotion.TIRED -> { p.lean = 5f; p.headNod = 9f; p.shoulderDrop = 5f; p.bob = sin(t * 1.4f) * 0.8f }
            Emotion.PROUD, Emotion.CONFIDENT -> { p.lean = -2f; p.headNod = -4f }
            Emotion.ANGRY -> { p.rUa = 30f; p.rFa = -52f; p.lUa = 30f; p.lFa = -52f; p.lean = 3f }
            Emotion.AFRAID -> { p.rUa = 14f; p.rFa = -75f; p.lUa = 14f; p.lFa = -75f; p.armsFront = 1f; p.shoulderDrop = -3f }
            Emotion.EXCITED -> { p.rUa = 32f + 12f * sin(t * 9f); p.rFa = 80f; p.lUa = 32f + 12f * sin(t * 9f + 2f); p.lFa = 80f; p.bob = abs(sin(t * 9f)) * 2.5f }
            Emotion.LAUGHING -> { p.lean = 3f * sin(t * 14f); p.headNod = -8f; p.bob = abs(sin(t * 14f)) * 2f; p.rUa = 25f; p.rFa = 90f; p.lUa = 20f; p.lFa = -70f; p.armsFront = 1f }
            Emotion.THOUGHTFUL -> p.headTilt = 6f
            Emotion.WORRIED -> { p.headNod = 5f; p.shoulderDrop = 2f }
            Emotion.EMBARRASSED -> { p.headNod = 8f; p.headTilt = 6f }
            Emotion.SURPRISED -> { p.lean = -4f; p.rUa = 28f; p.rFa = 70f; p.lUa = 28f; p.lFa = 70f }
            else -> {}
        }

        // Explicit gestures.
        when (gesture) {
            Gesture.NONE -> {}
            Gesture.EXPLAIN -> { p.rUa = 28f; p.rFa = 100f + 14f * sin(t * 5f); p.lUa = 16f; p.lFa = -62f + 6f * sin(t * 4.3f + 1f); p.armsFront = 1f }
            Gesture.POINT -> { p.rUa = 78f; p.rFa = 86f }
            Gesture.SHRUG -> { p.rUa = 30f; p.rFa = 120f; p.lUa = 30f; p.lFa = 120f; p.shoulderDrop = -5f; p.headTilt = 6f }
            Gesture.WAVE -> { p.rUa = 140f; p.rFa = 160f + 26f * sin(t * 9f) }
            Gesture.CROSS_ARMS -> { p.rUa = 18f; p.rFa = -88f; p.lUa = 18f; p.lFa = -88f; p.armsFront = 1f }
            Gesture.THINK -> { p.rUa = 22f; p.rFa = -156f; p.lUa = 18f; p.lFa = -85f; p.armsFront = 1f; p.headTilt = 6f }
            Gesture.HEAD_DOWN -> { p.headNod = 20f; p.lean = 5f; p.shoulderDrop = 5f }
            Gesture.HANDS_UP -> { p.rUa = 150f; p.rFa = 170f; p.lUa = 150f; p.lFa = 170f }
            Gesture.NOD -> p.headNod = 9f * sin(t * 7f)
            Gesture.SHAKE_HEAD -> p.headTilt = 9f * sin(t * 9f)
            Gesture.HANDSHAKE -> { p.rUa = 80f; p.rFa = 88f + 5f * sin(t * 6f); p.lean = 3f }
            Gesture.HUG -> { p.rUa = 70f; p.rFa = 100f; p.lUa = 70f; p.lFa = 100f; p.lForward = 1f; p.lean = 5f; p.headTilt = 6f; p.armsFront = 1f }
            Gesture.PHONE -> { p.rUa = 28f; p.rFa = -157f; p.headTilt = -6f }
            Gesture.TYPE -> { p.lForward = 1f; p.rUa = 14f; p.rFa = 86f + 6f * sin(t * 18f); p.lUa = 14f; p.lFa = 86f + 6f * sin(t * 18f + 2f); p.headNod = 6f; p.armsFront = 1f }
            Gesture.CLAP -> { p.rUa = 24f; p.rFa = -100f + 14f * sin(t * 12f); p.lUa = 24f; p.lFa = -100f - 14f * sin(t * 12f); p.armsFront = 1f }
            Gesture.FACEPALM -> { p.rUa = 20f; p.rFa = -160f; p.headNod = 16f; p.lean = 4f }
            Gesture.SCRATCH_HEAD -> { p.rUa = 66f; p.rFa = 172f + 8f * sin(t * 6f); p.headTilt = 6f }
            Gesture.FIST_PUMP -> { p.rUa = 36f; p.rFa = 150f + 14f * abs(sin(t * 8f)); p.bob = abs(sin(t * 8f)) * 3f }
            Gesture.GENTLE_SMILE -> p.headTilt = 4f
        }

        // Carrying something in the hand: bring it up in front of the body.
        val holding = gesture == Gesture.NONE && !speaking && prop in HELD_IN_HAND
        if (holding) { p.rUa = 20f; p.rFa = 90f }

        // Talking with the hands, a little.
        if (speaking && gesture == Gesture.NONE && !moving) {
            p.rUa = 18f + 8f * sin(t * 4f); p.rFa = 52f + 24f * sin(t * 5f + 1f)
            p.headNod += 2.5f * sin(t * 6f)
        }

        // Locomotion.
        if (moving && !seated) {
            val swing = (if (running) 40f else 24f) * sin(walk)
            p.thighL = swing; p.thighR = -swing
            p.shinL = swing - 16f * max(0f, -sin(walk + 0.8f)) - 2f
            p.shinR = -swing - 16f * max(0f, sin(walk + 0.8f)) - 2f
            if (gesture == Gesture.NONE) {
                // The left arm is mirrored (positive = away from the facing side), so equal angles swing the
                // arms in opposite directions, against the legs. The forearm always bends forward.
                val bend = if (running) 70f else 12f
                if (!holding) { p.rUa = swing * 0.9f; p.rFa = p.rUa + bend }
                p.lUa = swing * 0.9f; p.lFa = p.lUa - bend
            }
            p.bob += abs(sin(walk)) * (if (running) 5f else 2.5f)
            p.lean += if (running) 10f else 2f
        }
    }
}

private const val HIP_Y = -100f
private const val TORSO = 66f
private const val THIGH = 50f
private const val SHIN = 50f
private const val UPPER = 42f
private const val FORE = 40f
private const val HEAD_SCALE = 0.74f

private fun vec(deg: Float): Offset {
    val r = deg * 0.017453292f
    return Offset(sin(r), cos(r))
}

private fun shade(c: Color, f: Float): Color = lerp(c, Color.Black, f)

/**
 * Draws one figure with its feet at the local origin (negative y is up) and ~215 units tall.
 * [facing] is -1 (left) .. 1 (right); values near 0 appear mid-turn. [propPhase] animates props.
 */
fun DrawScope.drawFigure(
    look: Look, gender: Gender, age: Int, pose: FigurePose, face: Face, prop: Prop,
    facing: Float, propPhase: Float
) {
    val f = if (abs(facing) < 0.18f) (if (facing < 0f) -0.18f else 0.18f) else facing
    scale(f, 1f, pivot = Offset.Zero) {
        drawFigureLocal(look, gender, age, pose, face, prop, propPhase)
    }
}

private fun DrawScope.drawFigureLocal(look: Look, gender: Gender, age: Int, pose: FigurePose, face: Face, prop: Prop, propPhase: Float) {
    val style = outfitFor(look, age)
    val top = topColor(look, gender, age)
    val sleeve = if (style == OutfitStyle.JACKET || style == OutfitStyle.COAT) shade(top, 0.08f) else top
    val pants = look.pants
    val skin = look.skin
    val sw = if (gender == Gender.BOY) 27f else 24f
    val sit = pose.sit.coerceIn(0f, 1f)
    val hipY = HIP_Y + 48f * sit - pose.bob
    val lean = pose.lean * 0.017453292f
    val up = Offset(sin(lean), -cos(lean))
    val side = Offset(cos(lean), sin(lean))
    val hip = Offset(0f, hipY)
    val shoulderC = hip + up * TORSO + Offset(0f, pose.shoulderDrop)
    val shL = shoulderC - side * sw
    val shR = shoulderC + side * sw

    // chair when seated
    if (sit > 0.3f) {
        val c = Color(0xFF6A4630)
        drawRoundRect(c, Offset(-22f, -54f), Size(54f, 7f), CornerRadius(3f))
        drawRect(c, Offset(-24f, -102f), Size(5f, 52f))
        drawRect(shade(c, 0.3f), Offset(-20f, -47f), Size(4f, 47f)); drawRect(shade(c, 0.3f), Offset(24f, -47f), Size(4f, 47f))
    }

    // legs
    fun leg(hx: Float, thighA: Float, shinA: Float, back: Boolean) {
        val tA = thighA + sit * (88f - thighA)
        val sA = shinA + sit * (4f - shinA)
        val h = Offset(hx, hipY)
        val k = h + vec(tA) * THIGH
        val a = k + vec(sA) * SHIN
        val col = if (back) shade(pants, 0.18f) else pants
        drawLine(col, h, k, 21f, cap = StrokeCap.Round)
        drawLine(col, k, a, 18f, cap = StrokeCap.Round)
        drawOval(if (back) Color(0xFF1B1B24) else Color(0xFF14141C), Offset(a.x - 6f, a.y - 5f), Size(24f, 10f))
    }
    leg(-9f, pose.thighL, pose.shinL, true)
    leg(9f, pose.thighR, pose.shinR, false)

    // arms
    fun armPts(sh: Offset, ua: Float, fa: Float, outwardSign: Float): Pair<Offset, Offset> {
        fun d(a: Float): Offset { val v = vec(a); return Offset(v.x * outwardSign, v.y) }
        val e = sh + d(ua) * UPPER
        val h = e + d(fa) * FORE
        return e to h
    }
    val lSign = -1f + 2f * pose.lForward.coerceIn(0f, 1f)
    val (lE, lH) = armPts(shL, pose.lUa, pose.lFa, lSign)
    val (rE, rH) = armPts(shR, pose.rUa, pose.rFa, 1f)
    fun drawArm(sh: Offset, e: Offset, h: Offset, back: Boolean) {
        val col = if (back) shade(sleeve, 0.2f) else sleeve
        drawLine(col, sh, e, 15f, cap = StrokeCap.Round)
        drawLine(col, e, h, 13f, cap = StrokeCap.Round)
        drawCircle(if (back) shade(skin, 0.1f) else skin, 6.4f, h)
    }
    val armsFront = pose.armsFront > 0.5f
    if (!armsFront) drawArm(shL, lE, lH, true)

    // torso
    val torso = Path().apply {
        moveTo(shL.x, shL.y); lineTo(shR.x, shR.y)
        lineTo(hip.x + 19f, hip.y + 4f); lineTo(hip.x - 19f, hip.y + 4f); close()
    }
    drawPath(torso, Brush.verticalGradient(listOf(top, shade(top, 0.3f)), startY = shoulderC.y, endY = hip.y))
    drawCircle(top, 11.5f, Offset(shL.x, shL.y + 5f)); drawCircle(top, 11.5f, Offset(shR.x, shR.y + 5f))
    when (style) {
        OutfitStyle.JACKET -> {
            val v = Path().apply { moveTo(shoulderC.x - 10f, shoulderC.y); lineTo(hip.x, hip.y - 28f); lineTo(shoulderC.x + 10f, shoulderC.y); close() }
            drawPath(v, if (gender == Gender.BOY) Color(0xFFF2F4FA) else Color(0xFFE7DDF2))
            if (gender == Gender.BOY) drawLine(Color(0xFF2F7BFF), Offset(shoulderC.x, shoulderC.y + 6f), Offset(hip.x, hip.y - 26f), 4f)
            drawLine(shade(top, 0.4f), Offset(hip.x - 19f, hip.y + 2f), Offset(hip.x + 19f, hip.y + 2f), 3f)
        }
        OutfitStyle.COAT -> {
            drawLine(Color(0xFFC8D6E4), Offset(shoulderC.x, shoulderC.y + 6f), Offset(hip.x, hip.y + 30f), 2f)
            drawRect(Color(0xFFC8D6E4), Offset(hip.x + 6f, hip.y - 24f), Size(9f, 10f), style = Stroke(1.5f))
        }
        OutfitStyle.HOODIE -> {
            drawLine(look.topAccent, Offset(shoulderC.x - 4f, shoulderC.y + 8f), Offset(shoulderC.x - 5f, shoulderC.y + 28f), 2f, alpha = 0.8f)
            drawLine(look.topAccent, Offset(shoulderC.x + 4f, shoulderC.y + 8f), Offset(shoulderC.x + 5f, shoulderC.y + 28f), 2f, alpha = 0.8f)
            drawRect(shade(top, 0.25f), Offset(hip.x - 12f, hip.y - 20f), Size(24f, 12f))
        }
        OutfitStyle.SHIRT -> drawLine(shade(top, 0.3f), Offset(shoulderC.x, shoulderC.y + 6f), Offset(hip.x, hip.y), 1.5f)
    }

    // head (back hair, neck, face, front hair)
    val headScale = HEAD_SCALE
    val neck = shoulderC + up * 6f + Offset(0f, pose.headNod * 0.25f)
    // A solid neck from the shoulder line to the chin, so the head never floats above the body.
    drawRoundRect(shade(skin, 0.12f), Offset(neck.x - 6.5f, neck.y - 4f), Size(13f, shoulderC.y - neck.y + 14f), CornerRadius(4f))
    if (style == OutfitStyle.HOODIE) drawOval(shade(top, 0.3f), Offset(shoulderC.x - 18f, shoulderC.y - 4f), Size(36f, 14f))
    translate(neck.x, neck.y) {
        rotate(pose.headTilt + pose.lean + pose.headNod * 0.15f, Offset.Zero) {
            translate(-50f * headScale, -78f * headScale) {
                drawHairBack(look, age, headScale, headScale)
                drawNeck(look, headScale, headScale)
                drawHeadFront(look, gender, age, face, headScale, headScale)
            }
        }
    }

    if (armsFront) drawArm(shL, lE, lH, false)
    drawArm(shR, rE, rH, false)

    drawProp(prop, rH, lH, hip, shL, propPhase, skin)
}

private fun DrawScope.drawProp(prop: Prop, rHand: Offset, lHand: Offset, hip: Offset, shL: Offset, phase: Float, skin: Color) {
    when (prop) {
        Prop.NONE -> {}
        Prop.PHONE -> {
            drawRoundRect(Color(0xFF14182A), Offset(rHand.x - 4f, rHand.y - 12f), Size(9f, 16f), CornerRadius(2f))
            drawRect(Color(0xFF7FB6FF), Offset(rHand.x - 3f, rHand.y - 10f), Size(6f, 11f), alpha = 0.9f)
        }
        Prop.LAPTOP -> {
            val y = hip.y - 22f
            drawRoundRect(Color(0xFF9AA4B8), Offset(rHand.x - 4f, y), Size(34f, 4f), CornerRadius(1.5f))
            drawRoundRect(Color(0xFF2A3350), Offset(rHand.x + 12f, y - 24f), Size(20f, 24f), CornerRadius(2f))
            drawRect(Color(0xFF8CC4FF), Offset(rHand.x + 14f, y - 22f), Size(16f, 20f), alpha = 0.7f)
        }
        Prop.CUP -> {
            drawRoundRect(Color(0xFFEFE6D8), Offset(rHand.x - 5f, rHand.y - 10f), Size(10f, 11f), CornerRadius(2f))
            drawArc(Color(0xFFEFE6D8), -90f, 180f, false, Offset(rHand.x + 3f, rHand.y - 8f), Size(6f, 7f), style = Stroke(1.6f))
        }
        Prop.BAG -> {
            drawLine(Color(0xFF3A2A22), Offset(shL.x + 4f, shL.y), Offset(hip.x - 30f, hip.y + 8f), 3f)
            drawRoundRect(Color(0xFF6A4630), Offset(hip.x - 42f, hip.y + 6f), Size(26f, 22f), CornerRadius(4f))
            drawRect(Color(0xFF3A2A22), Offset(hip.x - 42f, hip.y + 12f), Size(26f, 2.5f))
        }
        Prop.BOOK -> {
            drawRoundRect(Color(0xFF2F5FAE), Offset(rHand.x - 10f, rHand.y - 8f), Size(20f, 14f), CornerRadius(2f))
            drawRect(Color(0xFFF2E8D8), Offset(rHand.x - 8f, rHand.y - 6f), Size(16f, 2f))
        }
        Prop.DOCUMENTS -> {
            drawRect(Color(0xFFF2F4FA), Offset(rHand.x - 9f, rHand.y - 12f), Size(18f, 22f))
            for (i in 0..3) drawLine(Color(0xFF9AA4B8), Offset(rHand.x - 6f, rHand.y - 8f + i * 4.5f), Offset(rHand.x + 6f, rHand.y - 8f + i * 4.5f), 1f)
        }
        Prop.BALL -> {
            val y = rHand.y + 12f + 14f * abs(sin(phase * 6f))
            drawCircle(Color(0xFFE5732B), 9f, Offset(rHand.x + 4f, y))
            drawLine(Color(0xFF3A1A0A), Offset(rHand.x - 5f, y), Offset(rHand.x + 13f, y), 1.2f)
            drawLine(Color(0xFF3A1A0A), Offset(rHand.x + 4f, y - 9f), Offset(rHand.x + 4f, y + 9f), 1.2f)
        }
        Prop.SUITCASE -> {
            drawRoundRect(Color(0xFF3F5A9A), Offset(hip.x + 30f, -34f), Size(26f, 34f), CornerRadius(4f))
            drawLine(Color(0xFFC8D0E4), Offset(hip.x + 43f, -34f), Offset(hip.x + 43f, -46f), 2f)
            drawLine(Color(0xFFC8D0E4), Offset(hip.x + 36f, -46f), Offset(hip.x + 50f, -46f), 2.5f)
            drawCircle(Color(0xFF14141C), 3f, Offset(hip.x + 35f, 1f)); drawCircle(Color(0xFF14141C), 3f, Offset(hip.x + 51f, 1f))
        }
        Prop.FLOWERS -> {
            drawLine(Color(0xFF3F9A5A), Offset(rHand.x, rHand.y), Offset(rHand.x - 2f, rHand.y - 16f), 2f)
            for (i in 0..2) drawCircle(listOf(Color(0xFFFF8FB0), Color.White, Color(0xFFFFC2D6))[i], 5f, Offset(rHand.x - 8f + i * 6f, rHand.y - 18f - (i % 2) * 3f))
        }
        Prop.BOX -> {
            drawRoundRect(Color(0xFFB88A5A), Offset(rHand.x - 14f, rHand.y - 12f), Size(30f, 24f), CornerRadius(2f))
            drawLine(Color(0xFF8A6238), Offset(rHand.x - 14f, rHand.y - 2f), Offset(rHand.x + 16f, rHand.y - 2f), 2f)
        }
    }
}

/** Soft ground shadow under a figure; drawn in stage coordinates before the figure. */
fun DrawScope.drawFigureShadow(cx: Float, groundY: Float, width: Float) {
    drawOval(Color(0x55000000), Offset(cx - width / 2f, groundY - width * 0.07f), Size(width, width * 0.14f))
}
