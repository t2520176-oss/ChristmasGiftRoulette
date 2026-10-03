package com.example.christmasgiftroulette.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.christmasgiftroulette.model.GiftIconType
import com.example.christmasgiftroulette.ui.theme.ChristmasColors
import kotlin.math.min

/** Original gift-box icon (decorative). */
@Composable
fun GiftIcon(modifier: Modifier = Modifier) = GiftIconView(GiftIconType.GIFT, modifier)

/** Draws the icon for [type] on a Canvas. Decorative: callers describe the gift in text. */
@Composable
fun GiftIconView(type: GiftIconType, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawGiftIcon(type, Offset.Zero, min(size.width, size.height)) }
}

/** Draws [type] inside the square with top-left [topLeft] and side [s]. Also used on the wheel. */
fun DrawScope.drawGiftIcon(type: GiftIconType, topLeft: Offset, s: Float) {
    fun o(fx: Float, fy: Float) = Offset(topLeft.x + s * fx, topLeft.y + s * fy)
    fun sz(fw: Float, fh: Float) = Size(s * fw, s * fh)
    fun r(f: Float) = CornerRadius(s * f)
    val red = ChristmasColors.Red
    val gold = ChristmasColors.Gold

    when (type) {
        GiftIconType.GIFT -> {
            drawRoundRect(red, o(0.10f, 0.42f), sz(0.80f, 0.52f), r(0.06f))
            drawRoundRect(ChristmasColors.RedBright, o(0.04f, 0.30f), sz(0.92f, 0.18f), r(0.05f))
            drawRect(gold, o(0.44f, 0.30f), sz(0.12f, 0.64f))
            drawOval(gold, o(0.20f, 0.08f), sz(0.30f, 0.24f))
            drawOval(gold, o(0.50f, 0.08f), sz(0.30f, 0.24f))
            drawCircle(Color(0xFFFFE08A), s * 0.075f, o(0.50f, 0.30f))
        }
        GiftIconType.CASH -> {
            drawRoundRect(Color(0xFF2E7D32), o(0.03f, 0.22f), sz(0.94f, 0.56f), r(0.08f))
            drawRoundRect(Color(0xFF66BB6A), o(0.09f, 0.29f), sz(0.82f, 0.42f), r(0.05f))
            drawCircle(gold, s * 0.15f, o(0.5f, 0.5f))
            drawCircle(Color(0xFF2E7D32), s * 0.07f, o(0.5f, 0.5f))
            drawCircle(ChristmasColors.Cream, s * 0.035f, o(0.19f, 0.5f))
            drawCircle(ChristmasColors.Cream, s * 0.035f, o(0.81f, 0.5f))
        }
        GiftIconType.CHOCOLATE -> {
            drawRoundRect(Color(0xFF5D3A1A), o(0.12f, 0.14f), sz(0.76f, 0.72f), r(0.06f))
            val line = Color(0xFF3E2410)
            for (i in 1..2) {
                val x = 0.12f + 0.76f * i / 3f
                drawLine(line, o(x, 0.14f), o(x, 0.86f), strokeWidth = s * 0.03f)
                val y = 0.14f + 0.72f * i / 3f
                drawLine(line, o(0.12f, y), o(0.88f, y), strokeWidth = s * 0.03f)
            }
            drawRect(gold, o(0.12f, 0.70f), sz(0.76f, 0.16f))
            drawRect(red, o(0.12f, 0.74f), sz(0.76f, 0.08f))
        }
        GiftIconType.MUG -> {
            drawCircle(Color.White, s * 0.19f, o(0.75f, 0.50f), style = Stroke(width = s * 0.08f))
            drawRoundRect(Color.White, o(0.12f, 0.20f), sz(0.58f, 0.66f), r(0.09f))
            drawRect(red, o(0.12f, 0.34f), sz(0.58f, 0.12f))
            drawRect(ChristmasColors.Green, o(0.12f, 0.50f), sz(0.58f, 0.06f))
        }
        GiftIconType.TOY_CAR -> {
            drawRoundRect(red, o(0.06f, 0.44f), sz(0.88f, 0.28f), r(0.08f))
            drawRoundRect(red, o(0.24f, 0.26f), sz(0.52f, 0.28f), r(0.09f))
            drawRoundRect(Color(0xFF9AD0F5), o(0.30f, 0.31f), sz(0.40f, 0.15f), r(0.03f))
            drawCircle(Color(0xFF263238), s * 0.12f, o(0.27f, 0.74f))
            drawCircle(Color(0xFF263238), s * 0.12f, o(0.73f, 0.74f))
            drawCircle(gold, s * 0.05f, o(0.27f, 0.74f))
            drawCircle(gold, s * 0.05f, o(0.73f, 0.74f))
        }
        GiftIconType.PERFUME -> {
            drawRoundRect(Color(0xFFF48FB1), o(0.22f, 0.38f), sz(0.56f, 0.52f), r(0.11f))
            drawRect(gold, o(0.41f, 0.24f), sz(0.18f, 0.16f))
            drawRoundRect(gold, o(0.33f, 0.10f), sz(0.34f, 0.15f), r(0.03f))
            drawCircle(Color.White.copy(alpha = 0.7f), s * 0.09f, o(0.50f, 0.64f))
        }
        GiftIconType.BOOK -> {
            drawRoundRect(Color(0xFFB71C1C), o(0.18f, 0.10f), sz(0.64f, 0.80f), r(0.05f))
            drawRect(ChristmasColors.Cream, o(0.26f, 0.84f), sz(0.56f, 0.06f))
            drawRect(gold, o(0.28f, 0.26f), sz(0.44f, 0.07f))
            drawRect(gold, o(0.28f, 0.40f), sz(0.30f, 0.05f))
        }
        GiftIconType.SOCKS -> {
            drawRoundRect(red, o(0.30f, 0.10f), sz(0.32f, 0.56f), r(0.05f))
            drawRoundRect(red, o(0.30f, 0.50f), sz(0.60f, 0.28f), r(0.13f))
            drawRect(Color.White, o(0.28f, 0.10f), sz(0.36f, 0.13f))
            drawRoundRect(ChristmasColors.Green, o(0.68f, 0.54f), sz(0.20f, 0.20f), r(0.10f))
        }
        GiftIconType.CARD -> {
            drawRoundRect(gold, o(0.06f, 0.24f), sz(0.88f, 0.52f), r(0.08f))
            drawRect(red, o(0.06f, 0.40f), sz(0.88f, 0.10f))
            drawRect(red, o(0.60f, 0.24f), sz(0.10f, 0.52f))
            drawCircle(Color.White.copy(alpha = 0.8f), s * 0.05f, o(0.2f, 0.65f))
        }
    }
}
