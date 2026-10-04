package com.lifeyourchoice.core.model

import kotlinx.serialization.Serializable

/** A person in the player's life who remembers how they were treated. */
@Serializable
class NpcState(
    val role: NpcRole,
    val name: String,
    val gender: Gender,
    var trust: Int = 50
)

@Serializable
class HistoryEntry(
    val ageYears: Int,
    val scenarioId: String,
    val choiceText: String,
    val result: String
)

/** An entry for the "Important choices" list in the final life report. */
@Serializable
class Milestone(val ageYears: Int, val text: String)

/** A delayed consequence that becomes eligible once the player reaches [dueMonths]. */
@Serializable
class Scheduled(val scenarioId: String, val dueMonths: Int)

@Serializable
class StatDelta(val stat: Stat, val delta: Int)

/** What the consequence screen shows after a decision. */
@Serializable
class ChoiceOutcome(
    val scenarioId: String,
    val choiceText: String,
    val result: String,
    val deltas: List<StatDelta>,
    /** Shows "Your decision may have consequences later…". */
    val hint: Boolean,
    /** Short "time passes" lines (from the life rules). */
    val notes: List<String> = emptyList(),
    val newAchievements: List<String> = emptyList()
)

/**
 * Complete state of one life. Plain mutable data so the engine stays simple; it is serialised as-is
 * by the save system (see [com.lifeyourchoice.core.save.SaveRepository]).
 */
@Serializable
class GameState(
    val seed: Long,
    var playerName: String,
    val gender: Gender,
    val appearance: Int,
    var ageMonths: Int,
    val stats: MutableMap<Stat, Int> = mutableMapOf(),
    val traits: MutableMap<Trait, Int> = mutableMapOf(),
    val flags: MutableSet<String> = mutableSetOf(),
    val counters: MutableMap<String, Int> = mutableMapOf(),
    val npcs: MutableMap<NpcRole, NpcState> = mutableMapOf(),
    var career: CareerTrack = CareerTrack.NONE,
    var careerLevel: Int = 0,
    /** The most recent job held, kept for the life report after retirement or a sold business. */
    var lastCareer: CareerTrack = CareerTrack.NONE,
    var lastCareerLevel: Int = 0,
    var education: Education = Education.HIGH_SCHOOL,
    var relationship: RelationshipStatus = RelationshipStatus.SINGLE,
    var children: Int = 0,
    /** 0 none, 1 startup, 2 running, 3 thriving. */
    var businessStage: Int = 0,
    val seen: MutableSet<String> = mutableSetOf(),
    val history: MutableList<HistoryEntry> = mutableListOf(),
    val milestones: MutableList<Milestone> = mutableListOf(),
    val scheduled: MutableList<Scheduled> = mutableListOf(),
    val queue: MutableList<String> = mutableListOf(),
    var currentScenarioId: String? = null,
    var lastCategory: Category? = null,
    var pendingOutcome: ChoiceOutcome? = null,
    var plannedEndAge: Int = 80,
    var step: Int = 0,
    var finished: Boolean = false,
    val recentEvents: MutableList<String> = mutableListOf(),
    val achievementsThisLife: MutableSet<String> = mutableSetOf(),
    /** Scenario ids that have been played, in order (used for variety and for tests). */
    val playedOrder: MutableList<String> = mutableListOf()
) {
    val ageYears: Int get() = ageMonths / 12
    val chapter: Chapter get() = Chapter.forAge(ageYears)

    fun stat(s: Stat): Int = stats[s] ?: 0

    fun setStat(s: Stat, value: Int) {
        stats[s] = value.coerceIn(0, 100)
    }

    fun change(s: Stat, delta: Int) = setStat(s, stat(s) + delta)

    fun trait(t: Trait): Int = traits[t] ?: 0

    fun bump(t: Trait, delta: Int) {
        traits[t] = ((traits[t] ?: 0) + delta).coerceIn(0, 100)
    }

    fun counter(name: String): Int = counters[name] ?: 0

    fun addCounter(name: String, delta: Int) {
        counters[name] = ((counters[name] ?: 0) + delta).coerceAtLeast(0)
    }

    fun has(flag: String): Boolean = flag in flags

    fun npc(role: NpcRole): NpcState = npcs.getValue(role)

    fun trust(role: NpcRole): Int = npcs[role]?.trust ?: 50

    fun adjustTrust(role: NpcRole, delta: Int) {
        npcs[role]?.let { it.trust = (it.trust + delta).coerceIn(0, 100) }
    }

    val employed: Boolean get() = career != CareerTrack.NONE && careerLevel > 0

    val jobTitle: String
        get() = if (employed) career.titleAt(careerLevel) else if (ageYears < 18) "Student" else "Job seeker"

    fun addMilestone(text: String) {
        if (milestones.none { it.text == text }) milestones.add(Milestone(ageYears, text))
    }

    fun remember(line: String) {
        recentEvents.add(line)
        while (recentEvents.size > 6) recentEvents.removeAt(0)
    }
}
