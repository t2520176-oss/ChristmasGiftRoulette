package com.lifeyourchoice.core.motto

import com.lifeyourchoice.core.engine.Flags
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.NpcRole
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.model.Trait
import kotlin.random.Random

/**
 * Picks the one sentence that best represents a completed life. Entirely local and deterministic:
 * the same life and the same history of previous mottos always give the same result.
 *
 * Step 1 scores every [MottoCategory] from the life's stats, hidden traits and story flags.
 * Step 2 chooses the best category that still has a motto the player has not recently received.
 */
object MottoEngine {

    /** How many past mottos are remembered when avoiding repeats. */
    const val RECENT_LIMIT = 60

    private fun v(s: GameState, k: Stat) = s.stat(k)

    /** True when the life ended in a good place by ordinary measures. */
    private fun succeeded(s: GameState) =
        v(s, Stat.MONEY) >= 60 || s.businessStage >= 3 || s.careerLevel >= 4 || v(s, Stat.CAREER) >= 70

    fun categoryScores(s: GameState): Map<MottoCategory, Int> {
        val money = v(s, Stat.MONEY); val family = v(s, Stat.FAMILY); val health = v(s, Stat.HEALTH)
        val happy = v(s, Stat.HAPPINESS); val friend = v(s, Stat.FRIENDSHIP); val rep = v(s, Stat.REPUTATION)
        val disc = v(s, Stat.DISCIPLINE); val career = v(s, Stat.CAREER)
        val risky = s.trait(Trait.RISK_TAKER)
        val avgCore = (money + family + health + happy) / 4
        val c = MottoCategory.values().associateWith { 0 }.toMutableMap()
        fun put(cat: MottoCategory, score: Int) { if (score > c.getValue(cat)) c[cat] = score }

        // Rich but the rest of the life suffered.
        if (money >= 70 && family <= 45 && happy <= 58) put(MottoCategory.SUCCESS_AT_A_COST, 90 + (45 - family).coerceIn(0, 6))
        else if (succeeded(s) && (family <= 40 || friend <= 32)) put(MottoCategory.SUCCESS_AT_A_COST, 82)

        // Failure followed by later success.
        if (s.has(Flags.COMEBACK) || (s.has(Flags.BUSINESS_FAILED) && s.businessStage >= 2)) put(MottoCategory.COMEBACK, 92)
        else if (s.has(Flags.BUSINESS_FAILED) || s.has(Flags.LOST_JOB)) put(MottoCategory.FAILURE, 72)

        // Family and meaning.
        if (family >= 80 && happy >= 70) put(MottoCategory.FAMILY, 86)
        else if (family >= 70) put(MottoCategory.FAMILY, 60 + (family - 70) / 2)
        if (s.has(Flags.FAMILY_FIRST) && family >= 60) put(MottoCategory.FAMILY, 84)

        // Skill and habits.
        if (disc >= 68 && (career >= 60 || s.careerLevel >= 4)) put(MottoCategory.DISCIPLINE, 85)
        else if (disc >= 80) put(MottoCategory.DISCIPLINE, 70)
        if (s.careerLevel >= 4) put(MottoCategory.CAREER, 68 + (rep - 60).coerceIn(0, 12))

        // Health versus ambition.
        if (health <= 38 && (career >= 70 || s.careerLevel >= 4 || money >= 68) &&
            (s.has(Flags.WORKED_TOO_MUCH) || s.counter(Flags.C_OVERWORK) >= 2 || health <= 30)) put(MottoCategory.HEALTH, 89)
        else if (health <= 30) put(MottoCategory.HEALTH, 70)

        // Friends.
        if (friend >= 66 && s.trait(Trait.LOYAL) >= 18) put(MottoCategory.FRIENDSHIP, 84)
        else if (friend >= 78 || s.trust(NpcRole.BEST_FRIEND) >= 85) put(MottoCategory.FRIENDSHIP, 66)

        // Fresh starts after a rocky youth.
        if (s.counter(Flags.C_EARLY_MISTAKES) >= 2 && (s.counter(Flags.C_REDEMPTIONS) >= 1 || s.has(Flags.SECOND_CHANCE))) {
            put(MottoCategory.SECOND_CHANCES, 88)
        } else if (s.has(Flags.SECOND_CHANCE)) put(MottoCategory.SECOND_CHANCES, 80)

        // A life in balance.
        if (money >= 60 && family >= 60 && health >= 60 && happy >= 60) put(MottoCategory.BALANCED_LIFE, 90)

        // Risk: rewarded versus costly.
        if (risky >= 30 && succeeded(s)) put(MottoCategory.COURAGE, 82)
        if (risky >= 30 && (money <= 35 || s.has(Flags.BUSINESS_FAILED) && !s.has(Flags.COMEBACK))) put(MottoCategory.RISK_WISDOM, 86)
        if (s.counter(Flags.C_RISK_TAKEN) >= 2 && succeeded(s)) put(MottoCategory.RISK, 74)
        if (s.counter(Flags.C_RISK_AVOIDED) >= 3 && s.counter(Flags.C_RISK_TAKEN) <= 1) put(MottoCategory.RISK, 78)

        // Character.
        if (s.has(Flags.KINDNESS_RETURNED)) put(MottoCategory.KINDNESS, 80)
        else if (s.trait(Trait.COMPASSIONATE) >= 40 || s.counter(Flags.C_KINDNESS) >= 8) put(MottoCategory.KINDNESS, 70)
        if (s.has(Flags.BETRAYED_FRIEND) || s.has(Flags.TRUST_BROKEN)) put(MottoCategory.TRUST, 77)
        else if (s.has(Flags.REFUSED_UNETHICAL) && rep >= 65) put(MottoCategory.TRUST, 73)
        if (s.trait(Trait.AMBITIOUS) >= 35) put(MottoCategory.AMBITION, 68)

        // Work, business, money.
        if (s.has(Flags.STARTED_BUSINESS)) put(MottoCategory.BUSINESS, 66 + s.businessStage * 3)
        if (money <= 35 && happy >= 60) put(MottoCategory.MONEY, 76)
        else if (s.has(Flags.SAVED_MONEY) && s.counter(Flags.C_CRISES) >= 1) put(MottoCategory.MONEY, 72)
        if (succeeded(s) && happy >= 60 && rep >= 70) put(MottoCategory.SUCCESS, 72)

        // Love, joy, regret.
        if (s.relationship == RelationshipStatus.MARRIED && s.trust(NpcRole.PARTNER) >= 62) put(MottoCategory.LOVE, 74)
        if (happy >= 80) put(MottoCategory.HAPPINESS, 71)
        if ((s.counter(Flags.C_MISTAKES) >= 5 || s.has(Flags.IGNORED_PARENTS)) && happy <= 55) put(MottoCategory.REGRET, 78)
        if (s.has(Flags.RECONCILED)) put(MottoCategory.REGRET, 70)

        // Gentle baselines so every life has an appropriate fallback.
        put(MottoCategory.HAPPINESS, 25 + happy / 4)
        put(MottoCategory.BALANCED_LIFE, 20 + avgCore / 4)
        put(MottoCategory.FAMILY, 15 + family / 5)
        put(MottoCategory.FRIENDSHIP, 15 + friend / 5)
        put(MottoCategory.SUCCESS, 10 + maxOf(money, career) / 6)
        return c
    }

