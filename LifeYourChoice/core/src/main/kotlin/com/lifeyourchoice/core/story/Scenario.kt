package com.lifeyourchoice.core.story

import com.lifeyourchoice.core.model.Category
import com.lifeyourchoice.core.model.SceneArt

/**
 * One possible result of a choice, picked by weighted chance. The weight is raised by [bonuses]
 * (usually the player's stats, traits or past decisions) so luck never completely decides success.
 */
class Outcome(
    val weight: Int,
    val bonuses: List<Pair<Condition, Int>>,
    val requires: List<Condition>,
    val effects: List<Effect>,
    val result: String
)

class Choice(
    val text: String,
    /** The choice is only shown when all of these hold (used for options unlocked by past decisions). */
    val showIf: List<Condition>,
    /** The choice is shown but locked until these hold (e.g. not enough money). */
    val enableIf: List<Condition>,
    val lockedHint: String?,
    /** Small gold caption such as "Because you helped Daniel years ago". */
    val badge: String?,
    val effects: List<Effect>,
    val outcomes: List<Outcome>,
    val result: String,
    /** Scenario ids this choice can queue or schedule (validated at start-up). */
    val refs: List<String>,
    /** Show "Your decision may have consequences later…". */
    val hint: Boolean
)

class Scenario(
    val id: String,
    val category: Category,
    val title: String,
    val art: SceneArt,
    val text: String,
    /** Extra paragraphs appended when their condition holds (callbacks to earlier decisions). */
    val extras: List<Pair<Condition, String>>,
    val minAge: Int,
    val maxAge: Int,
    val requires: List<Condition>,
    val boosts: List<Pair<Condition, Int>>,
    val weight: Int,
    /** Higher priority scenarios are shown before random ones once eligible (milestones, payoffs). */
    val priority: Int,
    val repeatable: Boolean,
    /** Only reachable through a choice (follow-up branches). */
    val branchOnly: Boolean,
    /** Shows the "a choice from your past" tag. */
    val fromPast: Boolean,
    /** Months that pass after this scenario; null = the chapter's default pace. */
    val months: Int?,
    val choices: List<Choice>
)

/** A bundle of scenarios. Later content packs (or a future AI director) can add more of these. */
interface StoryPack {
    val id: String
    val title: String
    val scenarios: List<Scenario>
}

class SimpleStoryPack(
    override val id: String,
    override val title: String,
    override val scenarios: List<Scenario>
) : StoryPack

/** All scenarios available to the engine, indexed by id, with a validator for content authors. */
class StoryLibrary(val packs: List<StoryPack>) {
    val all: List<Scenario> = packs.flatMap { it.scenarios }
    val byId: Map<String, Scenario> = all.associateBy { it.id }

    operator fun get(id: String): Scenario? = byId[id]

    /** Returns human-readable problems; an empty list means the content is consistent. */
    fun validate(): List<String> {
        val problems = mutableListOf<String>()
        val dupes = all.groupBy { it.id }.filter { it.value.size > 1 }.keys
        dupes.forEach { problems += "Duplicate scenario id: $it" }
        for (s in all) {
            if (s.choices.size !in 3..6) problems += "${s.id}: has ${s.choices.size} choices (expected 3-6)"
            if (s.choices.count { it.showIf.isEmpty() } < 2) problems += "${s.id}: fewer than 2 always-visible choices"
            if (s.text.isBlank()) problems += "${s.id}: empty text"
            for (c in s.choices) {
                if (c.result.isBlank() && c.outcomes.isEmpty()) problems += "${s.id}: choice '${c.text}' has no result"
                for (ref in c.refs) if (ref !in byId) problems += "${s.id}: unknown reference '$ref'"
                if (c.outcomes.isNotEmpty() && c.outcomes.none { it.requires.isEmpty() }) {
                    problems += "${s.id}: choice '${c.text}' has outcomes that may all be ineligible"
                }
            }
            if (s.minAge > s.maxAge) problems += "${s.id}: minAge > maxAge"
        }
        return problems
    }
}
