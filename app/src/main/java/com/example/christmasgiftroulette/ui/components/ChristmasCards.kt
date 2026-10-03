package com.example.christmasgiftroulette.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftroulette.ui.theme.ChristmasColors

/** Wooden-sign style title banner. */
@Composable
fun TitleSign(modifier: Modifier = Modifier, compact: Boolean = false) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .shadow(8.dp, shape)
            .background(Brush.verticalGradient(listOf(ChristmasColors.Wood, ChristmasColors.WoodDark)), shape)
            .border(BorderStroke(3.dp, ChristmasColors.Gold), shape)
            .padding(horizontal = 24.dp, vertical = if (compact) 8.dp else 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        val style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.ExtraBold,
            color = ChristmasColors.GoldLight,
            shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(2f, 3f), 4f),
            textAlign = TextAlign.Center,
        )
        if (compact) {
            Text("Christmas Gift Roulette", style = style.copy(fontSize = 22.sp, lineHeight = 26.sp))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Christmas", style = style.copy(fontSize = 38.sp, lineHeight = 40.sp))
                Text("Gift Roulette", style = style.copy(fontSize = 32.sp, lineHeight = 36.sp))
            }
        }
    }
}

/** Red ribbon banner used for headings like "SELECTED GIFT!". */
@Composable
fun RibbonBanner(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .shadow(6.dp, shape)
            .background(Brush.verticalGradient(listOf(ChristmasColors.RedBright, ChristmasColors.Red)), shape)
            .border(BorderStroke(2.dp, ChristmasColors.GoldLight), shape)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Color.White,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(2f, 2f), 3f),
            ),
        )
    }
}

/** Cream section card with a numbered red badge, like the numbered steps in the design. */
@Composable
fun SectionCard(
    number: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ChristmasColors.Cream),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(34.dp).background(ChristmasColors.Red, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$number", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

/** Generic confirm dialog (reset, leave game…). */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
