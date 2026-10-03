package com.example.christmasgiftroulette.ui.roulette

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.example.christmasgiftroulette.data.AppSettings
import com.example.christmasgiftroulette.data.GamePhase
import com.example.christmasgiftroulette.feedback.FeedbackController
import com.example.christmasgiftroulette.game.WheelMath
import com.example.christmasgiftroulette.model.GiftItem
import com.example.christmasgiftroulette.ui.components.ChristmasButton
import com.example.christmasgiftroulette.ui.components.ConfirmDialog
import com.example.christmasgiftroulette.ui.components.RibbonBanner
import com.example.christmasgiftroulette.viewmodel.GiftRouletteUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

class RouletteActions(
    val onSpin: () -> Unit,
    val onSpinFinished: (token: Long, finalRotation: Float) -> Unit,
    val onLeave: () -> Unit,
    val onReset: () -> Unit,
    val onOpenSettings: () -> Unit,
)

private enum class PendingConfirm { Reset, Leave }

private const val SPIN_DURATION_MS = 5_200
private val SpinEasing = CubicBezierEasing(0.10f, 0.72f, 0.12f, 1.0f) // fast start, long gradual deceleration

@Composable
fun RouletteScreen(
    state: GiftRouletteUiState,
    settings: AppSettings,
    feedback: FeedbackController,
    actions: RouletteActions,
    modifier: Modifier = Modifier,
) {
    val game = state.game
    val currentSettings by rememberUpdatedState(settings)
    var pendingConfirm by remember { mutableStateOf<PendingConfirm?>(null) }

    // The wheel keeps showing the pool it started spinning with, even in the instant after the
    // winner is committed (while the screen cross-fades to the result), so it never "jumps".
    var wheelGifts by remember { mutableStateOf(game.remainingGifts) }
    LaunchedEffect(game.remainingGifts, state.phase, state.isSpinning) {
        if (state.phase == GamePhase.ROULETTE && !state.isSpinning) wheelGifts = game.remainingGifts
    }

    val rotation = remember { Animatable(state.wheelRotation) }
    val request = state.spinRequest

    LaunchedEffect(request?.token) {
        val req = request ?: return@LaunchedEffect
        val index = wheelGifts.indexOfFirst { it.id == req.winnerId }
        if (index < 0) return@LaunchedEffect
        feedback.spinStarted(currentSettings)
        val target = WheelMath.targetRotation(
            currentRotation = rotation.value,
            winnerIndex = index,
            count = wheelGifts.size,
            extraTurns = req.extraTurns,
            offsetFraction = req.offsetFraction,
        )
        rotation.animateTo(target, tween(durationMillis = SPIN_DURATION_MS, easing = SpinEasing))
        actions.onSpinFinished(req.token, rotation.value)
    }

    // a tick each time a segment passes the pointer
    LaunchedEffect(state.isSpinning) {
        if (!state.isSpinning) return@LaunchedEffect
        snapshotFlow { WheelMath.indexAtPointer(rotation.value, wheelGifts.size) }
            .distinctUntilChanged()
            .drop(1)
            .collect { feedback.tick(currentSettings) }
    }

    fun requestLeave() {
        if (state.isSpinning) return
        if (settings.confirmBeforeReset && game.selectedGifts.isNotEmpty()) pendingConfirm = PendingConfirm.Leave else actions.onLeave()
    }

    fun requestReset() {
        if (state.isSpinning) return
        if (settings.confirmBeforeReset && game.selectedGifts.isNotEmpty()) pendingConfirm = PendingConfirm.Reset else actions.onReset()
    }

    // Back while the wheel is turning is ignored; otherwise it leaves the game (with confirmation).
    BackHandler { requestLeave() }

    BoxWithConstraints(modifier.fillMaxSize().safeDrawingPadding()) {
        val landscape = maxWidth > maxHeight * 1.15f
        val topBarHeight = 64.dp
        val remainingCount = wheelGifts.size
        val spinLabel = when {
            state.isSpinning -> "SPINNING…"
            remainingCount == 1 -> "REVEAL LAST GIFT"
            else -> "SPIN"
        }

        Column(Modifier.fillMaxSize()) {
            TopBar(
                enabled = !state.isSpinning,
                onBack = ::requestLeave,
                onReset = ::requestReset,
                onSettings = actions.onOpenSettings,
                modifier = Modifier.height(topBarHeight),
            )
            if (landscape) {
                val wheelSize = minOf(maxHeight - topBarHeight - 12.dp, maxWidth * 0.55f, 900.dp).coerceAtLeast(160.dp)
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelBox(wheelGifts.size, wheelSize, wheelGifts, rotation)
                    Column(
                        Modifier.widthIn(max = 380.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        RibbonBanner(if (state.isSpinning) "Spinning…" else "Spin the Wheel!")
                        GiftsLeftChip(remainingCount, game.gifts.size)
                        ChristmasButton(spinLabel, actions.onSpin, Modifier.fillMaxWidth(), enabled = !state.isSpinning)
                    }
                }
            } else {
                val reserved = 64.dp + 76.dp + 24.dp + 64.dp + topBarHeight // ribbon, chip, button, paddings
                val wheelSize = minOf(maxWidth - 24.dp, maxHeight - reserved, 860.dp).coerceAtLeast(200.dp)
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    RibbonBanner(if (state.isSpinning) "Spinning…" else "Spin the Wheel!")
                    GiftsLeftChip(remainingCount, game.gifts.size)
                    WheelBox(wheelGifts.size, wheelSize, wheelGifts, rotation)
                    ChristmasButton(
                        spinLabel,
                        actions.onSpin,
                        Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(bottom = 12.dp),
                        enabled = !state.isSpinning,
                    )
                }
            }
        }
    }

    when (pendingConfirm) {
        PendingConfirm.Reset -> ConfirmDialog(
            title = "Reset game?",
            message = "All gifts go back on the wheel and your selection history is cleared.",
            confirmText = "Reset",
            onConfirm = { pendingConfirm = null; actions.onReset() },
            onDismiss = { pendingConfirm = null },
        )
        PendingConfirm.Leave -> ConfirmDialog(
            title = "Leave this game?",
            message = "You will return to the gift list and the current round will be discarded.",
            confirmText = "Leave",
            onConfirm = { pendingConfirm = null; actions.onLeave() },
            onDismiss = { pendingConfirm = null },
        )
        null -> Unit
    }
}

@Composable
private fun WheelBox(
    count: Int,
    size: Dp,
    gifts: List<GiftItem>,
    rotation: Animatable<Float, *>,
) {
    Box(Modifier.semantics { contentDescription = "Roulette wheel with $count remaining gifts" }) {
        RouletteWheel(gifts = gifts, rotationProvider = { rotation.value }, size = size)
    }
}

@Composable
private fun TopBar(
    enabled: Boolean,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, enabled = enabled, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to gift list",
                tint = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onReset, enabled = enabled, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "Reset game",
                tint = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun GiftsLeftChip(remaining: Int, total: Int) {
    Box(
        Modifier
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            "Gifts left: $remaining of $total",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
