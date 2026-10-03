package com.example.christmasgiftroulette.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.christmasgiftroulette.ui.theme.ChristmasColors

/** Original vector-style gift box drawn on a Canvas (no bitmap assets). Purely decorative. */
@Composable
fun GiftIcon(
    modifier: Modifier = Modifier,
    boxColor: Color = ChristmasColors.Red,
    lidColor: Color = ChristmasColors.RedBright,
    ribbonColor: Color = ChristmasColors.Gold,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(boxColor, Offset(w * 0.10f, h * 0.42f), Size(w * 0.80f, h * 0.52f), CornerRadius(w * 0.06f))
        drawRoundRect(lidColor, Offset(w * 0.04f, h * 0.30f), Size(w * 0.92f, h * 0.18f), CornerRadius(w * 0.05f))
        drawRect(ribbonColor, Offset(w * 0.44f, h * 0.30f), Size(w * 0.12f, h * 0.64f))
        drawOval(ribbonColor, Offset(w * 0.20f, h * 0.08f), Size(w * 0.30f, h * 0.24f))
        drawOval(ribbonColor, Offset(w * 0.50f, h * 0.08f), Size(w * 0.30f, h * 0.24f))
        drawCircle(Color(0xFFFFE08A), w * 0.075f, Offset(w * 0.50f, h * 0.30f))
    }
}
