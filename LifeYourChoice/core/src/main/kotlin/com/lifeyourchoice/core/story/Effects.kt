package com.lifeyourchoice.core.story

import com.lifeyourchoice.core.model.CareerTrack
import com.lifeyourchoice.core.model.Education
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.NpcRole
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.Scheduled
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.model.Trait
import kotlin.random.Random

/** Mutable scratch space handed to every [Effect] while a decision is resolved. */
class EffectContext(val state: GameState, val rng: Random) {
    /** Extra sentences appended to the result text (from conditional effects). */
    val notes = mutableListOf<String>()
}

/** One change a decision makes to the world. Stats, traits, flags, NPC trust, career, schedule... */
fun interface Effect {
    fun apply(ctx: EffectContext)
}

object Effects {
    fun stat(stat: Stat, delta: Int) = Effect { it.state.change(stat, delta) }

    fun trait(trait: Trait, delta: Int) = Effect { it.state.bump(trait, delta) }

    fun flag(flag: String) = Effect { it.state.flags.add(flag) }

    fun unflag(flag: String) = Effect { it.state.flags.remove(flag) }

    fun counter(name: String, delta: Int) = Effect { it.state.addCounter(name, delta) }

    fun trust(role: NpcRole, delta: Int) = Effect { it.state.adjustTrust(role, delta) }

    fun milestone(text: String) = Effect { it.state.addMilestone(text) }

    /** Puts a follow-up scenario at the front of the queue so it is shown next, with no time passing. */
    fun queue(id: String) = Effect { it.state.queue.add(id) }

    /** A delayed consequence: becomes eligible [minYears]..[maxYears] from now. */
    fun schedule(id: String, minYears: Int, maxYears: Int) = Effect { ctx ->
        val months = (minYears * 12) + ctx.rng.nextInt(((maxYears - minYears) * 12).coerceAtLeast(1))
        ctx.state.scheduled.add(Scheduled(id, ctx.state.ageMonths + months))
    }

    fun job(track: CareerTrack, level: Int) = Effect {
        val s = it.state
        if (s.career != track && s.employed) s.addCounter("career_changes", 1)
        s.career = track
        s.careerLevel = level.coerceIn(1, 5)
    }

    fun promote(levels: Int) = Effect {
        if (it.state.employed) it.state.careerLevel = (it.state.careerLevel + levels).coerceIn(1, 5)
    }

    val loseJob = Effect {
        val s = it.state
        if (s.employed) {
            s.counters["prev_career"] = s.career.ordinal
            s.counters["prev_level"] = s.careerLevel
        }
        s.career = CareerTrack.NONE
        s.careerLevel = 0
        s.flags.add("lost_job")
    }

    /** Returns to the career held before [loseJob], at a level shifted by [levelDelta]. */
    fun rehire(levelDelta: Int) = Effect {
        val s = it.state
        val track = CareerTrack.values().getOrNull(s.counters["prev_career"] ?: -1)?.takeIf { t -> t != CareerTrack.NONE }
        s.career = track ?: CareerTrack.EMPLOYEE
        s.careerLevel = ((s.counters["prev_level"] ?: 1) + levelDelta).coerceIn(1, 5)
    }

    /** The player has no job (e.g. a business closed) without it counting as being fired. */
    val noJob = Effect {
        it.state.career = CareerTrack.NONE
        it.state.careerLevel = 0
    }

    fun education(e: Education) = Effect { it.state.education = e }

    fun relationship(r: RelationshipStatus) = Effect { it.state.relationship = r }

    fun children(delta: Int) = Effect { it.state.children = (it.state.children + delta).coerceAtLeast(0) }

    fun business(stage: Int) = Effect { it.state.businessStage = stage.coerceIn(0, 3) }

    fun note(text: String) = Effect { it.notes.add(text) }

    fun conditional(cond: Condition, effects: List<Effect>) = Effect { ctx ->
        if (cond.test(ctx.state)) effects.forEach { it.apply(ctx) }
    }
}
