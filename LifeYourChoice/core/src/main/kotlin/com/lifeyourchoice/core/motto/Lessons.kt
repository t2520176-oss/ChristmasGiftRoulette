package com.lifeyourchoice.core.motto

import com.lifeyourchoice.core.engine.Flags
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.NpcRole
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.model.Trait

class Lesson(
    val id: String,
    /** Lessons sharing a theme are never shown together. */
    val theme: String,
    val text: String,
    /** 0 = not relevant to this life; higher = more relevant. */
    val score: (GameState) -> Int
)

/**
 * "What you learned": exactly three short lessons chosen from the player's stats, hidden traits,
 * story flags and past decisions. Fully deterministic: the same life always yields the same lessons.
 */
object LessonEngine {

    private fun st(s: GameState, k: Stat) = s.stat(k)

    val library: List<Lesson> = listOf(
        Lesson("cost_everyone", "cost", "Success means little if you lose everyone along the way.") { s ->
            val achieved = st(s, Stat.MONEY) >= 62 || s.careerLevel >= 4
            if (achieved && (st(s, Stat.FAMILY) <= 45 || st(s, Stat.FRIENDSHIP) <= 35)) 82 + (50 - st(s, Stat.FAMILY)).coerceIn(0, 10) else 0
        },
        Lesson("failure_experience", "failure", "Failure can become experience if you choose to try again.") { s ->
            if (s.has(Flags.BUSINESS_FAILED) || s.has(Flags.LOST_JOB)) 70 + (if (s.has(Flags.COMEBACK)) 18 else 0) else 0
        },
        Lesson("kindness_returns", "kindness", "Small acts of kindness can return years later.") { s ->
            val base = if (s.has(Flags.KINDNESS_RETURNED)) 88 else 0
            val seeds = (if (s.has(Flags.HELPED_BULLIED_FRIEND)) 1 else 0) + (if (s.has(Flags.HELPED_COWORKER)) 1 else 0) +
                (if (s.has(Flags.MENTORED)) 1 else 0) + s.counter(Flags.C_KINDNESS)
            maxOf(base, if (seeds >= 2) 58 + s.trait(Trait.COMPASSIONATE) / 3 else 0)
        },
        Lesson("money_health", "health", "Money provides security, but it cannot replace health.") { s ->
            if (st(s, Stat.MONEY) >= 55 && st(s, Stat.HEALTH) <= 45) 84 else 0
        },
        Lesson("rest_ambition", "health", "Rest is not the opposite of ambition. It is what sustains it.") { s ->
            if (s.has(Flags.WORKED_TOO_MUCH) || s.counter(Flags.C_OVERWORK) >= 3) 66 + (if (st(s, Stat.HEALTH) < 50) 8 else 0) else 0
        },
        Lesson("trust_moment", "trust", "Trust takes years to build and moments to lose.") { s ->
            if (s.has(Flags.BETRAYED_FRIEND) || s.has(Flags.TRUST_BROKEN) || s.has(Flags.CHEATED) || s.has(Flags.TOOK_SHORTCUT)) 76 else 0
        },
        Lesson("honest_road", "trust", "Doing the right thing may cost you today and protect you for decades.") { s ->
            if (s.has(Flags.REFUSED_UNETHICAL) || (s.trait(Trait.DISHONEST) <= 2 && st(s, Stat.REPUTATION) >= 70)) 68 else 0
        },
        Lesson("courage_not_all", "risk", "Courage does not mean taking every risk.") { s ->
            if (s.trait(Trait.RISK_TAKER) >= 30 && (st(s, Stat.MONEY) <= 40 || s.has(Flags.BUSINESS_FAILED))) 80 else 0
        },
        Lesson("risk_rewarded", "risk", "A reasonable risk can open doors that caution never will.") { s ->
            if (s.counter(Flags.C_RISK_TAKEN) >= 2 && (st(s, Stat.MONEY) >= 55 || s.businessStage >= 2 || s.has(Flags.WENT_ABROAD))) 74 else 0
        },
        Lesson("safe_road", "risk", "A safe road protects you from failure, but it can also hide your possibilities.") { s ->
            if (s.counter(Flags.C_RISK_AVOIDED) >= 3 && s.counter(Flags.C_RISK_TAKEN) <= 1) 77 else 0
        },
        Lesson("family_time", "family", "Family relationships require time, not just good intentions.") { s ->
            if (st(s, Stat.FAMILY) <= 45 || s.has(Flags.IGNORED_PARENTS)) 76 + (if (s.has(Flags.WORKED_TOO_MUCH)) 6 else 0) else 0
        },
        Lesson("family_table", "family", "The people at your table matter more than the things on it.") { s ->
            if (st(s, Stat.FAMILY) >= 80 && s.has(Flags.FAMILY_FIRST)) 72 else if (st(s, Stat.FAMILY) >= 85) 60 else 0
        },
        Lesson("discipline_luck", "discipline", "Discipline built opportunities that luck never could.") { s ->
            if (st(s, Stat.DISCIPLINE) >= 70 && (st(s, Stat.CAREER) >= 60 || st(s, Stat.KNOWLEDGE) >= 70)) 72 else 0
        },
        Lesson("small_choices", "discipline", "Small choices, repeated every day, become who you are.") { s ->
            if (st(s, Stat.DISCIPLINE) >= 80) 56 else 0
        },
        Lesson("impatience", "discipline", "Quick decisions feel brave, but patience wins more battles.") { s ->
            if (s.trait(Trait.IMPULSIVE) >= 30 && s.counter(Flags.C_MISTAKES) >= 2) 67 else 0
        },
        Lesson("loyalty_wealth", "friendship", "Loyalty is a quiet kind of wealth.") { s ->
            if (st(s, Stat.FRIENDSHIP) >= 70 && s.trait(Trait.LOYAL) >= 25) 71 else 0
        },
        Lesson("friends_chosen", "friendship", "The friends who stay are the family you choose.") { s ->
            if (s.trust(NpcRole.BEST_FRIEND) >= 80 && !s.has(Flags.BETRAYED_FRIEND)) 66 else 0
        },
        Lesson("balance", "balance", "A good life is built in balance, not in extremes.") { s ->
            val low = minOf(st(s, Stat.MONEY), st(s, Stat.HEALTH), st(s, Stat.FAMILY), st(s, Stat.HAPPINESS))
            if (low >= 58) 74 else 0
        },
        Lesson("comeback", "comeback", "Your worst decision does not have to become your final decision.") { s ->
            if (s.has(Flags.COMEBACK) || s.has(Flags.SECOND_CHANCE) ||
                (s.counter(Flags.C_EARLY_MISTAKES) >= 2 && s.counter(Flags.C_REDEMPTIONS) >= 1)) 80 else 0
        },
        Lesson("resilience", "comeback", "Hard years do not define you. How you answer them does.") { s ->
            if (s.counter(Flags.C_CRISES) >= 2) 72 else 0
        },
        Lesson("repair_time", "regret", "Some things can only be repaired while the people are still here.") { s ->
            if (s.has(Flags.RECONCILED)) 70 else if (s.has(Flags.IGNORED_PARENTS) && st(s, Stat.FAMILY) <= 50) 69 else 0
        },
        Lesson("mistakes_teach", "regret", "Everyone makes mistakes. What matters is who you become afterwards.") { s ->
            if (s.counter(Flags.C_MISTAKES) >= 5) 65 else 0
        },
        Lesson("money_not_all", "money", "You can own very little and still live richly.") { s ->
            if (st(s, Stat.MONEY) <= 38 && st(s, Stat.HAPPINESS) >= 60) 76 else 0
        },
        Lesson("savings_calm", "money", "Savings are quiet protection. You only notice them when you need them.") { s ->
            if (s.has(Flags.SAVED_MONEY) && s.counter(Flags.C_CRISES) >= 1) 70 else if (s.has(Flags.SAVED_MONEY)) 45 else 0
        },
        Lesson("helping_grow", "mentor", "The best way to grow is to help someone else grow.") { s ->
            if (s.has(Flags.MENTORED)) 70 else 0
        },
        Lesson("love_daily", "love", "Love is built from small choices made every day.") { s ->
            if (s.relationship == RelationshipStatus.MARRIED && s.trust(NpcRole.PARTNER) >= 70) 66 else 0
        },
        Lesson("name_matters", "reputation", "A good name is a quiet résumé that follows you everywhere.") { s ->
            if (st(s, Stat.REPUTATION) >= 76) 62 else 0
        },
        Lesson("start_small", "business", "Every large thing began as a small, uncertain start.") { s ->
            if (s.has(Flags.STARTED_BUSINESS)) 62 + s.businessStage * 3 else 0
        },
        Lesson("keep_learning", "knowledge", "What you learn stays with you when everything else changes.") { s ->
            if (st(s, Stat.KNOWLEDGE) >= 80) 62 else 0
        },
        Lesson("ambition_fire", "ambition", "Ambition is a fire. It warms or burns depending on how you tend it.") { s ->
            if (s.trait(Trait.AMBITIOUS) >= 35) 55 else 0
        },
        Lesson("nobody_alone", "teamwork", "Nobody builds a good life alone.") { s ->
            if (s.has(Flags.HELPED_COWORKER) || st(s, Stat.FRIENDSHIP) >= 68) 54 else 0
        },
        Lesson("happy_company", "happiness", "Happiness was rarely in the destination. It was in who walked with you.") { s ->
            if (st(s, Stat.HAPPINESS) >= 75 && (st(s, Stat.FAMILY) >= 60 || st(s, Stat.FRIENDSHIP) >= 60)) 66 else 0
        },
        Lesson("generic_decisions", "generic1", "Every decision builds the life you will live.") { 12 },
        Lesson("generic_response", "generic2", "You cannot choose every circumstance, but you can choose your response.") { 11 },
        Lesson("generic_time", "generic3", "The time you give to people is the only gift that is never wasted.") { 10 }
    )

    /** Exactly three lessons with distinct themes, most relevant first. Ties resolve by library order. */
    fun select(s: GameState): List<Lesson> {
        val ranked = library.withIndex()
            .map { (i, l) -> Triple(l, l.score(s), i) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Triple<Lesson, Int, Int>> { it.second }.thenBy { it.third })
        val picked = mutableListOf<Lesson>()
        for ((l, _, _) in ranked) {
            if (picked.none { it.theme == l.theme }) picked += l
            if (picked.size == 3) break
        }
        return picked
    }
}
