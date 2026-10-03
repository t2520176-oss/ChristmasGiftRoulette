package com.example.christmasgiftroulette.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.christmasgiftroulette.data.AppSettings
import com.example.christmasgiftroulette.feedback.FeedbackController
import com.example.christmasgiftroulette.model.GiftItem
import com.example.christmasgiftroulette.ui.components.ChristmasButton
import com.example.christmasgiftroulette.ui.components.ChristmasButtonStyle
import com.example.christmasgiftroulette.ui.components.ConfettiOverlay
import com.example.christmasgiftroulette.ui.components.GlowingGiftBox
import com.example.christmasgiftroulette.ui.components.RibbonBanner
import com.example.christmasgiftroulette.ui.components.giftRows
import com.example.christmasgiftroulette.viewmodel.GiftRouletteUiState

class CompletionActions(
    val onPlayAgain: () -> Unit,
    val onNewGiftList: () -> Unit,
    val onOpenSettings: () -> Unit,
)

@Composable
fun CompletionScreen(
    state: GiftRouletteUiState,
    settings: AppSettings,
    feedback: FeedbackController,
    actions: CompletionActions,
    modifier: Modifier = Modifier,
) {
    // Frozen at entry: pressing PLAY AGAIN clears the history while the screen fades out.
    val selected = remember { state.game.selectedGifts }
    val currentSettings by rememberUpdatedState(settings)
    LaunchedEffect(Unit) { feedback.celebrate(currentSettings) }

    Box(modifier.fillMaxSize()) {
        ConfettiOverlay()
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val wide = maxWidth >= 840.dp && maxWidth > maxHeight
            val buttons: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChristmasButton("PLAY AGAIN", actions.onPlayAgain, Modifier.fillMaxWidth(), ChristmasButtonStyle.Green)
                    ChristmasButton("NEW GIFT LIST", actions.onNewGiftList, Modifier.fillMaxWidth(), ChristmasButtonStyle.Gold)
                }
            }
            val header: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RibbonBanner("ALL GIFTS HAVE BEEN SELECTED!")
                    GlowingGiftBox(size = if (wide) 120.dp else 84.dp)
                }
            }
            if (wide) {
                Row(
                    Modifier.widthIn(max = 1100.dp).fillMaxSize().padding(horizontal = 16.dp).align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(
                        Modifier.weight(1f).fillMaxSize().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                    ) {
                        header()
                        Box(Modifier.widthIn(max = 460.dp)) { buttons() }
                    }
                    OrderList(selected, Modifier.weight(1f).fillMaxSize())
                }
            } else {
                Column(
                    Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp).align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.padding(top = 48.dp)) { header() }
                    OrderList(selected, Modifier.weight(1f).fillMaxWidth())
                    Box(Modifier.padding(vertical = 8.dp)) { buttons() }
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
}

@Composable
private fun OrderList(selected: List<GiftItem>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "orderHeader") {
            Text(
                "Selection order",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Start,
            )
        }
        giftRows(selected, numbered = true, keyPrefix = "order")
    }
}
