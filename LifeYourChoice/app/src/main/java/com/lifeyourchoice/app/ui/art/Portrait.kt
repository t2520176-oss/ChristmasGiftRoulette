package com.lifeyourchoice.app.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.lifeyourchoice.core.model.Gender
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Look of one preset character. Everything is drawn in code; there are no image assets. */
class Look(
    val name: String,
    val skin: Color,
    val hair: Color,
    val style: HairStyle,
    val top: Color,
    val topAccent: Color
)

enum class HairStyle { SHORT_MESSY, SWEPT, CURLY_SHORT, LONG_WAVY, PONYTAIL, BOB }

object Looks {
    val boys = listOf(
        Look("Classic", Color(0xFFF0C8A0), Color(0xFF1A1420), HairStyle.SHORT_MESSY, Color(0xFF263050), Color(0xFFDCE6FF)),
        Look("Sporty", Color(0xFFD9A074), Color(0xFF4A2E1B), HairStyle.SWEPT, Color(0xFF1F5A99), Color(0xFFFFFFFF)),
        Look("Curly", Color(0xFF8A5A3C), Color(0xFF0F0B0A), HairStyle.CURLY_SHORT, Color(0xFF8A2F2F), Color(0xFFFFD2C2))
    )
    val girls = listOf(
        Look("Classic", Color(0xFFF3CFB0), Color(0xFF3A1F14), HairStyle.LONG_WAVY, Color(0xFF9AA0B8), Color(0xFFFFFFFF)),
        Look("Sporty", Color(0xFFE0AE86), Color(0xFF16121C), HairStyle.PONYTAIL, Color(0xFF2F6E5B), Color(0xFFD9FFF0)),
        Look("Creative", Color(0xFF9A6644), Color(0xFF7A3F1D), HairStyle.BOB, Color(0xFF6C3A8A), Color(0xFFF1D9FF))
    )

    fun of(gender: Gender, appearance: Int): Look {
        val list = if (gender == Gender.BOY) boys else girls
        return list[appearance.coerceIn(0, list.size - 1)]
    }
}

/**
 * A stylised bust. The look changes with life stage: hoodie as a teenager, jacket as an adult,
 * greying hair from the forties and glasses in later life. [happiness] bends the smile.
 */
@Composable
fun CharacterPortrait(
    gender: Gender,
    appearance: Int,
    ageYears: Int,
    modifier: Modifier = Modifier,
    happiness: Int = 65
) {
    val look = Looks.of(gender, appearance)
    Canvas(modifier) { drawPortrait(look, gender, ageYears, happiness) }
}

private fun shade(c: Color, f: Float): Color = lerp(c, Color.Black, f)

