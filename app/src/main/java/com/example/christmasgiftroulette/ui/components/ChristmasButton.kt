package com.example.christmasgiftroulette.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftroulette.ui.theme.ChristmasColors

enum class ChristmasButtonStyle { Green, Red, Gold }

/** Large, rounded festive button with a gold rim. Minimum height 64dp for easy touch. */
@Composable
fun ChristmasButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChristmasButtonStyle = ChristmasButtonStyle.Green,
    enabled: Boolean = true,
) {
    val container = when (style) {
        ChristmasButtonStyle.Green -> ChristmasColors.Green
        ChristmasButtonStyle.Red -> ChristmasColors.Red
        ChristmasButtonStyle.Gold -> ChristmasColors.Gold
    }
    val content = if (style == ChristmasButtonStyle.Gold) ChristmasColors.Ink else Color.White
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 64.dp),
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.45f),
            disabledContentColor = content.copy(alpha = 0.7f),
        ),
        border = BorderStroke(2.dp, if (enabled) ChristmasColors.GoldLight else ChristmasColors.GoldLight.copy(alpha = 0.4f)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}

/** Secondary, outlined variant for less important actions (still at least 48dp tall). */
@Composable
fun ChristmasOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(2.dp, ChristmasColors.GoldLight.copy(alpha = if (enabled) 1f else 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.5f),
        ),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
