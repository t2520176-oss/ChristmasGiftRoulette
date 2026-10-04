package com.lifeyourchoice.app.ui.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.lifeyourchoice.core.cinema.Emotion
import com.lifeyourchoice.core.model.Gender
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * What the face is doing this frame. The same head renderer serves portraits and full-body figures.
 * [yaw] (-1..1) shifts the features so the head reads as turned; [lookX]/[lookY] move the pupils.
 */
class Face(
    val emotion: Emotion = Emotion.NEUTRAL,
    val mouthOpen: Float = 0f,
    val blink: Float = 0f,
    val lookX: Float = 0f,
    val lookY: Float = 0f,
    val yaw: Float = 0f,
    /** Added to the mouth curve (used by portraits to follow the Happiness stat). */
    val smileBias: Float = 0f
)

/** Facial parameters per emotion: eyebrows, eyes, mouth and small extras. */
private class Expr(
    val slant: Float,       // + inner brow raised (worried/sad), - inner brow lowered (angry)
    val lift: Float,        // brows up
    val eyeOpen: Float,
    val curve: Float,       // + smile, - frown
    val open: Float,        // resting mouth opening
    val blush: Float = 0f,
    val tear: Boolean = false,
    val sweat: Boolean = false,
    val rage: Float = 0f,
    val lookY: Float = 0f,
    val bags: Float = 0f
)

private fun expr(e: Emotion): Expr = when (e) {
    Emotion.NEUTRAL -> Expr(0f, 0f, 1f, 0.15f, 0f)
    Emotion.HAPPY -> Expr(0.1f, 0.1f, 0.9f, 0.9f, 0f)
    Emotion.LAUGHING -> Expr(0.25f, 0.2f, 0.35f, 1f, 0.7f, blush = 0.4f)
    Emotion.CONFIDENT -> Expr(-0.2f, 0.1f, 0.95f, 0.55f, 0f)
    Emotion.WORRIED -> Expr(0.8f, 0.3f, 1.1f, -0.25f, 0f, sweat = true)
    Emotion.SAD -> Expr(0.9f, -0.1f, 0.8f, -0.9f, 0f, tear = true)
    Emotion.ANGRY -> Expr(-1f, -0.2f, 0.85f, -0.6f, 0.15f, rage = 1f)
    Emotion.SURPRISED -> Expr(0.3f, 0.9f, 1.4f, 0f, 0.7f)
    Emotion.EMBARRASSED -> Expr(0.5f, 0.1f, 0.8f, 0.25f, 0f, blush = 1f, lookY = 0.5f)
    Emotion.DISAPPOINTED -> Expr(0.5f, -0.05f, 0.7f, -0.55f, 0f)
    Emotion.PROUD -> Expr(0f, 0.35f, 0.9f, 0.75f, 0f)
    Emotion.AFRAID -> Expr(1f, 0.6f, 1.35f, -0.4f, 0.35f, sweat = true)
    Emotion.THOUGHTFUL -> Expr(0.35f, 0.25f, 0.9f, 0f, 0f, lookY = -0.8f)
    Emotion.TIRED -> Expr(0.35f, -0.2f, 0.45f, -0.2f, 0f, bags = 1f)
    Emotion.EXCITED -> Expr(0.2f, 0.5f, 1.25f, 1f, 0.45f, blush = 0.3f)
}

private fun shade(c: Color, f: Float): Color = lerp(c, Color.Black, f)

private fun greyed(look: Look, age: Int): Color {
    val f = ((age - 38) / 32f).coerceIn(0f, 1f) * 0.85f
    return lerp(look.hair, Color(0xFFC3C7D4), f)
}

/** Hair that sits behind the shoulders (long hair, ponytail). Design space: 100 x 120. */
fun DrawScope.drawHairBack(look: Look, age: Int, sx: Float, sy: Float) {
    fun X(v: Float) = v * sx
    fun Y(v: Float) = v * sy
    val hair = greyed(look, age)
    when (look.style) {
        HairStyle.LONG_WAVY -> {
            val p = Path().apply {
                moveTo(X(26f), Y(46f))
                cubicTo(X(10f), Y(60f), X(14f), Y(98f), X(24f), Y(112f))
                lineTo(X(76f), Y(112f))
                cubicTo(X(86f), Y(98f), X(90f), Y(60f), X(74f), Y(46f))
                close()
            }
            drawPath(p, shade(hair, 0.1f))
        }
        HairStyle.PONYTAIL -> {
            val p = Path().apply {
                moveTo(X(70f), Y(40f))
                cubicTo(X(98f), Y(40f), X(96f), Y(88f), X(80f), Y(96f))
                cubicTo(X(86f), Y(72f), X(80f), Y(58f), X(68f), Y(54f))
                close()
            }
            drawPath(p, shade(hair, 0.1f))
        }
        else -> {}
    }
}

