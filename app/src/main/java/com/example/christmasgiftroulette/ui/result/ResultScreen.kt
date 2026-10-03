package com.example.christmasgiftroulette.ui.result

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.christmasgiftroulette.data.AppSettings
import com.example.christmasgiftroulette.feedback.FeedbackController
import com.example.christmasgiftroulette.game.GameState
import com.example.christmasgiftroulette.model.GiftItem
import com.example.christmasgiftroulette.ui.components.ChristmasButton
import com.example.christmasgiftroulette.ui.components.ChristmasButtonStyle
import com.example.christmasgiftroulette.ui.components.ChristmasOutlinedButton
import com.example.christmasgiftroulette.ui.components.ConfettiOverlay
import com.example.christmasgiftroulette.ui.components.ConfirmDialog
import com.example.christmasgiftroulette.ui.components.GlowingGiftBox
import com.example.christmasgiftroulette.ui.components.RibbonBanner
import com.example.christmasgiftroulette.ui.components.giftRows
import com.example.christmasgiftroulette.ui.theme.ChristmasColors
import com.example.christmasgiftroulette.viewmodel.GiftRouletteUiState

class ResultActions(
    val onNextPick: () -> Unit,
    val onShowSummary: () -> Unit,
    val onReset: () -> Unit,
    val onOpenSettings: () -> Unit,
)

@Composable
fun ResultScreen(
    state: GiftRouletteUiState,
    settings: AppSettings,
    feedback: FeedbackController,
    actions: ResultActions,
    modifier: Modifier = Modifier,
) {
    // Frozen at entry so the screen stays stable while the navigation cross-fade runs.
    val game: GameState = remember { state.game }
    val winner = game.currentWinner ?: return
    val currentSettings by rememberUpdatedState(settings)
    var expandedSelected by rememberSaveable { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { feedback.celebrate(currentSettings) }

    val isLast = game.remainingGifts.isEmpty()

    Box(modifier.fillMaxSize()) {
        ConfettiOverlay()
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val wide = maxWidth >= 840.dp && maxWidth > maxHeight
            val bottomButtons: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isLast) {
                        ChristmasButton("SEE FINAL RESULTS  ▶", actions.onShowSummary, Modifier.fillMaxWidth(), ChristmasButtonStyle.Gold)
                    } else {
                        ChristmasButton("NEXT PICK  ▶", actions.onNextPick, Modifier.fillMaxWidth(), ChristmasButtonStyle.Red)
                    }
                    ChristmasOutlinedButton(
                        "Reset game",
                        onClick = { if (settings.confirmBeforeReset) confirmReset = true else actions.onReset() },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "No duplicate picks until all gifts are used.",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            if (wide) {
                Row(
                    Modifier.widthIn(max = 1180.dp).fillMaxSize().padding(horizontal = 16.dp).align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(
                        Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        RibbonBanner("SELECTED GIFT!")
                        WinnerCard(winner)
                        Box(Modifier.widthIn(max = 480.dp)) { bottomButtons() }
                    }
                    LazyColumn(
                        Modifier.weight(1f).fillMaxSize(),
                        contentPadding = PaddingValues(top = 48.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        giftListSections(game, expandedSelected) { expandedSelected = !expandedSelected }
                    }
                }
            } else {
                Column(
                    Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp).align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(top = 48.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "banner") { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { RibbonBanner("SELECTED GIFT!") } }
                        item(key = "winner") { WinnerCard(winner) }
                        giftListSections(game, expandedSelected) { expandedSelected = !expandedSelected }
                    }
                    Box(Modifier.padding(vertical = 8.dp)) { bottomButtons() }
                }
            }
            IconButton(
                onClick = actions.onOpenSettings,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 8.dp).size(52.dp),
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset game?",
            message = "All gifts go back on the wheel and your selection history is cleared.",
            confirmText = "Reset",
            onConfirm = { confirmReset = false; actions.onReset() },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun WinnerCard(gift: GiftItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = ChristmasColors.Cream),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GlowingGiftBox()
            Text(
                gift.name,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = ChristmasColors.Ink,
            )
            gift.formattedAmount?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.headlineLarge,
                    color = ChristmasColors.Green,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Box(Modifier.heightIn(min = 16.dp))
        }
    }
}

/** "Remaining Gifts (N)" list followed by the collapsible "Already Selected" history. */
private fun LazyListScope.giftListSections(game: GameState, expanded: Boolean, onToggle: () -> Unit) {
    item(key = "remainingHeader") {
        Text(
            "Remaining Gifts (${game.remainingGifts.size})",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    if (game.remainingGifts.isEmpty()) {
        item(key = "noneLeft") {
            Text("That was the last gift!", color = Color.White, style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        giftRows(game.remainingGifts, keyPrefix = "remaining")
    }
    item(key = "selectedHeader") {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(onClick = onToggle)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Already Selected (${game.selectedGifts.size})",
                style = MaterialTheme.typography.titleMedium,
                color = ChristmasColors.GoldLight,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse already selected" else "Expand already selected",
                tint = ChristmasColors.GoldLight,
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
            )
        }
    }
    if (expanded) {
        giftRows(game.selectedGifts, numbered = true, keyPrefix = "selected")
    }
}
