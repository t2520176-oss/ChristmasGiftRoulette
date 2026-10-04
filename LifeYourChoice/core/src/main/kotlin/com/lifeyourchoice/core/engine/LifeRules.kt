package com.lifeyourchoice.core.engine

import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.Stat
import kotlin.math.floor
import kotlin.random.Random

/**
 * The internal consequence system. Time passing nudges stats according to realistic principles,
 * and the player is never lectured: only a short, neutral "time passes" note is shown.
 *
 *  - Discipline and education improve long-term opportunities.
 *  - Constant work damages health and relationships; neglected health deteriorates.
 *  - Family and friendships need time. Money without anything else does not make you happy.
 */
object LifeRules {

    /** Applies [months] of passive change and returns short notes for the consequence screen. */
    fun advance(s: GameState, months: Int, rng: Random): List<String> {
        if (months <= 0) return emptyList()
        val y = months / 12.0
        val notes = mutableListOf<String>()
        val age = s.ageYears

        fun move(stat: Stat, amount: Double) {
            val v = amount * y
            val base = floor(v).toInt()
            val extra = if (rng.nextDouble() < v - base) 1 else 0
            s.change(stat, base + extra)
        }

        // Energy recovers towards a level set by health.
        val energyTarget = 55 + (s.stat(Stat.HEALTH) - 50) * 0.5
        val gap = energyTarget - s.stat(Stat.ENERGY)
        s.change(Stat.ENERGY, (gap * minOf(1.0, 0.6 * y + 0.25)).toInt())

        // Health: ageing, burnout, neglect.
        var healthDrift = 0.0
        if (age >= 40) healthDrift -= 0.4
        if (age >= 60) healthDrift -= 0.5
        if (s.stat(Stat.DISCIPLINE) >= 70) healthDrift += 0.4
        if (s.stat(Stat.HEALTH) < 30) {
            healthDrift -= 1.2
            notes += "Years of neglect are catching up with your body."
        }
        if (s.stat(Stat.ENERGY) < 25) healthDrift -= 1.0
        val overwork = s.counter("overwork")
        if (overwork >= 3) {
            healthDrift -= 1.0
            move(Stat.FAMILY, -2.0)
            move(Stat.HAPPINESS, -1.0)
            notes += "Long hours at work leave little time for anything else."
        }
        move(Stat.HEALTH, healthDrift)

        // Work and money.
        if (s.has(Flags.RETIRED)) {
            // Pension and savings: modest income, same living costs.
            move(Stat.MONEY, 0.9 + (if (s.has(Flags.SAVED_MONEY)) 0.5 else 0.0) - 1.1)
        } else if (s.employed) {
            val income = 0.4 + s.careerLevel * 0.38 + s.businessStage * 0.5
            val expenses = 1.05 + s.children * 0.6 + (if (s.relationship == RelationshipStatus.MARRIED) 0.3 else 0.0)
            move(Stat.MONEY, income - expenses)
            val growth = if (s.stat(Stat.DISCIPLINE) >= 60) 1.5 else 0.8
            move(Stat.CAREER, growth)
        } else if (age >= 20) {
            move(Stat.MONEY, -1.4)
            if (s.stat(Stat.MONEY) < 15) notes += "Money is tight."
        }
        if (s.stat(Stat.MONEY) < 10) move(Stat.HAPPINESS, -1.5)
        // Lifestyle inflation: wealth above comfortable levels tends to get spent.
        if (s.stat(Stat.MONEY) > 72) move(Stat.MONEY, -(s.stat(Stat.MONEY) - 72) * 0.06)

        // Learning compounds with discipline.
        if (s.stat(Stat.DISCIPLINE) >= 70) move(Stat.KNOWLEDGE, 0.8)

        // Relationships need time.
        if (age >= 22) {
            if (s.stat(Stat.FAMILY) < 50) move(Stat.FAMILY, -0.4)
            move(Stat.FRIENDSHIP, if (s.stat(Stat.FRIENDSHIP) > 55) -0.5 else -0.2)
        }

        // Everything that is not tended drifts back towards the ordinary: no stat stays maxed for free.
        fun regress(stat: Stat, baseline: Int, rate: Double) {
            val above = s.stat(stat) - baseline
            if (above > 0) move(stat, -above * rate)
        }
        regress(Stat.FAMILY, 60, 0.07)
        regress(Stat.FRIENDSHIP, 55, 0.06)
        regress(Stat.KNOWLEDGE, 60, 0.02)
        regress(Stat.CONFIDENCE, 60, 0.05)
        regress(Stat.REPUTATION, 60, 0.04)
        regress(Stat.DISCIPLINE, 60, 0.03)
        regress(Stat.CAREER, 60, 0.04)

        // Happiness follows the things that matter most.
        val moneyFeel = minOf(s.stat(Stat.MONEY), 70)
        val target = 0.32 * s.stat(Stat.FAMILY) + 0.24 * s.stat(Stat.FRIENDSHIP) +
            0.24 * s.stat(Stat.HEALTH) + 0.20 * moneyFeel
        val hGap = target - s.stat(Stat.HAPPINESS)
        s.change(Stat.HAPPINESS, (hGap * minOf(0.6, 0.2 * y + 0.05)).toInt())

        return notes.distinct()
    }

    /** Pace of life: months that pass after a scenario, by age. */
    fun paceMonths(ageYears: Int, rng: Random): Int {
        val base = when {
            ageYears < 18 -> 6
            ageYears < 22 -> 12
            ageYears < 32 -> 13
            ageYears < 50 -> 19
            ageYears < 65 -> 24
            else -> 32
        }
        return base + rng.nextInt(base / 3 + 1)
    }
}