fun DrawScope.drawNeck(look: Look, sx: Float, sy: Float) {
    val skinShade = shade(look.skin, 0.12f)
    drawRect(skinShade, Offset(41f * sx, 70f * sy), Size(18f * sx, 18f * sy))
    drawOval(shade(look.skin, 0.22f), Offset(41f * sx, 80f * sy), Size(18f * sx, 8f * sy), alpha = 0.55f)
}

/** Ears, head, face with expression, front hair and glasses. */
fun DrawScope.drawHeadFront(look: Look, gender: Gender, age: Int, face: Face, sx: Float, sy: Float) {
    fun X(v: Float) = v * sx
    fun Y(v: Float) = v * sy
    fun P(x: Float, y: Float) = Offset(X(x), Y(y))
    val skin = look.skin
    val skinShade = shade(skin, 0.12f)
    val hair = greyed(look, age)
    val e = expr(face.emotion)
    val fx = face.yaw * 3f

    // ears and head
    drawCircle(skinShade, X(4.2f), P(28.5f, 56f)); drawCircle(skinShade, X(4.2f), P(71.5f, 56f))
    val head = Path().apply {
        moveTo(X(50f), Y(26f))
        cubicTo(X(72f), Y(26f), X(73f), Y(50f), X(70f), Y(60f))
        cubicTo(X(66f), Y(74f), X(58f), Y(80f), X(50f), Y(80f))
        cubicTo(X(42f), Y(80f), X(34f), Y(74f), X(30f), Y(60f))
        cubicTo(X(27f), Y(50f), X(28f), Y(26f), X(50f), Y(26f))
        close()
    }
    drawPath(head, Brush.horizontalGradient(listOf(shade(skin, 0.08f), skin, skin), startX = X(28f), endX = X(72f)))
    if (e.rage > 0f) drawOval(Color(0xFFFF4B4B), P(32f, 40f), Size(X(36f), Y(18f)), alpha = 0.14f * e.rage)

    // eyes
    val eyeY = 56f
    val open = (e.eyeOpen * (1f - face.blink)).coerceAtLeast(0.08f)
    for (dx in listOf(-9f, 9f)) {
        val cx = 50f + dx + fx
        val h = 5.6f * open
        drawOval(Color.White, P(cx - 4.4f, eyeY - h / 2f), Size(X(8.8f), Y(h)))
        if (open > 0.3f) {
            val ix = cx + face.lookX * 1.7f + (if (dx < 0) 0.4f else -0.4f)
            val iy = eyeY + (face.lookY + e.lookY) * 1.2f
            drawCircle(Color(0xFF2A1B14), X(2.3f * minOf(1f, open + 0.3f)), P(ix, iy))
            drawCircle(Color.White, X(0.8f), P(ix + 0.6f, iy - 0.9f))
        }
        drawArc(shade(skin, 0.55f), 200f, 140f, false, P(cx - 4.6f, eyeY - h / 2f - 0.6f), Size(X(9.2f), Y(maxOf(h, 2.4f))), style = Stroke(X(0.9f)))
        if (e.bags > 0f) drawArc(shade(skin, 0.4f), 20f, 140f, false, P(cx - 4.2f, eyeY + 1.5f), Size(X(8.4f), Y(4f)), alpha = 0.35f * e.bags, style = Stroke(X(0.9f)))
    }
    // brows
    val brow = shade(hair, 0.05f)
    val bw = if (gender == Gender.BOY) 1.9f else 1.4f
    val baseY = 49f - e.lift * 3f
    val innerY = baseY - e.slant * 3f
    val outerY = baseY + e.slant * 1.6f
    drawLine(brow, P(37f + fx, outerY), P(46f + fx, innerY), X(bw))
    drawLine(brow, P(54f + fx, innerY), P(63f + fx, outerY), X(bw))
    // nose
    val nose = Path().apply { moveTo(X(50f + fx), Y(57f)); quadraticBezierTo(X(52.5f + fx), Y(64f), X(50f + fx), Y(65f)) }
    drawPath(nose, shade(skin, 0.28f), style = Stroke(X(0.9f)))
    // mouth
    val curve = (e.curve + face.smileBias).coerceIn(-1f, 1.1f)
    val openAmt = maxOf(e.open, face.mouthOpen).coerceIn(0f, 1f)
    if (openAmt > 0.08f) {
        drawOval(Color(0xFF5A1F28), P(46f + fx, 68.5f), Size(X(8f), Y(1.6f + 6.5f * openAmt)))
        drawArc(Color(0xFFF4EDE6), 180f, 180f, false, P(47f + fx, 68.7f), Size(X(6f), Y(2.4f)), style = Stroke(X(0.8f)))
    } else {
        val mouth = Path().apply { moveTo(X(43.5f + fx), Y(70f)); quadraticBezierTo(X(50f + fx), Y(70f + 5f * curve + 1f), X(56.5f + fx), Y(70f)) }
        drawPath(mouth, Color(0xFF8A3A3A), style = Stroke(X(1.3f)))
    }
    val blush = (if (gender == Gender.GIRL) 0.18f else 0f) + e.blush * 0.28f
    if (blush > 0f) {
        drawCircle(Color(0xFFFF7F95), X(4.2f), P(37f + fx, 64f), alpha = blush)
        drawCircle(Color(0xFFFF7F95), X(4.2f), P(63f + fx, 64f), alpha = blush)
    }
    if (e.tear) drawOval(Color(0xFF9FD4FF), P(40f + fx, 61f), Size(X(2f), Y(3.4f)), alpha = 0.85f)
    if (e.sweat) drawOval(Color(0xFF9FD4FF), P(66f, 40f), Size(X(2.4f), Y(4f)), alpha = 0.8f)

    // front hair
    val cap = Path().apply {
        moveTo(X(27f), Y(54f))
        cubicTo(X(22f), Y(30f), X(36f), Y(16f), X(52f), Y(16f))
        cubicTo(X(68f), Y(16f), X(80f), Y(30f), X(73f), Y(54f))
        lineTo(X(70f), Y(46f))
        when (look.style) {
            HairStyle.SHORT_MESSY -> {
                lineTo(X(66f), Y(36f)); lineTo(X(61f), Y(44f)); lineTo(X(55f), Y(33f)); lineTo(X(48f), Y(43f))
                lineTo(X(41f), Y(34f)); lineTo(X(36f), Y(43f)); lineTo(X(31f), Y(38f))
            }
            HairStyle.SWEPT -> { quadraticBezierTo(X(52f), Y(28f), X(34f), Y(44f)); lineTo(X(31f), Y(40f)) }
            HairStyle.CURLY_SHORT -> { lineTo(X(64f), Y(38f)); lineTo(X(36f), Y(38f)); lineTo(X(31f), Y(42f)) }
            HairStyle.LONG_WAVY -> { quadraticBezierTo(X(56f), Y(30f), X(50f), Y(36f)); quadraticBezierTo(X(40f), Y(30f), X(32f), Y(44f)) }
            HairStyle.PONYTAIL -> { quadraticBezierTo(X(50f), Y(32f), X(30f), Y(46f)) }
            HairStyle.BOB -> { quadraticBezierTo(X(60f), Y(32f), X(50f), Y(34f)); quadraticBezierTo(X(38f), Y(32f), X(32f), Y(44f)) }
        }
        close()
    }
    drawPath(cap, Brush.verticalGradient(listOf(lerp(hair, Color.White, 0.14f), hair), startY = Y(14f), endY = Y(50f)))
    when (look.style) {
        HairStyle.CURLY_SHORT -> for (i in 0..8) {
            val a = PI.toFloat() * (1.05f + 0.9f * i / 8f)
            drawCircle(hair, X(6.2f), P(50f + 25f * cos(a), 40f + 24f * sin(a)))
        }
        HairStyle.LONG_WAVY -> {
            val left = Path().apply { moveTo(X(27f), Y(50f)); cubicTo(X(16f), Y(66f), X(22f), Y(92f), X(18f), Y(108f)); lineTo(X(30f), Y(108f)); cubicTo(X(30f), Y(88f), X(30f), Y(66f), X(33f), Y(52f)); close() }
            val right = Path().apply { moveTo(X(73f), Y(50f)); cubicTo(X(84f), Y(66f), X(78f), Y(92f), X(82f), Y(108f)); lineTo(X(70f), Y(108f)); cubicTo(X(70f), Y(88f), X(70f), Y(66f), X(67f), Y(52f)); close() }
            drawPath(left, hair); drawPath(right, hair)
        }
        HairStyle.BOB -> {
            val left = Path().apply { moveTo(X(27f), Y(50f)); cubicTo(X(18f), Y(62f), X(22f), Y(78f), X(30f), Y(82f)); lineTo(X(34f), Y(70f)); lineTo(X(33f), Y(52f)); close() }
            val right = Path().apply { moveTo(X(73f), Y(50f)); cubicTo(X(82f), Y(62f), X(78f), Y(78f), X(70f), Y(82f)); lineTo(X(66f), Y(70f)); lineTo(X(67f), Y(52f)); close() }
            drawPath(left, hair); drawPath(right, hair)
        }
        HairStyle.PONYTAIL -> drawCircle(Color(0xFFFFC83D), X(2.4f), P(72f, 44f))
        else -> {}
    }
    drawArc(Color.White.copy(alpha = 0.10f), 200f, 70f, false, P(36f, 20f), Size(X(30f), Y(22f)), style = Stroke(X(2.2f)))

    if (age >= 58) {
        val frame = Color(0xFF2B2B34)
        for (dx in listOf(-9f, 9f)) {
            drawRoundRect(frame, P(50f + dx - 6.4f + fx, eyeY - 4.6f), Size(X(12.8f), Y(9.2f)), CornerRadius(X(3.2f)), style = Stroke(X(0.9f)))
        }
        drawLine(frame, P(46.2f + fx, eyeY - 1f), P(53.8f + fx, eyeY - 1f), X(0.9f))
    }
}

internal fun maxF(a: Float, b: Float) = max(a, b)
