package com.lifeyourchoice.core

import com.lifeyourchoice.core.engine.GameEngine
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.story.StoryContent
import com.lifeyourchoice.core.story.StoryLibrary
import kotlin.random.Random

object TestSupport {
    val library: StoryLibrary by lazy { StoryContent.library() }

    enum class Style { RANDOM, FIRST, LAST, CAUTIOUS_PICK, BOLD_PICK, BALANCED, MONEY_FIRST, FAMILY_FIRST, RISK_AVERSE }

    class Result(val engine: GameEngine, val steps: Int, val playedIds: List<String>)

    /** Plays a whole life headlessly. Fails the test on any engine exception or runaway loop. */
    fun playLife(seed: Long, style: Style = Style.RANDOM, gender: Gender = if (seed % 2 == 0L) Gender.BOY else Gender.GIRL): Result {
        val engine = GameEngine.newLife(library, "Tester$seed", gender, (seed % 3).toInt(), seed)
        val rng = Random(seed * 7 + 1)
        var steps = 0
        while (!engine.isFinished) {
            val p = engine.present() ?: error("no scenario but life not finished (seed=$seed step=$steps age=${engine.state.ageYears})")
            val enabled = p.choices.filter { it.enabled }
            check(enabled.size >= 2) { "scenario ${p.id} offers ${enabled.size} enabled choices (seed=$seed)" }
            check(p.choices.size <= 6) { "scenario ${p.id} offers ${p.choices.size} choices" }
            val pick = when (style) {
                Style.RANDOM -> enabled[rng.nextInt(enabled.size)]
                Style.FIRST -> enabled.first()
                Style.LAST -> enabled.last()
                Style.CAUTIOUS_PICK -> enabled[minOf(1, enabled.size - 1)]
                Style.BOLD_PICK -> enabled[minOf(3, enabled.size - 1) % enabled.size]
                else -> bestByLookahead(engine, enabled.map { it.index }, style, rng).let { idx -> enabled.first { it.index == idx } }
            }
            val out = engine.choose(pick.index)
            check(out.result.isNotBlank()) { "blank result in ${p.id}" }
            engine.acknowledgeOutcome()
            steps++
            check(steps < 400) { "life did not end (seed=$seed, age=${engine.state.ageYears})" }
        }
        return Result(engine, steps, engine.state.playedOrder.toList())
    }

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun weights(style: Style): Map<Stat, Double> {
        return when (style) {
            Style.MONEY_FIRST -> mapOf(Stat.MONEY to 3.0, Stat.CAREER to 3.0, Stat.REPUTATION to 1.0, Stat.KNOWLEDGE to 1.0, Stat.HEALTH to 0.2)
            Style.FAMILY_FIRST -> mapOf(Stat.FAMILY to 3.0, Stat.FRIENDSHIP to 1.5, Stat.HAPPINESS to 2.0, Stat.HEALTH to 1.0, Stat.MONEY to 0.5)
            else -> Stat.values().associateWith { 1.0 }
        }
    }

    /** Looks one decision ahead on a cloned life and picks the choice that best serves [style]'s goals. */
    private fun bestByLookahead(engine: GameEngine, indexes: List<Int>, style: Style, rng: Random): Int {
        val w = weights(style)
        var best = indexes.first(); var bestScore = Double.NEGATIVE_INFINITY
        for (i in indexes) {
            val copy = json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), engine.state))
            val out = GameEngine(copy, library).choose(i)
            var score = out.deltas.sumOf { (w[it.stat] ?: 0.5) * it.delta } + rng.nextDouble() * 2
            if (style == Style.RISK_AVERSE) score = out.deltas.sumOf { d -> if (d.delta > 0) d.delta * 1.0 else d.delta * 2.5 } + rng.nextDouble()
            if (score > bestScore) { bestScore = score; best = i }
        }
        return best
    }

    fun freshState(gender: Gender = Gender.BOY, seed: Long = 1): GameState =
        GameEngine.newLife(library, "Alex", gender, 0, seed).state
}
