package com.example.christmasgiftroulette.game

import com.example.christmasgiftroulette.model.GiftItem
import kotlin.random.Random

/**
 * Immutable game state.
 *
 * Invariant: [remainingGifts] and [selectedGifts] are disjoint (by id) and together equal [gifts].
 */
data class GameState(
    val gifts: List<GiftItem> = emptyList(),
    val remainingGifts: List<GiftItem> = emptyList(),
    val selectedGifts: List<GiftItem> = emptyList(),
    val currentWinner: GiftItem? = null,
) {
    val isCompleted: Boolean get() = gifts.isNotEmpty() && remainingGifts.isEmpty()
}

/** Pure, deterministic-when-seeded roulette rules. No Android dependencies. */
object RouletteEngine {

    fun newGame(gifts: List<GiftItem>): GameState =
        GameState(gifts = gifts, remainingGifts = gifts, selectedGifts = emptyList(), currentWinner = null)

    /** Picks uniformly from the REMAINING gifts only (never from all gifts). */
    fun pickWinner(state: GameState, random: Random = Random.Default): GiftItem? {
        val pool = state.remainingGifts
        if (pool.isEmpty()) return null
        return pool[random.nextInt(pool.size)]
    }

    /**
     * Marks the gift with [giftId] as selected: removes it from the remaining pool and appends it
     * to the selection history. Ids that are not in the pool (e.g. already selected) are ignored,
     * which is what guarantees a gift can never win twice.
     */
    fun select(state: GameState, giftId: String): GameState {
        val winner = state.remainingGifts.firstOrNull { it.id == giftId } ?: return state
        return state.copy(
            remainingGifts = state.remainingGifts.filterNot { it.id == giftId },
            selectedGifts = state.selectedGifts + winner,
            currentWinner = winner,
        )
    }

    /** Restores every gift to the pool and clears the history ("Play again" / reset). */
    fun restart(state: GameState): GameState = newGame(state.gifts)

    /**
     * Rebuilds a state from persisted ids. Returns null when the data is inconsistent so callers
     * can fall back to the setup screen instead of running a corrupt game.
     */
    fun restore(
        gifts: List<GiftItem>,
        remainingIds: List<String>,
        selectedIds: List<String>,
        winnerId: String?,
    ): GameState? {
        val byId = gifts.associateBy { it.id }
        if (byId.size != gifts.size) return null
        val remaining = remainingIds.map { byId[it] ?: return null }
        val selected = selectedIds.map { byId[it] ?: return null }
        val allIds = (remaining + selected).map { it.id }
        if (allIds.size != gifts.size || allIds.toSet().size != gifts.size) return null
        return GameState(gifts, remaining, selected, winnerId?.let { byId[it] })
    }
}
