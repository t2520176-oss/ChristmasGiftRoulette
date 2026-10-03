package com.example.christmasgiftroulette.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftroulette.game.GameRules
import com.example.christmasgiftroulette.game.GiftValidator
import com.example.christmasgiftroulette.game.RowValidation
import com.example.christmasgiftroulette.model.CurrencyType
import com.example.christmasgiftroulette.ui.components.ChristmasButton
import com.example.christmasgiftroulette.ui.components.GiftIcon
import com.example.christmasgiftroulette.ui.components.GiftInputRow
import com.example.christmasgiftroulette.ui.components.GiftValueDialog
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
    val onQuickName: (String) -> Unit,
    val onValueChange: (id: String, amountText: String, currency: CurrencyType) -> Unit,
    val onStart: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onMessageConsumed: (Long) -> Unit,
)

private val WideBreakpoint = 840.dp
private val QuickCounts = listOf(3, 5, 8, 10, 15, 20)
private val Suggestions = listOf("Chocolate", "Mug", "Socks", "Perfume", "Toy car", "Gift card", "Cash prize", "Candle", "Book", "Mystery gift")

@Composable
fun SetupScreen(state: GiftRouletteUiState, actions: SetupActions, modifier: Modifier = Modifier) {
    val setup = state.setup
    val rowErrors = remember(setup.rows) { GiftValidator.validate(setup.rows).rowErrors }
    val snackbar = rememberMessageSnackbar(state.message, actions.onMessageConsumed)
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

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
                            }
                            GiftList(state, rowErrors, actions, showIntro = false, onEditValue = { editingId = it }, modifier = Modifier.weight(1f).fillMaxSize())
                        }
                    } else {
                        GiftList(
                            state, rowErrors, actions, showIntro = true, onEditValue = { editingId = it },
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

    val editing = editingId?.let { id -> setup.rows.firstOrNull { it.id == id } }
    if (editing != null) {
        GiftValueDialog(
            giftName = editing.name,
            initialAmountText = editing.amountText,
            initialCurrency = editing.currency,
            onConfirm = { text, currency ->
                actions.onValueChange(editing.id, text, currency)
                editingId = null
            },
            onDismiss = { editingId = null },
        )
    }
}

@Composable
private fun GiftList(
    state: GiftRouletteUiState,
    rowErrors: Map<String, RowValidation>,
    actions: SetupActions,
    showIntro: Boolean,
    onEditValue: (String) -> Unit,
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
                Box(Modifier.fillMaxWidth().padding(end = 52.dp), contentAlignment = Alignment.Center) { TitleSign() }
            }
            item(key = "count") { CountSection(setup.giftCount, actions) }
        }
        item(key = "namesHeader") {
            SectionCard(
                number = 2,
                title = "Name your gifts",
                subtitle = "Type a name, or tap a suggestion. Tap \"Add value\" to set an optional price.",
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Suggestions, key = { it }) { name ->
                        AssistChip(onClick = { actions.onQuickName(name) }, label = { Text(name, fontWeight = FontWeight.Bold) })
                    }
                }
            }
        }
        itemsIndexed(setup.rows, key = { _, row -> row.id }) { index, row ->
            GiftInputRow(
                index = index,
                draft = row,
                validation = rowErrors[row.id],
                showNameError = setup.showErrors,
                onNameChange = { actions.onNameChange(row.id, it) },
                onEditValue = { onEditValue(row.id) },
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
        title = "How many gifts?",
        subtitle = "Tap − or + (${GameRules.MIN_GIFTS}–${GameRules.MAX_GIFTS}), or pick a number",
    ) {
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
                modifier = Modifier.size(60.dp).semantics { contentDescription = "Decrease number of gifts" },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = ChristmasColors.Red, contentColor = Color.White),
            ) { Text("−", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold) }
            Text(
                "$count",
                modifier = Modifier.width(88.dp).semantics { contentDescription = "$count gifts" },
                style = MaterialTheme.typography.headlineLarge,
                color = ChristmasColors.Ink,
                textAlign = TextAlign.Center,
            )
            FilledIconButton(
                onClick = actions.onIncrement,
                enabled = count < GameRules.MAX_GIFTS,
                modifier = Modifier.size(60.dp).semantics { contentDescription = "Increase number of gifts" },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = ChristmasColors.Green, contentColor = Color.White),
            ) { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(32.dp)) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(QuickCounts, key = { it }) { n ->
                FilterChip(
                    selected = n == count,
                    onClick = { actions.onSetCount(n) },
                    label = { Text("$n", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) },
                )
            }
        }
    }
}
