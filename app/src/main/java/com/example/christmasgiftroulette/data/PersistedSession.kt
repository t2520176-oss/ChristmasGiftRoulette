package com.example.christmasgiftroulette.data

import com.example.christmasgiftroulette.game.GameState
import com.example.christmasgiftroulette.game.RouletteEngine
import com.example.christmasgiftroulette.model.CurrencyType
import com.example.christmasgiftroulette.model.GiftDraft
import com.example.christmasgiftroulette.model.GiftItem
import java.math.BigDecimal
import org.json.JSONArray
import org.json.JSONObject

/** Which screen the app is on. Persisted so the session resumes where it was left. */
enum class GamePhase { SETUP, ROULETTE, RESULT, COMPLETE }

/** Everything that survives closing the app: gift setup, remaining/selected ids, history. */
data class PersistedSession(
    val phase: GamePhase,
    val defaultCurrency: CurrencyType,
    val drafts: List<GiftDraft>,
    val game: GameState,
)

/** JSON (de)serialisation; any malformed payload simply yields null so the app starts fresh. */
object SessionSerializer {

    fun encode(session: PersistedSession): String {
        val root = JSONObject()
        root.put("phase", session.phase.name)
        root.put("defaultCurrency", session.defaultCurrency.code)
        root.put("drafts", JSONArray().apply {
            session.drafts.forEach { d ->
                put(JSONObject().put("id", d.id).put("name", d.name).put("amount", d.amountText).put("currency", d.currency.code))
            }
        })
        val game = session.game
        root.put("game", JSONObject().apply {
            put("gifts", JSONArray().apply {
                game.gifts.forEach { g ->
                    put(
                        JSONObject().put("id", g.id).put("name", g.name)
                            .put("amount", g.amount?.toPlainString() ?: JSONObject.NULL)
                            .put("currency", g.currency.code),
                    )
                }
            })
            put("remaining", JSONArray(game.remainingGifts.map { it.id }))
            put("selected", JSONArray(game.selectedGifts.map { it.id }))
            put("winner", game.currentWinner?.id ?: JSONObject.NULL)
        })
        return root.toString()
    }

    fun decode(json: String?): PersistedSession? {
        if (json.isNullOrBlank()) return null
        return try {
            val root = JSONObject(json)
            val drafts = root.getJSONArray("drafts").objects().map {
                GiftDraft(
                    id = it.getString("id"),
                    name = it.getString("name"),
                    amountText = it.getString("amount"),
                    currency = CurrencyType.fromCode(it.getString("currency")),
                )
            }
            val gameJson = root.getJSONObject("game")
            val gifts = gameJson.getJSONArray("gifts").objects().map {
                GiftItem(
                    id = it.getString("id"),
                    name = it.getString("name"),
                    amount = if (it.isNull("amount")) null else BigDecimal(it.getString("amount")),
                    currency = CurrencyType.fromCode(it.getString("currency")),
                )
            }
            val game = if (gifts.isEmpty()) {
                GameState()
            } else {
                RouletteEngine.restore(
                    gifts = gifts,
                    remainingIds = gameJson.getJSONArray("remaining").strings(),
                    selectedIds = gameJson.getJSONArray("selected").strings(),
                    winnerId = if (gameJson.isNull("winner")) null else gameJson.getString("winner"),
                ) ?: return null
            }
            val phase = runCatching { GamePhase.valueOf(root.getString("phase")) }.getOrDefault(GamePhase.SETUP)
            PersistedSession(
                phase = sanitize(phase, game),
                defaultCurrency = CurrencyType.fromCode(root.optString("defaultCurrency")),
                drafts = drafts,
                game = game,
            )
        } catch (e: Exception) {
            null
        }
    }

    /** A screen is only restorable if the data it needs is present. */
    private fun sanitize(phase: GamePhase, game: GameState): GamePhase = when {
        game.gifts.isEmpty() -> GamePhase.SETUP
        phase == GamePhase.RESULT && game.currentWinner == null -> GamePhase.ROULETTE
        phase == GamePhase.COMPLETE && !game.isCompleted -> GamePhase.ROULETTE
        phase == GamePhase.ROULETTE && game.remainingGifts.isEmpty() -> GamePhase.COMPLETE
        else -> phase
    }

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
    private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }
}
