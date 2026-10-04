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
import androidx.compose.ui.graphics.lerp
import com.lifeyourchoice.core.cinema.Emotion
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.PlayerLook

/**
 * A stylised bust used in menus, records and the Life Card. Its look comes from the player's
 * character-creation choices ([look]) or, for Version 1 lives, from the preset [appearance].
 * The look changes with life stage: hoodie as a teenager, jacket as an adult, greying hair, glasses.
 */
@Composable
fun CharacterPortrait(
    gender: Gender,
    appearance: Int,
    ageYears: Int,
    modifier: Modifier = Modifier,
    happiness: Int = 65,
    look: PlayerLook? = null,
    emotion: Emotion = Emotion.NEUTRAL
) {
    val resolved = if (look != null) Looks.forPlayer(gender, look) else Looks.of(gender, appearance)
    val face = Face(emotion = emotion, smileBias = ((happiness - 40) / 60f).coerceIn(-0.4f, 1f) * 0.9f)
    Canvas(modifier) { drawBust(resolved, gender, ageYears, face, size.width / 100f, size.height / 120f) }
}

/** Bust of any [Look] (non-player characters in previews). */
@Composable
fun LookPortrait(look: Look, gender: Gender, ageYears: Int, modifier: Modifier = Modifier, face: Face = Face()) {
    Canvas(modifier) { drawBust(look, gender, ageYears, face, size.width / 100f, size.height / 120f) }
}

private fun shade(c: Color, f: Float): Color = lerp(c, Color.Black, f)

internal fun outfitFor(look: Look, age: Int): OutfitStyle = look.outfit ?: if (age >= 19) OutfitStyle.JACKET else OutfitStyle.HOODIE

internal fun topColor(look: Look, gender: Gender, age: Int): Color =
    if (look.outfit == null && age >= 19) (if (gender == Gender.BOY) Color(0xFF1B2745) else Color(0xFF2B2F52)) else look.top

fun DrawScope.drawBust(look: Look, gender: Gender, age: Int, face: Face, sx: Float, sy: Float) {
    fun X(v: Float) = v * sx
    fun Y(v: Float) = v * sy
    val style = outfitFor(look, age)
    val top = topColor(look, gender, age)

    drawHairBack(look, age, sx, sy)

    val torso = Path().apply {
        moveTo(X(4f), Y(121f))
        cubicTo(X(6f), Y(98f), X(18f), Y(90f), X(36f), Y(85f))
        lineTo(X(64f), Y(85f))
        cubicTo(X(82f), Y(90f), X(94f), Y(98f), X(96f), Y(121f))
        close()
    }
    drawPath(torso, Brush.verticalGradient(listOf(top, shade(top, 0.35f)), startY = Y(85f), endY = Y(121f)))
    drawNeck(look, sx, sy)

    when (style) {
        OutfitStyle.HOODIE -> {
            val hood = Path().apply {
                moveTo(X(30f), Y(88f)); cubicTo(X(34f), Y(78f), X(66f), Y(78f), X(70f), Y(88f))
                cubicTo(X(60f), Y(96f), X(40f), Y(96f), X(30f), Y(88f)); close()
            }
            drawPath(hood, shade(top, 0.25f))
            drawLine(look.topAccent, Offset(X(43f), Y(92f)), Offset(X(42f), Y(108f)), X(1.6f), alpha = 0.9f)
            drawLine(look.topAccent, Offset(X(57f), Y(92f)), Offset(X(58f), Y(108f)), X(1.6f), alpha = 0.9f)
            drawRect(look.topAccent, Offset(X(33f), Y(108f)), Size(X(34f), Y(1.6f)), alpha = 0.35f)
        }
        OutfitStyle.SHIRT -> {
            val collar = Path().apply { moveTo(X(38f), Y(85f)); lineTo(X(50f), Y(96f)); lineTo(X(62f), Y(85f)); lineTo(X(57f), Y(84f)); lineTo(X(50f), Y(90f)); lineTo(X(43f), Y(84f)); close() }
            drawPath(collar, Color(0xFFF2F4FA))
            drawLine(shade(top, 0.3f), Offset(X(50f), Y(96f)), Offset(X(50f), Y(121f)), X(1.2f))
        }
        OutfitStyle.COAT -> {
            val collar = Path().apply { moveTo(X(36f), Y(85f)); lineTo(X(50f), Y(108f)); lineTo(X(64f), Y(85f)); lineTo(X(58f), Y(84f)); lineTo(X(50f), Y(96f)); lineTo(X(42f), Y(84f)); close() }
            drawPath(collar, Color(0xFFDDE7F0))
            drawLine(look.topAccent, Offset(X(70f), Y(100f)), Offset(X(70f), Y(112f)), X(1.4f))
        }
        OutfitStyle.JACKET -> {
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
    }
    drawHeadFront(look, gender, age, face, sx, sy)
}
