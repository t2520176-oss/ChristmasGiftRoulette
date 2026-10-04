package com.lifeyourchoice.core.story

import com.lifeyourchoice.core.model.CareerTrack
import com.lifeyourchoice.core.model.Category
import com.lifeyourchoice.core.model.Education
import com.lifeyourchoice.core.model.NpcRole
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.SceneArt
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.model.Trait

/**
 * Authoring DSL for story content. Example:
 *
 * ```
 * scenario("sch_skip", SCHOOL, HALLWAY, 14..17) {
 *     text("{friend} whispers: “Let’s skip school today.”")
 *     choice("Go with {friend.him}.") {
 *         knowledge(-3); happiness(+5); trust(BEST_FRIEND, +3)
 *         result("You spend the day at the mall.")
 *     }
 * }
 * ```
 */
@DslMarker
annotation class StoryDsl

@StoryDsl
open class EffectBuilder {
    internal val effects = mutableListOf<Effect>()
    internal val refs = mutableListOf<String>()

    fun stat(stat: Stat, n: Int) { effects += Effects.stat(stat, n) }
    fun health(n: Int) = stat(Stat.HEALTH, n)
    fun knowledge(n: Int) = stat(Stat.KNOWLEDGE, n)
    fun discipline(n: Int) = stat(Stat.DISCIPLINE, n)
    fun confidence(n: Int) = stat(Stat.CONFIDENCE, n)
    fun reputation(n: Int) = stat(Stat.REPUTATION, n)
    fun family(n: Int) = stat(Stat.FAMILY, n)
    fun friendship(n: Int) = stat(Stat.FRIENDSHIP, n)
    fun money(n: Int) = stat(Stat.MONEY, n)
    fun happiness(n: Int) = stat(Stat.HAPPINESS, n)
    fun energy(n: Int) = stat(Stat.ENERGY, n)
    fun career(n: Int) = stat(Stat.CAREER, n)

    fun trait(trait: Trait, n: Int) { effects += Effects.trait(trait, n) }
    fun flag(vararg flags: String) { flags.forEach { FlagAudit.defined += it; effects += Effects.flag(it) } }
    fun unflag(vararg flags: String) { flags.forEach { FlagAudit.referenced += it; effects += Effects.unflag(it) } }
    fun count(name: String, n: Int = 1) { FlagAudit.counterDefined += name; effects += Effects.counter(name, n) }
    fun trust(role: NpcRole, n: Int) { effects += Effects.trust(role, n) }
    fun milestone(text: String) { effects += Effects.milestone(text) }
    fun note(text: String) { effects += Effects.note(text) }

    /** Show [id] right after this decision (a branch of the same moment). */
    fun then(id: String) { refs += id; effects += Effects.queue(id) }

    /** Make [id] eligible between [minYears] and [maxYears] from now: a delayed consequence. */
    fun schedule(id: String, minYears: Int, maxYears: Int) {
        refs += id
        effects += Effects.schedule(id, minYears, maxYears)
    }

    fun job(track: CareerTrack, level: Int = 1) { effects += Effects.job(track, level) }
    fun promote(levels: Int = 1) { effects += Effects.promote(levels) }
    fun loseJob() { effects += Effects.loseJob }
    fun rehire(levelDelta: Int = 0) { effects += Effects.rehire(levelDelta) }
    fun noJob() { effects += Effects.noJob }
    fun education(e: Education) { effects += Effects.education(e) }
    fun relationship(r: RelationshipStatus) { effects += Effects.relationship(r) }
    fun children(n: Int) { effects += Effects.children(n) }
    fun business(stage: Int) { effects += Effects.business(stage) }

    /** Effects (and notes) that only apply when [cond] holds — e.g. savings softening a loss. */
    fun onlyIf(cond: Condition, block: EffectBuilder.() -> Unit) {
        val inner = EffectBuilder().apply(block)
        refs += inner.refs
        effects += Effects.conditional(cond, inner.effects.toList())
    }
}

@StoryDsl
class OutcomeBuilder(private val baseWeight: Int) : EffectBuilder() {
    private val bonuses = mutableListOf<Pair<Condition, Int>>()
    private val requires = mutableListOf<Condition>()
    private var result = ""

