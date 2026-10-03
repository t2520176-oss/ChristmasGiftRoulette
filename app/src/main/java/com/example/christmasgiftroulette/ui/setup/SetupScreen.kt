package com.example.christmasgiftroulette.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftroulette.game.GameRules
import com.example.christmasgiftroulette.game.GiftValidator
import com.example.christmasgiftroulette.game.RowValidation
import com.example.christmasgiftroulette.model.CurrencyType
import com.example.christmasgiftroulette.ui.components.ChristmasButton
import com.example.christmasgiftroulette.ui.components.CurrencyDropdown
import com.example.christmasgiftroulette.ui.components.GiftIcon
import com.example.christmasgiftroulette.ui.components.GiftInputRow
import com.example.christmasgiftroulette.ui.components.SectionCard
import com.example.christmasgiftroulette.ui.components.TitleSign
import com.example.christmasgiftroulette.ui.components.rememberMessageSnackbar
import com.example.christmasgiftroulette.ui.theme.ChristmasColors
import com.example.christmasgiftroulette.viewmodel.GiftRouletteUiState

/** All user intents of the setup screen. The ViewModel owns the logic. */
class SetupActions(
    val onSetCount: (Int) -> Unit,
    val onIncrement: () -> Unit,
    val onDecrement: () -> Unit,
    val onAddGift: () -> Unit,
    val onRemoveGift: (String) -> Unit,
    val onNameChange: (String, String) -> Unit,
    val onAmountChange: (String, String) -> Unit,
    val onCurrencyChange: (String, CurrencyType) -> Unit,
    val onDefaultCurrency: (CurrencyType) -> Unit,
    val onApplyCurrencyToAll: (CurrencyType) -> Unit,
    val onStart: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onMessageConsumed: (Long) -> Unit,
)

private val WideBreakpoint = 840.dp

@Composable
fun SetupScreen(state: GiftRouletteUiState, actions: SetupActions, modifier: Modifier = Modifier) {
    val setup = state.setup
    val rowErrors = remember(setup.rows) { GiftValidator.validate(setup.rows).rowErrors }
    val snackbar = rememberMessageSnackbar(state.message, actions.onMessageConsumed)

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val wide = maxWidth >= WideBreakpoint
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    if (wide) {
                        Row(
                            Modifier.widthIn(max = 1180.dp).fillMaxSize().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            Column(
                                Modifier.width(380.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 40.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                TitleSign()
                                CountSection(setup.giftCount, actions)
                                CurrencySection(setup.defaultCurrency, actions)
                            }
                            GiftList(state, rowErrors, actions, showIntro = false, modifier = Modifier.weight(1f).fillMaxSize())
                        }
                    } else {
                        GiftList(
                            state, rowErrors, actions, showIntro = true,
                            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp),
                        )
                    }
                    IconButton(
                        onClick = actions.onOpenSettings,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 8.dp).size(52.dp),
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
                ChristmasButton(
                    text = "START ROULETTE  ▶",
                    onClick = actions.onStart,
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun GiftList(
    state: GiftRouletteUiState,
    rowErrors: Map<String, RowValidation>,
    actions: SetupActions,
    showIntro: Boolean,
    modifier: Modifier = Modifier,
) {
    val setup = state.setup
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 40.dp, bottom = 12.dp),
    ) {
        if (showIntro) {
            item(key = "title") {
                Box(Modifier.fillMaxWidth().padding(end = 52.dp), contentAlignment = Alignment.Center) { TitleSign(compact = false) }
            }
            item(key = "count") { CountSection(setup.giftCount, actions) }
            item(key = "currency") { CurrencySection(setup.defaultCurrency, actions) }
        }
        item(key = "listHeader") {
            SectionCard(
                number = 3,
                title = "Enter Gift Names & Prices",
                subtitle = "Type the gift names and set a price for each gift (price is optional)",
            ) {}
        }
        itemsIndexed(setup.rows, key = { _, row -> row.id }) { index, row ->
            GiftInputRow(
                index = index,
                draft = row,
                validation = rowErrors[row.id],
                showNameError = setup.showErrors,
                onNameChange = { actions.onNameChange(row.id, it) },
                onAmountChange = { actions.onAmountChange(row.id, it) },
                onCurrencyChange = { actions.onCurrencyChange(row.id, it) },
                onDelete = { actions.onRemoveGift(row.id) },
            )
        }
        item(key = "add") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FilledTonalButton(
                    onClick = actions.onAddGift,
                    modifier = Modifier.padding(vertical = 4.dp).heightIn(min = 52.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("  Add Gift", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
        item(key = "hint") {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(
                    "  No duplicate picks until all gifts are used.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun CountSection(count: Int, actions: SetupActions) {
    SectionCard(
        number = 1,
        title = "Select Number of Gifts",
        subtitle = "Choose how many gifts you want to add (${GameRules.MIN_GIFTS}–${GameRules.MAX_GIFTS})",
    ) {
        var text by remember(count) { mutableStateOf(count.toString()) }
        val parsed = text.toIntOrNull()
        val invalid = parsed == null || parsed !in GameRules.MIN_GIFTS..GameRules.MAX_GIFTS
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GiftIcon(Modifier.size(52.dp))
            Spacer(Modifier.width(14.dp))
            FilledIconButton(
                onClick = actions.onDecrement,
                enabled = count > GameRules.MIN_GIFTS,
                modifier = Modifier.size(56.dp).semantics { contentDescription = "Decrease number of gifts" },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = ChristmasColors.Red, contentColor = Color.White),
            ) { Text("−", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold) }
            OutlinedTextField(
                value = text,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(2)
                    text = digits
                    digits.toIntOrNull()?.takeIf { it in GameRules.MIN_GIFTS..GameRules.MAX_GIFTS }?.let(actions.onSetCount)
                },
                modifier = Modifier.width(92.dp).padding(horizontal = 8.dp).semantics { contentDescription = "Number of gifts" },
                singleLine = true,
                isError = invalid,
                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            )
            FilledIconButton(
                onClick = actions.onIncrement,
                enabled = count < GameRules.MAX_GIFTS,
                modifier = Modifier.size(56.dp).semantics { contentDescription = "Increase number of gifts" },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = ChristmasColors.Green, contentColor = Color.White),
            ) { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(30.dp)) }
        }
        if (invalid) {
            Text(
                "Enter a number from ${GameRules.MIN_GIFTS} to ${GameRules.MAX_GIFTS}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CurrencySection(selected: CurrencyType, actions: SetupActions) {
    SectionCard(
        number = 2,
        title = "Country / Currency",
        subtitle = "Currency for newly added gifts — every gift can still choose its own",
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CurrencyType.entries, key = { it.code }) { currency ->
                FilterChip(
                    selected = currency == selected,
                    onClick = { actions.onDefaultCurrency(currency) },
                    label = { Text("${currency.flag} ${currency.symbol} ${currency.code}", fontWeight = FontWeight.Bold) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CurrencyDropdown(selected, actions.onDefaultCurrency, Modifier.weight(1f), label = "Default currency")
            TextButton(onClick = { actions.onApplyCurrencyToAll(selected) }) {
                Text("Apply to all", fontWeight = FontWeight.Bold)
            }
        }
    }
}