private fun DrawScope.drawPortrait(look: Look, gender: Gender, age: Int, happiness: Int) {
    // Work in a 100 x 120 design space.
    val sx = size.width / 100f
    val sy = size.height / 120f
    fun X(v: Float) = v * sx
    fun Y(v: Float) = v * sy
    fun P(x: Float, y: Float) = Offset(X(x), Y(y))

    val grey = Color(0xFFC3C7D4)
    val greyFactor = ((age - 38) / 32f).coerceIn(0f, 1f) * 0.85f
    val hair = lerp(look.hair, grey, greyFactor)
    val skin = look.skin
    val skinShade = shade(skin, 0.12f)
    val adult = age >= 19
    val top = if (adult) {
        if (gender == Gender.BOY) Color(0xFF1B2745) else Color(0xFF2B2F52)
    } else look.top

    // ---- hair behind the head (long styles)
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

    // ---- torso
    val torso = Path().apply {
        moveTo(X(4f), Y(121f))
        cubicTo(X(6f), Y(98f), X(18f), Y(90f), X(36f), Y(85f))
        lineTo(X(64f), Y(85f))
        cubicTo(X(82f), Y(90f), X(94f), Y(98f), X(96f), Y(121f))
        close()
    }
    drawPath(torso, Brush.verticalGradient(listOf(top, shade(top, 0.35f)), startY = Y(85f), endY = Y(121f)))

    // ---- neck
    drawRect(skinShade, P(41f, 70f), Size(X(18f), Y(18f)))
    drawOval(shade(skin, 0.22f), P(41f, 80f), Size(X(18f), Y(8f)), alpha = 0.55f)

    // ---- outfit details
    if (!adult) {
        // hoodie collar and strings
        val hood = Path().apply {
            moveTo(X(30f), Y(88f))
            cubicTo(X(34f), Y(78f), X(66f), Y(78f), X(70f), Y(88f))
            cubicTo(X(60f), Y(96f), X(40f), Y(96f), X(30f), Y(88f))
            close()
        }
        drawPath(hood, shade(top, 0.25f))
        drawLine(look.topAccent, P(43f, 92f), P(42f, 108f), X(1.6f), alpha = 0.9f)
        drawLine(look.topAccent, P(57f, 92f), P(58f, 108f), X(1.6f), alpha = 0.9f)
        drawRect(look.topAccent, P(33f, 108f), Size(X(34f), Y(1.6f)), alpha = 0.35f)
    } else {
        // jacket lapels with a collar
        val collar = Path().apply {
            moveTo(X(36f), Y(85f)); lineTo(X(50f), Y(104f)); lineTo(X(64f), Y(85f))
            lineTo(X(58f), Y(84f)); lineTo(X(50f), Y(94f)); lineTo(X(42f), Y(84f)); close()
        }
        drawPath(collar, if (gender == Gender.BOY) Color(0xFFF2F4FA) else Color(0xFFE7DDF2))
        val l1 = Path().apply { moveTo(X(36f), Y(85f)); lineTo(X(50f), Y(112f)); lineTo(X(34f), Y(121f)); lineTo(X(24f), Y(100f)); close() }
        val l2 = Path().apply { moveTo(X(64f), Y(85f)); lineTo(X(50f), Y(112f)); lineTo(X(66f), Y(121f)); lineTo(X(76f), Y(100f)); close() }
        drawPath(l1, shade(top, 0.2f)); drawPath(l2, shade(top, 0.2f))
        if (gender == Gender.BOY) {
            val tie = Path().apply { moveTo(X(48f), Y(94f)); lineTo(X(52f), Y(94f)); lineTo(X(54f), Y(114f)); lineTo(X(50f), Y(118f)); lineTo(X(46f), Y(114f)); close() }
            drawPath(tie, Color(0xFF2F7BFF))
        }
    }

    // ---- ears and head
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

    // ---- face
    val eyeY = 56f
    for (dx in listOf(-9f, 9f)) {
        val cx = 50f + dx
        drawOval(Color.White, P(cx - 4.4f, eyeY - 2.8f), Size(X(8.8f), Y(5.6f)))
        drawCircle(Color(0xFF2A1B14), X(2.3f), P(cx + (if (dx < 0) 0.4f else -0.4f), eyeY))
        drawCircle(Color.White, X(0.8f), P(cx + 0.6f, eyeY - 0.9f))
        // lashes / lid line
        drawArc(shade(skin, 0.55f), 200f, 140f, false, P(cx - 4.6f, eyeY - 3.2f), Size(X(9.2f), Y(6f)), style = Stroke(X(0.9f)))
    }
    val brow = shade(hair, 0.05f)
    val browW = if (gender == Gender.BOY) 1.9f else 1.4f
    drawLine(brow, P(37f, 49.5f), P(46f, 48.5f), X(browW))
    drawLine(brow, P(54f, 48.5f), P(63f, 49.5f), X(browW))
    // nose
    val nose = Path().apply { moveTo(X(50f), Y(57f)); quadraticBezierTo(X(52.5f), Y(64f), X(50f), Y(65f)) }
    drawPath(nose, shade(skin, 0.28f), style = Stroke(X(0.9f)))
    // mouth: smile follows happiness
    val smile = ((happiness - 40) / 60f).coerceIn(-0.4f, 1f)
    val mouth = Path().apply {
        moveTo(X(43.5f), Y(70f)); quadraticBezierTo(X(50f), Y(70f + 5f * smile + 1f), X(56.5f), Y(70f))
    }
    drawPath(mouth, Color(0xFF8A3A3A), style = Stroke(X(1.3f)))
    if (gender == Gender.GIRL) {
        drawCircle(Color(0xFFFF8FA3), X(4f), P(37f, 64f), alpha = 0.18f)
        drawCircle(Color(0xFFFF8FA3), X(4f), P(63f, 64f), alpha = 0.18f)
    }

    // ---- hair in front
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
            HairStyle.SWEPT -> {
                quadraticBezierTo(X(52f), Y(28f), X(34f), Y(44f)); lineTo(X(31f), Y(40f))
            }
            HairStyle.CURLY_SHORT -> {
                lineTo(X(64f), Y(38f)); lineTo(X(36f), Y(38f)); lineTo(X(31f), Y(42f))
            }
            HairStyle.LONG_WAVY -> {
                quadraticBezierTo(X(56f), Y(30f), X(50f), Y(36f)); quadraticBezierTo(X(40f), Y(30f), X(32f), Y(44f))
            }
            HairStyle.PONYTAIL -> {
                quadraticBezierTo(X(50f), Y(32f), X(30f), Y(46f))
            }
            HairStyle.BOB -> {
                quadraticBezierTo(X(60f), Y(32f), X(50f), Y(34f)); quadraticBezierTo(X(38f), Y(32f), X(32f), Y(44f))
            }
        }
        close()
    }
    drawPath(cap, Brush.verticalGradient(listOf(lerp(hair, Color.White, 0.14f), hair), startY = Y(14f), endY = Y(50f)))
    when (look.style) {
        HairStyle.CURLY_SHORT -> {
            for (i in 0..8) {
                val a = PI.toFloat() * (1.05f + 0.9f * i / 8f)
                drawCircle(hair, X(6.2f), P(50f + 25f * cos(a), 40f + 24f * sin(a)))
            }
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
        HairStyle.PONYTAIL -> {
            drawCircle(Color(0xFFFFC83D), X(2.4f), P(72f, 44f))
        }
        else -> {}
    }
    // highlight sheen on hair
    drawArc(Color.White.copy(alpha = 0.10f), 200f, 70f, false, P(36f, 20f), Size(X(30f), Y(22f)), style = Stroke(X(2.2f)))

    // ---- glasses in later life
    if (age >= 58) {
        val frame = Color(0xFF2B2B34)
        for (dx in listOf(-9f, 9f)) {
            drawRoundRect(frame, P(50f + dx - 6.4f, eyeY - 4.6f), Size(X(12.8f), Y(9.2f)),
                androidx.compose.ui.geometry.CornerRadius(X(3.2f)), style = Stroke(X(0.9f)))
        }
        drawLine(frame, P(46.2f, eyeY - 1f), P(53.8f, eyeY - 1f), X(0.9f))
    }
}