    /** Raises this outcome's chance by [weight] when [cond] holds. */
    fun bonus(cond: Condition, weight: Int) { bonuses += cond to weight }
    fun requires(vararg c: Condition) { requires += c }
    fun result(text: String) { result = text }

    internal fun build() = Outcome(baseWeight, bonuses.toList(), requires.toList(), effects.toList(), result)
}

@StoryDsl
class ChoiceBuilder(private val text: String) : EffectBuilder() {
    private val showIf = mutableListOf<Condition>()
    private val enableIf = mutableListOf<Condition>()
    private var lockedHint: String? = null
    private var badge: String? = null
    private var result = ""
    private var hint = false
    private val outcomes = mutableListOf<Outcome>()

    fun result(text: String) { result = text }

    /** Show "Your decision may have consequences later…" on the consequence screen. */
    fun hint() { hint = true }

    /** Only show this choice when the condition holds (option unlocked by earlier decisions). */
    fun showIf(vararg c: Condition) { showIf += c }

    /** Show but lock this choice until the condition holds. */
    fun enableIf(lockedText: String, vararg c: Condition) {
        enableIf += c
        lockedHint = lockedText
    }

    fun badge(text: String) { badge = text }

    /** One possible result chosen by weighted chance (see [OutcomeBuilder.bonus]). */
    fun outcome(weight: Int, block: OutcomeBuilder.() -> Unit) {
        val b = OutcomeBuilder(weight).apply(block)
        refs += b.refs
        outcomes += b.build()
    }

    internal fun build() = Choice(
        text = text,
        showIf = showIf.toList(),
        enableIf = enableIf.toList(),
        lockedHint = lockedHint,
        badge = badge,
        effects = effects.toList(),
        outcomes = outcomes.toList(),
        result = result,
        refs = refs.toList(),
        hint = hint
    )
}

@StoryDsl
class ScenarioBuilder(
    private val id: String,
    private val category: Category,
    private val art: SceneArt,
    private val ages: IntRange,
    private val title: String?,
    private val weight: Int,
    private val priority: Int,
    private val repeatable: Boolean,
    private val branch: Boolean,
    private val fromPast: Boolean,
    private val months: Int?
) {
    private var text = ""
    private val extras = mutableListOf<Pair<Condition, String>>()
    private val requires = mutableListOf<Condition>()
    private val boosts = mutableListOf<Pair<Condition, Int>>()
    private val choices = mutableListOf<Choice>()

    fun text(t: String) { text = t }

    /** Appends [t] to the story text when [cond] holds. */
    fun extra(cond: Condition, t: String) { extras += cond to t }

    fun requires(vararg c: Condition) { requires += c }

    /** Makes this scenario [w] points more likely while [cond] holds. */
    fun boost(cond: Condition, w: Int) { boosts += cond to w }

    fun choice(text: String, block: ChoiceBuilder.() -> Unit) {
        choices += ChoiceBuilder(text).apply(block).build()
    }

    internal fun build() = Scenario(
        id = id, category = category, title = title ?: category.defaultTitle, art = art, text = text,
        extras = extras.toList(), minAge = ages.first, maxAge = ages.last, requires = requires.toList(),
        boosts = boosts.toList(), weight = weight, priority = priority, repeatable = repeatable,
        branchOnly = branch, fromPast = fromPast, months = months, choices = choices.toList()
    )
}

@StoryDsl
class PackBuilder {
    internal val scenarios = mutableListOf<Scenario>()

    fun scenario(
        id: String,
        category: Category,
        art: SceneArt,
        ages: IntRange,
        title: String? = null,
        weight: Int = 10,
        priority: Int = 0,
        repeatable: Boolean = false,
        branch: Boolean = false,
        fromPast: Boolean = false,
        months: Int? = null,
        block: ScenarioBuilder.() -> Unit
    ) {
        scenarios += ScenarioBuilder(id, category, art, ages, title, weight, priority, repeatable, branch, fromPast, months)
            .apply(block).build()
    }
}

fun storyPack(id: String, title: String, block: PackBuilder.() -> Unit): StoryPack =
    SimpleStoryPack(id, title, PackBuilder().apply(block).scenarios.toList())