    /** The category the engine considers the best fit for [s] (ignores repetition). */
    fun bestCategory(s: GameState): MottoCategory =
        categoryScores(s).entries.sortedWith(compareByDescending<Map.Entry<MottoCategory, Int>> { it.value }.thenBy { it.key.ordinal })
            .first().key

    /**
     * Chooses the final motto. [recentIds] holds previously awarded motto ids, oldest first; a motto
     * is only repeated when no other appropriate one exists.
     */
    fun select(s: GameState, recentIds: List<String>): Motto {
        val scores = categoryScores(s)
        val ranked = scores.entries
            .sortedWith(compareByDescending<Map.Entry<MottoCategory, Int>> { it.value }.thenBy { it.key.ordinal })
        val top = ranked.first().value
        val recent = recentIds.toSet()
        val rng = Random(s.seed xor 0x4D4F54544FL)

        // Tier 1: close to the best fit. Tier 2: still reasonably relevant. Tier 3: anything.
        val tiers = listOf(top - 20, 40, Int.MIN_VALUE)
        for (floor in tiers) {
            for (entry in ranked) {
                if (entry.value < floor || entry.value <= 0 && floor != Int.MIN_VALUE) continue
                val unused = MottoLibrary.byCategory(entry.key).filter { it.id !in recent }
                if (unused.isNotEmpty()) return unused[rng.nextInt(unused.size)]
            }
        }
        // Everything has been seen: reuse the least recently awarded line of the best category.
        val pool = MottoLibrary.byCategory(ranked.first().key)
        return pool.minByOrNull { m -> recentIds.lastIndexOf(m.id) } ?: pool.first()
    }
}
