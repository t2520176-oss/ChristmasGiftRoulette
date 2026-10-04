package com.lifeyourchoice.core.story

import com.lifeyourchoice.core.model.CareerTrack
import com.lifeyourchoice.core.model.Education
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.NpcRole
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.model.Trait

/** A predicate over the game state. Used to gate scenarios, choices, outcomes and text. */
fun interface Condition {
    fun test(s: GameState): Boolean
}

fun atLeast(stat: Stat, n: Int) = Condition { it.stat(stat) >= n }

fun atMost(stat: Stat, n: Int) = Condition { it.stat(stat) <= n }

fun statBetween(stat: Stat, min: Int, max: Int) = Condition { it.stat(stat) in min..max }

fun has(flag: String): Condition { FlagAudit.referenced += flag; return Condition { it.has(flag) } }

fun lacks(flag: String): Condition { FlagAudit.referenced += flag; return Condition { !it.has(flag) } }

fun hasAll(vararg flags: String): Condition { FlagAudit.referenced += flags; return Condition { s -> flags.all { s.has(it) } } }

fun hasAny(vararg flags: String): Condition { FlagAudit.referenced += flags; return Condition { s -> flags.any { s.has(it) } } }

fun trustAtLeast(role: NpcRole, n: Int) = Condition { it.trust(role) >= n }

fun trustAtMost(role: NpcRole, n: Int) = Condition { it.trust(role) <= n }

/** Hidden trait at least [n] (traits start at 0 and grow with behaviour). */
fun traitAtLeast(trait: Trait, n: Int) = Condition { it.trait(trait) >= n }

fun playerIs(gender: Gender) = Condition { it.gender == gender }

fun careerIs(vararg tracks: CareerTrack) = Condition { it.career in tracks }

fun careerLevel(min: Int = 0, max: Int = 5) = Condition { it.careerLevel in min..max }

val employed = Condition { it.employed }

val unemployed = Condition { !it.employed }

fun status(vararg statuses: RelationshipStatus) = Condition { it.relationship in statuses }

fun educated(vararg e: Education) = Condition { it.education in e }

fun counterAtLeast(name: String, n: Int): Condition { FlagAudit.counterReferenced += name; return Condition { it.counter(name) >= n } }

fun counterAtMost(name: String, n: Int): Condition { FlagAudit.counterReferenced += name; return Condition { it.counter(name) <= n } }

val hasChildren = Condition { it.children > 0 }

val noChildren = Condition { it.children == 0 }

fun businessAtLeast(n: Int) = Condition { it.businessStage >= n }

fun businessAtMost(n: Int) = Condition { it.businessStage <= n }

fun ageBetween(min: Int, max: Int) = Condition { it.ageYears in min..max }

fun anyOf(vararg conditions: Condition) = Condition { s -> conditions.any { it.test(s) } }

fun allOf(vararg conditions: Condition) = Condition { s -> conditions.all { it.test(s) } }

fun not(condition: Condition) = Condition { !condition.test(it) }

fun chance(percent: Int, salt: Int = 0) = Condition { s ->
    // Deterministic per life and step so saved games resume identically.
    kotlin.random.Random(s.seed * 31 + s.step * 17 + salt).nextInt(100) < percent
}

/** True during the last few years of a life (used for the final reflection). */
val nearEnd = Condition { it.ageYears >= it.plannedEndAge - 3 }
