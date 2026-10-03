package com.example.christmasgiftroulette.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.christmasgiftroulette.game.RowValidation
import com.example.christmasgiftroulette.model.AmountParseResult
import com.example.christmasgiftroulette.model.AmountParser
import com.example.christmasgiftroulette.model.CurrencyFormatter
import com.example.christmasgiftroulette.model.GiftDraft
import com.example.christmasgiftroulette.model.GiftIconType
import com.example.christmasgiftroulette.ui.theme.ChristmasColors

/**
 * One gift: a name field plus a single tappable value button (opens [GiftValueDialog]).
 * Wide rows put everything on one line; narrow rows put the value button underneath.
 */
@Composable
fun GiftInputRow(
    index: Int,
    draft: GiftDraft,
    validation: RowValidation?,
    showNameError: Boolean,
    onNameChange: (String) -> Unit,
    onEditValue: () -> Unit,
    onPickIcon: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nameError = if (showNameError) validation?.nameError else null
    val valueLabel = remember(draft.amountText, draft.currency) {
        when (val parsed = AmountParser.parse(draft.amountText, draft.currency)) {
            is AmountParseResult.Valid -> parsed.amount?.let { CurrencyFormatter.format(it, draft.currency) }
            is AmountParseResult.Invalid -> draft.amountText
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ChristmasColors.Cream),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        BoxWithConstraints(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            val wide = maxWidth >= 520.dp
            val nameField: @Composable (Modifier) -> Unit = { m ->
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    modifier = m,
                    singleLine = true,
                    label = { Text("Gift name") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            }
            val valueButton: @Composable (Modifier) -> Unit = { m ->
                FilledTonalButton(onClick = onEditValue, modifier = m.heightIn(min = 52.dp)) {
                    Text(
                        text = valueLabel ?: "＋ Add value (optional)",
                        fontWeight = if (valueLabel != null) FontWeight.ExtraBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val deleteButton: @Composable () -> Unit = {
                M3IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete gift ${index + 1}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    GiftPictureButton(index, draft.icon, onPickIcon, Modifier.padding(top = 4.dp))
                    nameField(Modifier.weight(1.4f))
                    valueButton(Modifier.weight(1f).padding(top = 4.dp))
                    Column(Modifier.padding(top = 4.dp)) { deleteButton() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GiftPictureButton(index, draft.icon, onPickIcon, Modifier.padding(top = 4.dp))
                        nameField(Modifier.weight(1f))
                        Column(Modifier.padding(top = 4.dp)) { deleteButton() }
                    }
                    Row {
                        Spacer(Modifier.width(52.dp))
                        valueButton(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun GiftPictureButton(index: Int, icon: GiftIconType, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(48.dp)
            .background(ChristmasColors.CreamDark, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = "Change icon of gift ${index + 1}", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GiftIconView(icon, Modifier.size(34.dp))
    }
}
