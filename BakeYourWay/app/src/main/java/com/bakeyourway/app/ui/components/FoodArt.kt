package com.bakeyourway.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakeyourway.core.CookingMethod
import com.bakeyourway.core.RecipeCategory

private fun palette(category: RecipeCategory): Pair<Color, Color> = when (category) {
    RecipeCategory.BREAD -> Color(0xFFFFE9C9) to Color(0xFFF6C98B)
    RecipeCategory.COOKIES -> Color(0xFFFFEBD0) to Color(0xFFF2B885)
    RecipeCategory.MUFFINS -> Color(0xFFE6EDFB) to Color(0xFFBFCDF2)
    RecipeCategory.CUPCAKES -> Color(0xFFFFE3EA) to Color(0xFFFFB8C9)
    RecipeCategory.PIE -> Color(0xFFFFE8C2) to Color(0xFFF0B25E)
    RecipeCategory.PANCAKES -> Color(0xFFFFF0C6) to Color(0xFFF7CF72)
    RecipeCategory.BROWNIES -> Color(0xFFEBD5C9) to Color(0xFFC79A82)
    RecipeCategory.SCONES -> Color(0xFFFFE6CF) to Color(0xFFEDB78A)
    RecipeCategory.CAKES -> Color(0xFFFFE0E8) to Color(0xFFF9AFC3)
    RecipeCategory.QUICK_BREADS -> Color(0xFFF7E9C6) to Color(0xFFE3C27C)
    RecipeCategory.BISCUITS -> Color(0xFFFFEFD6) to Color(0xFFEFC694)
    RecipeCategory.MORE -> Color(0xFFE8F2E4) to Color(0xFFB9D8B1)
}

/** Only emoji that exist on Android 8.0 (Emoji 5.0) are used. */
private fun mainEmoji(category: RecipeCategory): String? = when (category) {
    RecipeCategory.BREAD -> "🍞"
    RecipeCategory.COOKIES -> "🍪"
    RecipeCategory.MUFFINS, RecipeCategory.CUPCAKES -> null // drawn with a Canvas
    RecipeCategory.PIE -> "🥧"
    RecipeCategory.PANCAKES -> "🥞"
    RecipeCategory.BROWNIES -> "🍫"
    RecipeCategory.SCONES -> "🥐"
    RecipeCategory.CAKES -> "🍰"
    RecipeCategory.QUICK_BREADS -> "🥖"
    RecipeCategory.BISCUITS -> "🥯"
    RecipeCategory.MORE -> "🥙"
}

private fun accentEmoji(imageKey: String?): String? = when (imageKey) {
    "blueberry" -> "🍇"
    "chocolate" -> "🍫"
    "banana" -> "🍌"
    "apple" -> "🍎"
    "strawberry" -> "🍓"
    "peanut" -> "🥜"
    "walnut" -> "🌰"
    "oat" -> "🌾"
    "cinnamon" -> "🍂"
    "vanilla" -> "🍦"
    "cheese" -> "🧀"
    "raisin" -> "🍇"
    "cornbread" -> "🌽"
    else -> null
}

private fun toppingColor(imageKey: String?): Color = when (imageKey) {
    "blueberry" -> Color(0xFF3F51B5)
    "chocolate" -> Color(0xFF5D3A2A)
    "banana" -> Color(0xFFF2C94C)
    "apple" -> Color(0xFFC62828)
    "strawberry" -> Color(0xFFE53958)
    "cinnamon" -> Color(0xFF9A5B33)
    else -> Color(0xFFFFFFFF)
}

/**
 * Local "photo" for a recipe or category: a soft gradient with a big illustration. Everything is drawn
 * or typed locally so the app needs no images and no internet.
 */
@Composable
fun FoodArt(
    category: RecipeCategory,
    modifier: Modifier = Modifier,
    imageKey: String? = null,
) {
    val (top, bottom) = palette(category)
    BoxWithConstraints(
        modifier = modifier.background(Brush.linearGradient(listOf(top, bottom))),
        contentAlignment = Alignment.Center,
    ) {
        val side = if (maxWidth < maxHeight) maxWidth else maxHeight
        val emoji = mainEmoji(category)
        if (emoji == null) {
            CupcakeArt(
                topping = toppingColor(imageKey),
                muffin = category == RecipeCategory.MUFFINS,
                modifier = Modifier.fillMaxSize().padding(side * 0.12f),
            )
        } else {
            Text(text = emoji, fontSize = (side.value * 0.5f).sp)
        }
        val accent = accentEmoji(imageKey)
        if (accent != null) {
            Box(Modifier.fillMaxSize().padding(side * 0.06f), contentAlignment = Alignment.BottomEnd) {
                Text(text = accent, fontSize = (side.value * 0.24f).sp)
            }
        }
    }
}

@Composable
private fun CupcakeArt(topping: Color, muffin: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val liner = Path().apply {
            moveTo(w * 0.22f, h * 0.55f)
            lineTo(w * 0.78f, h * 0.55f)
            lineTo(w * 0.69f, h * 0.92f)
            lineTo(w * 0.31f, h * 0.92f)
            close()
        }
        drawPath(liner, Color(0xFFD93A5E))
        for (i in 0..3) {
            val x = w * (0.31f + i * 0.13f)
            drawLine(Color(0xFFF58AA3), Offset(x, h * 0.57f), Offset(x * 0.97f + w * 0.015f, h * 0.9f), strokeWidth = w * 0.03f)
        }
        if (muffin) {
            // Domed muffin top
            drawArc(
                color = Color(0xFFD9A066), startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = Offset(w * 0.12f, h * 0.12f), size = Size(w * 0.76f, h * 0.86f),
            )
            drawRoundRect(Color(0xFFD9A066), Offset(w * 0.18f, h * 0.45f), Size(w * 0.64f, h * 0.14f), CornerRadius(w * 0.07f))
            val dots = listOf(0.35f to 0.3f, 0.52f to 0.22f, 0.66f to 0.34f, 0.46f to 0.41f, 0.28f to 0.44f)
            dots.forEach { (x, y) -> drawCircle(topping, radius = w * 0.045f, center = Offset(w * x, h * y)) }
        } else {
            // Frosted cupcake
            drawRoundRect(Color(0xFFD9A066), Offset(w * 0.2f, h * 0.42f), Size(w * 0.6f, h * 0.17f), CornerRadius(w * 0.07f))
            drawFrosting(Color(0xFFFFB7C5), w * 0.5f, h * 0.38f, w * 0.34f, h * 0.2f)
            drawFrosting(Color(0xFFFFCDD8), w * 0.5f, h * 0.24f, w * 0.25f, h * 0.17f)
            drawFrosting(Color(0xFFFFDFE6), w * 0.5f, h * 0.13f, w * 0.15f, h * 0.13f)
            drawCircle(Color(0xFFB71C3C), radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.07f))
            if (topping != Color.White) {
                listOf(0.38f, 0.5f, 0.62f).forEach { x ->
                    drawCircle(topping, radius = w * 0.022f, center = Offset(w * x, h * 0.34f))
                }
            }
        }
    }
}

private fun DrawScope.drawFrosting(color: Color, cx: Float, cy: Float, halfWidth: Float, height: Float) {
    drawRoundRect(color, Offset(cx - halfWidth, cy), Size(halfWidth * 2, height), CornerRadius(height / 2))
}

fun methodEmoji(method: CookingMethod): String = when (method) {
    CookingMethod.OVEN -> "🔥"
    CookingMethod.AIR_FRYER -> "🌀"
    CookingMethod.STEAM -> "♨"
    CookingMethod.STOVETOP -> "🍳"
}
