package com.example.christmasgiftroulette.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.christmasgiftroulette.data.GamePhase
import com.example.christmasgiftroulette.feedback.FeedbackController
import com.example.christmasgiftroulette.ui.result.CompletionActions
import com.example.christmasgiftroulette.ui.result.CompletionScreen
import com.example.christmasgiftroulette.ui.result.ResultActions
import com.example.christmasgiftroulette.ui.result.ResultScreen
import com.example.christmasgiftroulette.ui.roulette.RouletteActions
import com.example.christmasgiftroulette.ui.roulette.RouletteScreen
import com.example.christmasgiftroulette.ui.settings.SettingsDialog
import com.example.christmasgiftroulette.ui.setup.SetupActions
import com.example.christmasgiftroulette.ui.setup.SetupScreen
import com.example.christmasgiftroulette.viewmodel.GiftRouletteViewModel

object Routes {
    const val SETUP = "setup"
    const val ROULETTE = "roulette"
    const val RESULT = "result"
    const val COMPLETE = "complete"
}

private fun GamePhase.route(): String = when (this) {
    GamePhase.SETUP -> Routes.SETUP
    GamePhase.ROULETTE -> Routes.ROULETTE
    GamePhase.RESULT -> Routes.RESULT
    GamePhase.COMPLETE -> Routes.COMPLETE
}

/**
 * Navigation Compose host. The ViewModel's [GamePhase] is the single source of truth: whenever it
 * changes, the NavController is moved to the matching destination (replacing the back stack, so
 * system Back never walks through earlier phases).
 */
@Composable
fun AppNavigation(
    viewModel: GiftRouletteViewModel,
    feedback: FeedbackController,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val navController = rememberNavController()
    val startRoute = remember { state.phase.route() }

    LaunchedEffect(state.phase) {
        val target = state.phase.route()
        if (navController.currentDestination?.route != target) {
            navController.navigate(target) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val setupActions = remember(viewModel) {
        SetupActions(
            onSetCount = viewModel::setGiftCount,
            onIncrement = viewModel::incrementGiftCount,
            onDecrement = viewModel::decrementGiftCount,
            onAddGift = viewModel::addGift,
            onRemoveGift = viewModel::removeGift,
            onNameChange = viewModel::updateGiftName,
            onQuickGift = viewModel::addSuggestedGift,
            onIconChange = viewModel::setGiftIcon,
            onValueChange = viewModel::setGiftValue,
            onStart = viewModel::startRoulette,
            onOpenSettings = { showSettings = true },
            onMessageConsumed = viewModel::consumeMessage,
        )
    }
    val rouletteActions = remember(viewModel) {
        RouletteActions(
            onSpin = viewModel::spin,
            onSpinFinished = viewModel::onSpinFinished,
            onLeave = viewModel::newGiftList,
            onReset = viewModel::resetGame,
            onOpenSettings = { showSettings = true },
        )
    }
    val resultActions = remember(viewModel) {
        ResultActions(
            onNextPick = viewModel::nextPick,
            onShowSummary = viewModel::showSummary,
            onReset = viewModel::resetGame,
            onOpenSettings = { showSettings = true },
        )
    }
    val completionActions = remember(viewModel) {
        CompletionActions(
            onPlayAgain = viewModel::playAgain,
            onNewGiftList = viewModel::newGiftList,
            onOpenSettings = { showSettings = true },
        )
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        modifier = modifier,
        enterTransition = { fadeIn(tween(350)) },
        exitTransition = { fadeOut(tween(250)) },
        popEnterTransition = { fadeIn(tween(350)) },
        popExitTransition = { fadeOut(tween(250)) },
    ) {
        composable(Routes.SETUP) { SetupScreen(state, setupActions) }
        composable(Routes.ROULETTE) { RouletteScreen(state, settings, feedback, rouletteActions) }
        composable(Routes.RESULT) { ResultScreen(state, settings, feedback, resultActions) }
        composable(Routes.COMPLETE) { CompletionScreen(state, settings, feedback, completionActions) }
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onSoundChange = viewModel::setSoundEnabled,
            onVibrationChange = viewModel::setVibrationEnabled,
            onConfirmResetChange = viewModel::setConfirmBeforeReset,
            onDismiss = { showSettings = false },
        )
    }
}
