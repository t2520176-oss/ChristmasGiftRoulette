package com.lifeyourchoice.core.engine

import com.lifeyourchoice.core.model.Category
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.story.Scenario
import com.lifeyourchoice.core.story.StoryLibrary
import kotlin.random.Random

/**
 * Decides which scenario comes next. Version 1 ships [RuleBasedDirector] (fully offline).
 * A future "AI Story Director" can implement this interface (and fall back to the rule-based one),
 * generating personalised situations while the engine, saves and UI stay unchanged.
 */
interface StoryDirector {
    fun next(state: GameState, library: StoryLibrary, rng: Random): Scenario?
}

class RuleBasedDirector(
    private val randomEventChance: Int = 17
) : StoryDirector {

    override fun next(state: GameState, library: StoryLibrary, rng: Random): Scenario? {
        // 1. Explicit follow-up branches always come first.
        while (state.queue.isNotEmpty()) {
            library[state.queue.removeAt(0)]?.let { return it }
        }

        // 2. Delayed consequences that have come due (and still make sense).
        val due = state.scheduled.filter { it.dueMonths <= state.ageMonths }.sortedBy { it.dueMonths }
        for (d in due) {
            val sc = library[d.scenarioId]
            state.scheduled.remove(d)
            if (sc != null && (sc.repeatable || sc.id !in state.seen) && sc.requires.all { it.test(state) }) return sc
        }

        // 3. Eligible pool.
        val recent = state.playedOrder.takeLast(14).toSet()
        val pool = library.all.filter { sc ->
            !sc.branchOnly &&
                state.ageYears in sc.minAge..sc.maxAge &&
                (sc.id !in state.seen) &&
                (!sc.repeatable || sc.id !in recent) &&
                sc.requires.all { it.test(state) }
        }
        val repeatablePool = library.all.filter { sc ->
            sc.repeatable && !sc.branchOnly && sc.id !in recent && state.ageYears in sc.minAge..sc.maxAge &&
                sc.requires.all { it.test(state) } && sc.id in state.seen
        }
        val candidates = pool + repeatablePool
        if (candidates.isEmpty()) {
            // Last resort: any everyday filler that fits, even if seen recently, so a life never stalls.
            val fillers = library.all.filter { sc ->
                sc.repeatable && !sc.branchOnly && state.ageYears in sc.minAge..sc.maxAge && sc.requires.all { it.test(state) }
            }
            return if (fillers.isEmpty()) null else fillers[rng.nextInt(fillers.size)]
        }

        // 4. Milestones and payoffs (priority) before everything else.
        val top = candidates.maxOf { it.priority }
        if (top > 0) return weightedPick(candidates.filter { it.priority == top }, state, rng)

        // 5. Occasional random event; the player's choices stay the main influence.
        val randoms = candidates.filter { it.category == Category.RANDOM }
        if (randoms.isNotEmpty() && state.lastCategory != Category.RANDOM && rng.nextInt(100) < randomEventChance) {
            return weightedPick(randoms, state, rng)
        }
        val regular = candidates.filter { it.category != Category.RANDOM }.ifEmpty { candidates }
        return weightedPick(regular, state, rng)
    }

    private fun weightedPick(list: List<Scenario>, state: GameState, rng: Random): Scenario {
        val weights = list.map { weightOf(it, state) }
        val total = weights.sum()
        var roll = rng.nextInt(total.coerceAtLeast(1))
        for ((i, w) in weights.withIndex()) {
            roll -= w
            if (roll < 0) return list[i]
        }
        return list.last()
    }

    private fun weightOf(sc: Scenario, state: GameState): Int {
        var w = sc.weight + sc.boosts.filter { it.first.test(state) }.sumOf { it.second }
        if (sc.category == state.lastCategory) w = (w * 0.35).toInt()
        // Gently favour scenarios that fit the player's current life stage window centre.
        return w.coerceAtLeast(1)
    }
}
