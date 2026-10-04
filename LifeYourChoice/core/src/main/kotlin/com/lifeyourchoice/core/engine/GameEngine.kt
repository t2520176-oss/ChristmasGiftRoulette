package com.lifeyourchoice.core.engine

import com.lifeyourchoice.core.model.Category
import com.lifeyourchoice.core.model.ChoiceOutcome
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.HistoryEntry
import com.lifeyourchoice.core.model.NpcRole
import com.lifeyourchoice.core.model.NpcState
import com.lifeyourchoice.core.model.SceneArt
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.model.StatDelta
import com.lifeyourchoice.core.model.Trait
import com.lifeyourchoice.core.story.Choice
import com.lifeyourchoice.core.story.EffectContext
import com.lifeyourchoice.core.story.Outcome
import com.lifeyourchoice.core.story.Scenario
import com.lifeyourchoice.core.story.StoryLibrary
import kotlin.random.Random

class PresentedChoice(
    /** Index into the scenario's choice list; pass this to [GameEngine.choose]. */
    val index: Int,
    val text: String,
    val enabled: Boolean,
    val lockedHint: String?,
    val badge: String?
)

class PresentedScenario(
    val id: String,
    val title: String,
    val category: Category,
    val art: SceneArt,
    val text: String,
    val fromPast: Boolean,
    val choices: List<PresentedChoice>
)

/**
 * The game's rule engine. Holds one life ([state]) and moves it forward one decision at a time.
 * It knows nothing about Android; the app only renders what it exposes.
 */
