package com.example.christmasgiftroulette.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.christmasgiftroulette.data.AppSettings
import com.example.christmasgiftroulette.data.GamePhase
import com.example.christmasgiftroulette.data.GameRepository
import com.example.christmasgiftroulette.data.PersistedSession
import com.example.christmasgiftroulette.data.SettingsRepository
import com.example.christmasgiftroulette.data.appDataStore
import com.example.christmasgiftroulette.game.GameRules
import com.example.christmasgiftroulette.game.GameState
import com.example.christmasgiftroulette.game.GiftValidator
import com.example.christmasgiftroulette.game.RouletteEngine
import com.example.christmasgiftroulette.game.WheelMath
import com.example.christmasgiftroulette.model.CurrencyType
import com.example.christmasgiftroulette.model.GiftDraft
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns the complete game state; Composables only render it and forward events. */
class GiftRouletteViewModel(application: Application) : AndroidViewModel(application) {

    private val gameRepository = GameRepository(application.appDataStore)
    private val settingsRepository = SettingsRepository(application.appDataStore)
    private val random: Random = Random.Default

    private val _uiState = MutableStateFlow(GiftRouletteUiState())
    val uiState: StateFlow<GiftRouletteUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private var spinCounter = 0L
    private var messageCounter = 0L

    init {
        viewModelScope.launch {
            val saved = gameRepository.load()
            _uiState.update { current ->
                if (saved == null) {
                    current.copy(isLoaded = true, setup = SetupUiState(rows = List(DEFAULT_COUNT) { newDraft(CurrencyType.DEFAULT) }))
                } else {
                    val rows = saved.drafts.ifEmpty { List(DEFAULT_COUNT) { newDraft(saved.defaultCurrency) } }
                    current.copy(
                        isLoaded = true,
                        phase = saved.phase,
                        game = saved.game,
                        setup = SetupUiState(rows = rows, defaultCurrency = saved.defaultCurrency),
                    )
                }
            }
            observeAndPersist()
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeAndPersist() {
        _uiState
            .filter { it.isLoaded }
            .map { PersistedSession(it.phase, it.setup.defaultCurrency, it.setup.rows, it.game) }
            .distinctUntilChanged()
            .debounce(PERSIST_DEBOUNCE_MS)
            .collect { gameRepository.save(it) }
    }

    // ---- Setup -----------------------------------------------------------------------------

    fun setGiftCount(count: Int) = updateSetup { s ->
        val target = count.coerceIn(GameRules.MIN_GIFTS, GameRules.MAX_GIFTS)
        when {
            target > s.rows.size -> s.copy(rows = s.rows + List(target - s.rows.size) { newDraft(s.defaultCurrency) })
            target < s.rows.size -> s.copy(rows = s.rows.take(target))
            else -> s
        }
    }

    fun incrementGiftCount() {
        val count = _uiState.value.setup.giftCount
        if (count >= GameRules.MAX_GIFTS) postMessage("A game can have at most ${GameRules.MAX_GIFTS} gifts") else setGiftCount(count + 1)
    }

    fun decrementGiftCount() {
        val count = _uiState.value.setup.giftCount
        if (count <= GameRules.MIN_GIFTS) postMessage("You need at least ${GameRules.MIN_GIFTS} gifts") else setGiftCount(count - 1)
    }

    fun addGift() = incrementGiftCount()

    fun removeGift(id: String) {
        if (_uiState.value.setup.giftCount <= GameRules.MIN_GIFTS) {
            postMessage("You need at least ${GameRules.MIN_GIFTS} gifts")
            return
        }
        updateSetup { s -> s.copy(rows = s.rows.filterNot { it.id == id }) }
    }

    fun updateGiftName(id: String, name: String) =
        updateRow(id) { it.copy(name = name.take(GameRules.MAX_NAME_LENGTH)) }

    /** Sets a gift's optional value and currency together; the chosen currency becomes the default for new gifts. */
    fun setGiftValue(id: String, amountText: String, currency: CurrencyType) = updateSetup { s ->
        s.copy(
            defaultCurrency = currency,
            rows = s.rows.map { if (it.id == id) it.copy(amountText = amountText.take(MAX_AMOUNT_TEXT), currency = currency) else it },
        )
    }

    /** Tap-to-add suggestion: fills the first unnamed row, otherwise appends a new row. */
    fun addSuggestedGift(name: String) {
        val setup = _uiState.value.setup
        val blank = setup.rows.firstOrNull { it.name.isBlank() }
        if (blank != null) {
            updateGiftName(blank.id, name)
        } else if (setup.giftCount >= GameRules.MAX_GIFTS) {
            postMessage("A game can have at most ${GameRules.MAX_GIFTS} gifts")
        } else {
            updateSetup { it.copy(rows = it.rows + newDraft(it.defaultCurrency).copy(name = name)) }
        }
    }

    fun startRoulette() {
        val state = _uiState.value
        if (state.phase != GamePhase.SETUP) return
        val result = GiftValidator.validate(state.setup.rows)
        if (!result.isValid) {
            _uiState.update { it.copy(setup = it.setup.copy(showErrors = true)) }
            postMessage(result.generalError ?: "Please fix the highlighted gifts before starting")
            return
        }
        _uiState.update {
            it.copy(
                phase = GamePhase.ROULETTE,
                game = RouletteEngine.newGame(result.gifts),
                isSpinning = false,
                spinRequest = null,
                wheelRotation = 0f,
                setup = it.setup.copy(showErrors = false),
            )
        }
    }

    // ---- Roulette --------------------------------------------------------------------------

    /** Ignored while a spin is running, so rapid taps can never start a second spin. */
    fun spin() {
        val state = _uiState.value
        if (state.phase != GamePhase.ROULETTE || state.isSpinning || state.spinRequest != null) return
        val winner = RouletteEngine.pickWinner(state.game, random) ?: return
        val request = SpinRequest(
            token = ++spinCounter,
            winnerId = winner.id,
            extraTurns = random.nextInt(5, 8),
            offsetFraction = random.nextFloat() * 0.7f - 0.35f,
        )
        _uiState.update { it.copy(isSpinning = true, spinRequest = request) }
    }

    /** Called by the wheel once its animation has come to rest on the winner. */
    fun onSpinFinished(token: Long, finalRotation: Float) {
        _uiState.update { s ->
            val request = s.spinRequest
            if (request == null || request.token != token) {
                s
            } else {
                s.copy(
                    game = RouletteEngine.select(s.game, request.winnerId),
                    isSpinning = false,
                    spinRequest = null,
                    wheelRotation = WheelMath.normalize(finalRotation),
                    phase = GamePhase.RESULT,
                )
            }
        }
    }

    // ---- Result / completion ---------------------------------------------------------------

    fun nextPick() {
        val s = _uiState.value
        if (s.phase != GamePhase.RESULT || s.game.remainingGifts.isEmpty()) return
        _uiState.update { it.copy(phase = GamePhase.ROULETTE) }
    }

    fun showSummary() {
        val s = _uiState.value
        if (s.phase == GamePhase.RESULT && s.game.isCompleted) _uiState.update { it.copy(phase = GamePhase.COMPLETE) }
    }

    /** Restores every gift and starts another round with the same list. */
    fun playAgain() {
        if (_uiState.value.isSpinning) return
        _uiState.update {
            it.copy(
                phase = GamePhase.ROULETTE,
                game = RouletteEngine.restart(it.game),
                spinRequest = null,
                wheelRotation = 0f,
            )
        }
    }

    /** Alias used by the in-game reset action; behaves exactly like [playAgain]. */
    fun resetGame() = playAgain()

    /** Back to the setup screen, keeping the gift list for editing. */
    fun newGiftList() {
        if (_uiState.value.isSpinning) return
        _uiState.update {
            it.copy(phase = GamePhase.SETUP, game = GameState(), spinRequest = null, wheelRotation = 0f)
        }
    }

    // ---- Settings & messages ---------------------------------------------------------------

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVibrationEnabled(enabled) }
    }

    fun setConfirmBeforeReset(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setConfirmBeforeReset(enabled) }
    }

    fun consumeMessage(id: Long) {
        _uiState.update { if (it.message?.id == id) it.copy(message = null) else it }
    }

    // ---- Helpers ---------------------------------------------------------------------------

    private fun postMessage(text: String) {
        _uiState.update { it.copy(message = UserMessage(++messageCounter, text)) }
    }

    private fun updateSetup(transform: (SetupUiState) -> SetupUiState) {
        _uiState.update { it.copy(setup = transform(it.setup)) }
    }

    private fun updateRow(id: String, transform: (GiftDraft) -> GiftDraft) = updateSetup { s ->
        s.copy(rows = s.rows.map { if (it.id == id) transform(it) else it })
    }

    private fun newDraft(currency: CurrencyType) = GiftDraft(id = UUID.randomUUID().toString(), currency = currency)

    private companion object {
        const val DEFAULT_COUNT = 5
        const val PERSIST_DEBOUNCE_MS = 250L
        const val MAX_AMOUNT_TEXT = 20
    }
}
