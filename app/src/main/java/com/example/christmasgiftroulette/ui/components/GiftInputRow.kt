package com.example.christmasgiftroulette.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.christmasgiftroulette.game.RowValidation
import com.example.christmasgiftroulette.model.CurrencyType
import com.example.christmasgiftroulette.model.GiftDraft
import com.example.christmasgiftroulette.ui.theme.ChristmasColors

/**
 * One editable gift: name, optional amount and currency. Lays out in one line on wide rows and in
 * two lines on narrow ones. Errors are shown with text (not just colour).
 */
@Composable
fun GiftInputRow(
    index: Int,
    draft: GiftDraft,
    validation: RowValidation?,
    showNameError: Boolean,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (CurrencyType) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nameError = if (showNameError) validation?.nameError else null
    // amount problems are shown live as soon as something invalid was typed
    val amountError = validation?.amountError

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ChristmasColors.Cream),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        BoxWithConstraints(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            val wide = maxWidth >= 560.dp
            val nameField: @Composable (Modifier) -> Unit = { m ->
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    modifier = m,
                    singleLine = true,
                    label = { Text("Gift name") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
            }
            val amountField: @Composable (Modifier) -> Unit = { m ->
                OutlinedTextField(
                    value = draft.amountText,
                    onValueChange = onAmountChange,
                    modifier = m,
                    singleLine = true,
                    label = { Text("Amount (optional)") },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                )
            }
            val deleteButton: @Composable () -> Unit = {
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete gift ${index + 1}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (wide) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IndexBadge(index, Modifier.padding(top = 14.dp))
                    nameField(Modifier.weight(1.5f))
                    amountField(Modifier.weight(1f))
                    CurrencyDropdown(draft.currency, onCurrencyChange, Modifier.width(150.dp))
                    Column(Modifier.padding(top = 4.dp)) { deleteButton() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IndexBadge(index, Modifier.padding(top = 14.dp))
                        nameField(Modifier.weight(1f))
                        Column(Modifier.padding(top = 4.dp)) { deleteButton() }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Spacer(Modifier.width(32.dp))
                        amountField(Modifier.weight(1f))
                        CurrencyDropdown(draft.currency, onCurrencyChange, Modifier.width(140.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun IndexBadge(index: Int, modifier: Modifier = Modifier) {
    Text(
        text = "${index + 1}",
        modifier = modifier.width(24.dp),
        style = MaterialTheme.typography.titleMedium,
        color = ChristmasColors.Red,
    )
}