class GameEngine(
    val state: GameState,
    val library: StoryLibrary,
    private val director: StoryDirector = RuleBasedDirector()
) {
    val isFinished: Boolean get() = state.finished

    /** The consequence waiting to be shown (set after a choice, cleared by [acknowledgeOutcome]). */
    val pendingOutcome: ChoiceOutcome? get() = state.pendingOutcome

    fun currentScenario(): Scenario? {
        if (state.finished) return null
        val id = state.currentScenarioId
        val sc = id?.let { library[it] }
        if (sc != null) return sc
        // Content changed since the save, or first call: ask the director.
        val next = director.next(state, library, rngFor(state.step))
        state.currentScenarioId = next?.id
        if (next == null) finish()
        return next
    }

    /** The scenario ready to be played, with text and choices resolved for this life. */
    fun present(): PresentedScenario? {
        if (state.pendingOutcome != null) return null
        val sc = currentScenario() ?: return null
        var text = TextTemplate.render(sc.text, state)
        for ((cond, extra) in sc.extras) if (cond.test(state)) text += "\n\n" + TextTemplate.render(extra, state)
        val choices = sc.choices.withIndex()
            .filter { (_, c) -> c.showIf.all { it.test(state) } }
            .map { (i, c) ->
                PresentedChoice(
                    index = i,
                    text = TextTemplate.render(c.text, state),
                    enabled = c.enableIf.all { it.test(state) },
                    lockedHint = c.lockedHint?.let { TextTemplate.render(it, state) },
                    badge = c.badge?.let { TextTemplate.render(it, state) }
                )
            }
        return PresentedScenario(sc.id, sc.title, sc.category, sc.art, text, sc.fromPast, choices)
    }

    /** Resolves the player's decision. [choiceIndex] is [PresentedChoice.index]. */
    fun choose(choiceIndex: Int): ChoiceOutcome {
        check(!state.finished) { "The life is over." }
        check(state.pendingOutcome == null) { "Acknowledge the previous outcome first." }
        val sc = checkNotNull(currentScenario()) { "No scenario to play." }
        val choice: Choice = sc.choices[choiceIndex]
        require(choice.showIf.all { it.test(state) } && choice.enableIf.all { it.test(state) }) { "Choice is not available." }

        val rng = rngFor(state.step)
        val before = snapshot()
        val mistakesBefore = state.counter(Flags.C_MISTAKES)

        val ctx = EffectContext(state, rng)
        choice.effects.forEach { it.apply(ctx) }
        val outcome = pickOutcome(choice, rng)
        outcome?.effects?.forEach { it.apply(ctx) }

        // Engine bookkeeping that content should not have to repeat.
        if (state.employed && !state.has(Flags.FIRST_JOB)) {
            state.flags.add(Flags.FIRST_JOB)
        }
        if (state.employed) {
            state.lastCareer = state.career
            state.lastCareerLevel = state.careerLevel
        }
        val newMistakes = state.counter(Flags.C_MISTAKES) - mistakesBefore
        if (newMistakes > 0 && state.ageYears < 28) state.addCounter(Flags.C_EARLY_MISTAKES, newMistakes)

        val deltas = diff(before, snapshot())
        val resultText = listOfNotNull(choice.result.ifBlank { null }, outcome?.result?.ifBlank { null }) + ctx.notes
        val result = resultText.joinToString("\n\n") { TextTemplate.render(it, state) }
        val choiceText = TextTemplate.render(choice.text, state)

        state.seen.add(sc.id)
        state.playedOrder.add(sc.id)
        state.lastCategory = sc.category
        state.history.add(HistoryEntry(state.ageYears, sc.id, choiceText, result))
        state.remember(shortLine(choiceText))

        // Time passes (not when a follow-up branch is queued: that is the same moment).
        val hadFollowUp = state.queue.isNotEmpty()
        val months = if (hadFollowUp) 0 else (sc.months ?: LifeRules.paceMonths(state.ageYears, rng))
        val notes = LifeRules.advance(state, months, rng)
        state.ageMonths += months
        if (state.ageYears >= 60 && state.counter("end_planned") == 0) planEnd(rng)

        val newAch = evaluateAchievements(atEnd = false)

        // What comes next?
        if (!hadFollowUp && state.ageYears >= state.plannedEndAge) {
            state.currentScenarioId = null
            finish()
        } else {
            state.step++
            val next = director.next(state, library, rngFor(state.step))
            state.currentScenarioId = next?.id
            if (next == null) finish()
        }
        val achievementsNow = if (state.finished) newAch + evaluateAchievements(atEnd = true) else newAch

        val out = ChoiceOutcome(
            scenarioId = sc.id,
            choiceText = choiceText,
            result = result,
            deltas = deltas,
            hint = choice.hint,
            notes = notes,
            newAchievements = achievementsNow
        )
        state.pendingOutcome = out
        return out
    }

    /** Called when the player taps CONTINUE on the consequence screen. */
    fun acknowledgeOutcome() {
        state.pendingOutcome = null
    }

    private fun finish() {
        state.finished = true
        state.currentScenarioId = null
    }

    private fun planEnd(rng: Random) {
        val h = state.stat(Stat.HEALTH)
        var end = state.plannedEndAge + when {
            h >= 75 -> 3
            h <= 35 -> -6
            h <= 50 -> -2
            else -> 0
        }
        end = end.coerceIn(70, 90).coerceAtLeast(state.ageYears + 4)
        state.plannedEndAge = end
        state.addCounter("end_planned", 1)
    }

    private fun evaluateAchievements(atEnd: Boolean): List<String> {
        val fresh = mutableListOf<String>()
        for (a in Achievements.all) {
            if (a.id in state.achievementsThisLife) continue
            if (a.atEnd && !atEnd) continue
            if (a.test(state)) {
                state.achievementsThisLife.add(a.id)
                fresh.add(a.id)
            }
        }
        return fresh
    }

    private fun pickOutcome(choice: Choice, rng: Random): Outcome? {
        val eligible = choice.outcomes.filter { o -> o.requires.all { it.test(state) } }
        if (eligible.isEmpty()) return null
        val weights = eligible.map { o ->
            (o.weight + o.bonuses.filter { it.first.test(state) }.sumOf { it.second }).coerceAtLeast(1)
        }
        var roll = rng.nextInt(weights.sum())
        for ((i, w) in weights.withIndex()) {
            roll -= w
            if (roll < 0) return eligible[i]
        }
        return eligible.last()
    }

    private fun snapshot(): Map<Stat, Int> = Stat.values().associateWith { state.stat(it) }

    private fun diff(before: Map<Stat, Int>, after: Map<Stat, Int>): List<StatDelta> =
        Stat.values().mapNotNull { st ->
            val d = after.getValue(st) - before.getValue(st)
            if (d != 0) StatDelta(st, d) else null
        }

    private fun shortLine(text: String): String = if (text.length <= 60) text else text.take(57).trimEnd() + "…"

    private fun rngFor(step: Int): Random = Random(state.seed * 1_000_003L + step * 7919L + 13)

    companion object {
        private val boyNames = listOf("Marcus", "Ethan", "Leo", "Daniel", "Jake", "Noah", "Ryan", "Caleb", "Omar", "Kenji", "Mateo", "Luca")
        private val girlNames = listOf("Mia", "Sofia", "Chloe", "Hannah", "Priya", "Naomi", "Ava", "Elena", "Grace", "Yuna", "Isabel", "Zoe")

        /** Story that opens every life (falls back to the director when absent from the library). */
        const val INTRO_SCENARIO = "sch_skip_school"

        fun newLife(
            library: StoryLibrary,
            playerName: String,
            gender: Gender,
            appearance: Int,
            seed: Long = Random.nextLong(),
            director: StoryDirector = RuleBasedDirector()
        ): GameEngine {
            val rng = Random(seed)
            val startAge = 14 + rng.nextInt(2)
            val state = GameState(
                seed = seed,
                playerName = playerName.trim().ifEmpty { if (gender == Gender.BOY) "Alex" else "Alexa" },
                gender = gender,
                appearance = appearance,
                ageMonths = startAge * 12 + rng.nextInt(6),
                plannedEndAge = 76 + rng.nextInt(10)
            )
            fun jitter(base: Int) = (base + rng.nextInt(-5, 6)).coerceIn(5, 95)
            state.stats[Stat.HEALTH] = jitter(76)
            state.stats[Stat.KNOWLEDGE] = jitter(50)
            state.stats[Stat.DISCIPLINE] = jitter(48)
            state.stats[Stat.CONFIDENCE] = jitter(50)
            state.stats[Stat.REPUTATION] = jitter(50)
            state.stats[Stat.FAMILY] = jitter(70)
            state.stats[Stat.FRIENDSHIP] = jitter(55)
            state.stats[Stat.MONEY] = jitter(38)
            state.stats[Stat.HAPPINESS] = jitter(65)
            state.stats[Stat.ENERGY] = jitter(80)
            state.stats[Stat.CAREER] = 0
            Trait.values().forEach { state.traits[it] = 0 }
            assignNpcs(state, rng)
            state.queue.add(INTRO_SCENARIO)
            val engine = GameEngine(state, library, director)
            engine.currentScenario()
            return engine
        }

        private fun assignNpcs(state: GameState, rng: Random) {
            val used = mutableSetOf(state.playerName)
            fun draw(gender: Gender): String {
                val pool = (if (gender == Gender.BOY) boyNames else girlNames).filter { it !in used }
                val name = pool[rng.nextInt(pool.size)]
                used += name
                return name
            }
            fun anyGender() = if (rng.nextBoolean()) Gender.BOY else Gender.GIRL
            val opposite = if (state.gender == Gender.BOY) Gender.GIRL else Gender.BOY
            val spec = listOf(
                Triple(NpcRole.BEST_FRIEND, state.gender, 60),
                Triple(NpcRole.UNDERDOG, anyGender(), 40),
                Triple(NpcRole.RIVAL, anyGender(), 30),
                Triple(NpcRole.MENTOR, anyGender(), 40),
                Triple(NpcRole.PARTNER, opposite, 50),
                Triple(NpcRole.SIBLING, anyGender(), 55),
                Triple(NpcRole.BOSS, anyGender(), 45),
                Triple(NpcRole.COWORKER, anyGender(), 50)
            )
            for ((role, gender, trust) in spec) state.npcs[role] = NpcState(role, draw(gender), gender, trust)
        }
    }
}
