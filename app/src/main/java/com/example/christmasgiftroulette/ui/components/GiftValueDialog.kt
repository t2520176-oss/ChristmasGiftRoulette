package com.example.christmasgiftroulette.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftroulette.model.AmountParseResult
import com.example.christmasgiftroulette.model.AmountParser
import com.example.christmasgiftroulette.model.CurrencyFormatter
import com.example.christmasgiftroulette.model.CurrencyType
import com.example.christmasgiftroulette.ui.theme.ChristmasColors

private const val MAX_LENGTH = 14

private fun presetsFor(currency: CurrencyType): List<Int> = when (currency) {
    CurrencyType.KRW -> listOf(5_000, 10_000, 30_000, 50_000, 100_000)
    CurrencyType.JPY -> listOf(500, 1_000, 3_000, 5_000, 10_000)
    CurrencyType.PHP -> listOf(100, 500, 1_000, 2_000, 5_000)
    CurrencyType.CNY -> listOf(10, 50, 100, 200, 500)
    else -> listOf(5, 10, 20, 50, 100)
}

/**
 * Tap-only way to set a gift's optional value: pick a currency chip, tap a preset or use the
 * on-screen keypad. Nothing needs the system keyboard.
 */
@Composable
fun GiftValueDialog(
    giftName: String,
    initialAmountText: String,
    initialCurrency: CurrencyType,
    onConfirm: (amountText: String, currency: CurrencyType) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialAmountText) }
    var currency by remember { mutableStateOf(initialCurrency) }

    val cleaned = text.trimEnd('.')
    val parsed = AmountParser.parse(cleaned, currency)
    val error = (parsed as? AmountParseResult.Invalid)?.message
    val display = when (parsed) {
        is AmountParseResult.Valid -> parsed.amount?.let { CurrencyFormatter.format(it, currency) } ?: "No value"
        is AmountParseResult.Invalid -> currency.symbol + text
    }

    fun press(key: String) {
        text = when (key) {
            "." -> when {
                currency.fractionDigits == 0 || '.' in text -> text
                text.isEmpty() -> "0."
                else -> "$text."
            }
            else -> {
                val next = if (text == "0") key else text + key
                val dot = next.indexOf('.')
                val decimalsOk = dot < 0 || next.length - dot - 1 <= currency.fractionDigits
                if (next.length <= MAX_LENGTH && decimalsOk) next else text
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gift value (optional)") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(giftName.ifBlank { "Unnamed gift" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)

                // live preview
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(ChristmasColors.CreamDark, RoundedCornerShape(16.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        display,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (error != null) MaterialTheme.colorScheme.error else ChristmasColors.Green,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)

                Text("Currency", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CurrencyType.entries, key = { it.code }) { option ->
                        FilterChip(
                            selected = option == currency,
                            onClick = {
                                currency = option
                                if (option.fractionDigits == 0 && '.' in text) text = text.substringBefore('.')
                            },
                            label = { Text("${option.flag} ${option.symbol} ${option.code}", fontWeight = FontWeight.Bold) },
                        )
                    }
                }

                Text("Quick amounts", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetsFor(currency), key = { it }) { preset ->
                        AssistChip(
                            onClick = { text = preset.toString() },
                            label = { Text(CurrencyFormatter.format(preset.toBigDecimal(), currency), fontWeight = FontWeight.Bold) },
                        )
                    }
                }

                val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key -> KeypadKey(key, { press(key) }, Modifier.weight(1f)) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currency.fractionDigits > 0) {
                        KeypadKey(".", { press(".") }, Modifier.weight(1f))
                    } else {
                        KeypadKey("C", { text = "" }, Modifier.weight(1f), description = "Clear value")
                    }
                    KeypadKey("0", { press("0") }, Modifier.weight(1f))
                    KeypadKey("⌫", { text = text.dropLast(1) }, Modifier.weight(1f), description = "Delete last digit")
                }
                TextButton(onClick = { text = "" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("No value (just the gift)") }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(cleaned, currency) }, enabled = error == null, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Done", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } },
    )
}

@Composable
private fun KeypadKey(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, description: String? = null) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp).then(
            if (description != null) Modifier.semantics { contentDescription = description } else Modifier,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}
