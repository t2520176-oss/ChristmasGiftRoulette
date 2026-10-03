package com.example.christmasgiftroulette.viewmodel

import com.example.christmasgiftroulette.data.GamePhase
import com.example.christmasgiftroulette.game.GameState
import com.example.christmasgiftroulette.model.CurrencyType
import com.example.christmasgiftroulette.model.GiftDraft

data class SetupUiState(
    val rows: List<GiftDraft> = emptyList(),
    /** Currency given to newly added rows. */
    val defaultCurrency: CurrencyType = CurrencyType.DEFAULT,
    /** Turns on once the user tried to start, so empty fields are flagged. */
    val showErrors: Boolean = false,
) {
    val giftCount: Int get() = rows.size
}

/** A pending spin. The winner is chosen up-front; the wheel animation is computed to land on it. */
data class SpinRequest(
    val token: Long,
    val winnerId: String,
    val extraTurns: Int,
    val offsetFraction: Float,
)

data class UserMessage(val id: Long, val text: String)

data class GiftRouletteUiState(
    val isLoaded: Boolean = false,
    val phase: GamePhase = GamePhase.SETUP,
    val setup: SetupUiState = SetupUiState(),
    /** allGifts / remainingGifts / selectedGifts / currentWinner live in [GameState]. */
    val game: GameState = GameState(),
    val isSpinning: Boolean = false,
    val spinRequest: SpinRequest? = null,
    /** Resting rotation of the wheel in degrees [0, 360). */
    val wheelRotation: Float = 0f,
    val message: UserMessage? = null,
) {
    val gameCompleted: Boolean get() = game.isCompleted
}
