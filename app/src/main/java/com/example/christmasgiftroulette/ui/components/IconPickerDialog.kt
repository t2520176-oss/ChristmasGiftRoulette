package com.example.christmasgiftroulette.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftroulette.model.GiftIconType
import com.example.christmasgiftroulette.ui.theme.ChristmasColors

/** Tap an icon to choose it for a gift. */
@Composable
fun IconPickerDialog(
    selected: GiftIconType,
    onSelect: (GiftIconType) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an icon") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GiftIconType.entries.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { type ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .widthIn(min = 72.dp)
                                    .heightIn(min = 84.dp)
                                    .background(
                                        if (type == selected) Color(0xFFFFDAD5) else ChristmasColors.CreamDark,
                                        RoundedCornerShape(14.dp),
                                    )
                                    .clickable(role = Role.Button) { onSelect(type) }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                GiftIconView(type, Modifier.size(44.dp))
                                Text(type.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ChristmasColors.Ink)
                            }
                        }
                        repeat(3 - rowItems.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") } },
    )
}
