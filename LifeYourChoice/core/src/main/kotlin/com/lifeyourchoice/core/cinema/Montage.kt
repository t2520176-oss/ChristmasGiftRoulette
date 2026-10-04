package com.lifeyourchoice.core.cinema

import com.lifeyourchoice.core.engine.Flags
import com.lifeyourchoice.core.model.CareerTrack
import com.lifeyourchoice.core.model.Education
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.NpcRole
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.SceneArt
import com.lifeyourchoice.core.model.Stat

/** One actor in a memory frame. [playerAge] is the age of the player in that memory (others derive theirs). */
class MontageActor(val actor: ActorId, val x: Float, val facing: Facing, val emotion: Emotion, val gesture: Gesture = Gesture.NONE, val seated: Boolean = false)

/** A remembered moment, rendered with the same stage, figures and camera as a normal scene. */
class MontageFrame(
    val id: String,
    /** The player's age in this memory. */
    val playerAge: Int,
    val env: SceneArt,
    val time: TimeOfDay,
    val actors: List<MontageActor>,
    val caption: String,
    val shot: Shot = Shot.TWO_SHOT
)

/** Stages a memory frame with the normal cinematic machinery (placement, mood, camera). */
fun MontageFrame.toScript(): CineScript {
    val b = BeatBuilder()
    with(b) {
        for (m in actors) {
            place(m.actor, m.x, m.facing, m.seated)
            mood(m.actor, m.emotion, m.gesture)
        }
        for (m in actors) if (m.actor != ActorId.PLAYER) look(m.actor, ActorId.PLAYER)
        look(ActorId.PLAYER, actors.firstOrNull { it.actor != ActorId.PLAYER }?.actor)
        cam(shot, ActorId.PLAYER, actors.firstOrNull { it.actor != ActorId.PLAYER }?.actor)
    }
    return CineScript("montage:$id", env, time, b.beats.toList(), emptyMap())
}

/** The cinematic that closes a life: memories, then the older player in a meaningful place. */
class EndingPlan(val memories: List<MontageFrame>, val finale: MontageFrame, val finaleCaption: String)

/**
 * Builds the ending montage ONLY from what actually happened: no wedding if the player never married,
 * no child if there were none, no business triumph if the business failed for good.
 */
object MontageBuilder {
    private class Candidate(val weight: Int, val order: Int, val frame: MontageFrame)

    fun build(s: GameState): EndingPlan {
        val c = mutableListOf<Candidate>()
        fun add(weight: Int, order: Int, f: MontageFrame) { c += Candidate(weight, order, f) }
        val P = ActorId.PLAYER
        fun a(id: ActorId, x: Float, f: Facing, e: Emotion, g: Gesture = Gesture.NONE, seated: Boolean = false) = MontageActor(id, x, f, e, g, seated)
        val startAge = (s.history.firstOrNull()?.ageYears ?: 15).coerceAtLeast(14)

        // Where it began: always.
        add(100, startAge, MontageFrame("school", startAge, SceneArt.CLASSROOM, TimeOfDay.DAY,
            listOf(a(P, 0.34f, Facing.RIGHT, Emotion.HAPPY), a(ActorId.FRIEND, 0.68f, Facing.LEFT, Emotion.HAPPY, Gesture.EXPLAIN)),
            "It began in a classroom, with a friend beside you."))

        if (s.has(Flags.HELPED_BULLIED_FRIEND)) {
            add(90, startAge + 1, MontageFrame("bullied_help", startAge + 1, SceneArt.HALLWAY, TimeOfDay.DAY,
                listOf(a(P, 0.34f, Facing.RIGHT, Emotion.CONFIDENT, Gesture.POINT), a(ActorId.FRIEND, 0.66f, Facing.LEFT, Emotion.HAPPY)),
                "You stood up for a friend who needed you."))
        }
        if (s.has("helped_underdog")) {
            add(60, startAge + 1, MontageFrame("underdog", startAge + 1, SceneArt.SCHOOL_YARD, TimeOfDay.DAY,
                listOf(a(P, 0.40f, Facing.RIGHT, Emotion.HAPPY, seated = true), a(ActorId.UNDERDOG, 0.62f, Facing.LEFT, Emotion.HAPPY, seated = true)),
                "You sat beside someone who was eating alone."))
        }
        when (s.education) {
            Education.UNIVERSITY -> add(55, 21, MontageFrame("education", 21, SceneArt.CAMPUS, TimeOfDay.DAY,
                listOf(a(P, 0.40f, Facing.RIGHT, Emotion.PROUD, Gesture.FIST_PUMP), a(ActorId.MOTHER, 0.68f, Facing.LEFT, Emotion.HAPPY, Gesture.CLAP)),
                "You chose to keep learning."))
            Education.TRADE_SCHOOL -> add(50, 21, MontageFrame("education", 21, SceneArt.WORKSHOP, TimeOfDay.DAY,
                listOf(a(P, 0.40f, Facing.RIGHT, Emotion.PROUD), a(ActorId.MENTOR, 0.68f, Facing.LEFT, Emotion.PROUD, Gesture.NOD)),
                "You learned a craft with your own hands."))
            else -> {}
        }
        if (s.has(Flags.FIRST_JOB)) {
            val env = when (s.lastCareer) {
                CareerTrack.SKILLED_WORKER -> SceneArt.WORKSHOP
                CareerTrack.DOCTOR -> SceneArt.HOSPITAL
                CareerTrack.TEACHER -> SceneArt.CLASSROOM
                CareerTrack.CREATOR, CareerTrack.DESIGNER -> SceneArt.APARTMENT
                else -> SceneArt.OFFICE
            }
            add(60, 24, MontageFrame("first_job", 24, env, TimeOfDay.DAY,
                listOf(a(P, 0.38f, Facing.RIGHT, Emotion.HAPPY), a(ActorId.COWORKER, 0.68f, Facing.LEFT, Emotion.HAPPY, Gesture.HANDSHAKE)),
                "Your first real job, and your first real paycheck."))
        }
        when (s.relationship) {
            RelationshipStatus.MARRIED -> add(85, 29, MontageFrame("wedding", 29, SceneArt.WEDDING, TimeOfDay.EVENING,
                listOf(a(P, 0.42f, Facing.RIGHT, Emotion.HAPPY, Gesture.HUG), a(ActorId.PARTNER, 0.58f, Facing.LEFT, Emotion.HAPPY, Gesture.HUG)),
                "You built a life with someone you love."))
            RelationshipStatus.DATING -> add(50, 27, MontageFrame("love", 27, SceneArt.COFFEE_SHOP, TimeOfDay.EVENING,
                listOf(a(P, 0.40f, Facing.RIGHT, Emotion.HAPPY, seated = true), a(ActorId.PARTNER, 0.62f, Facing.LEFT, Emotion.HAPPY, seated = true)),
                "Someone special shared part of the road."))
            RelationshipStatus.SINGLE -> {}
        }
        if (s.children > 0) {
            val cast = mutableListOf(a(P, 0.38f, Facing.RIGHT, Emotion.HAPPY), a(ActorId.CHILD, 0.58f, Facing.LEFT, Emotion.EXCITED, Gesture.WAVE))
            if (s.relationship == RelationshipStatus.MARRIED) cast += a(ActorId.PARTNER, 0.76f, Facing.LEFT, Emotion.HAPPY)
            add(80, 36, MontageFrame("children", 36, SceneArt.NEW_HOME, TimeOfDay.EVENING, cast, "A family grew around you."))
        }
        val failedForGood = s.has(Flags.BUSINESS_FAILED) && s.businessStage < 2 && !s.has(Flags.BUSINESS_SUCCESS)
        if (s.has(Flags.STARTED_BUSINESS) && s.has(Flags.BUSINESS_FAILED)) {
            add(75, 33, MontageFrame("business_failed", 33, SceneArt.SMALL_BUSINESS, TimeOfDay.NIGHT,
                listOf(a(P, 0.5f, Facing.RIGHT, Emotion.SAD, Gesture.HEAD_DOWN, seated = true)),
                "A business that didn’t make it.", Shot.PUSH_IN))
        }
        if (!failedForGood && (s.has(Flags.BUSINESS_SUCCESS) || s.businessStage >= 2 || s.has("sold_business") || s.has("passed_on_business"))) {
            add(85, 44, MontageFrame("business_success", 44, SceneArt.SMALL_BUSINESS, TimeOfDay.MORNING,
                listOf(a(P, 0.40f, Facing.RIGHT, Emotion.PROUD), a(ActorId.COWORKER, 0.66f, Facing.LEFT, Emotion.HAPPY, Gesture.HANDSHAKE)),
                "You built something that lasted."))
        }
        if (s.has(Flags.LOST_JOB)) {
            add(65, 38, MontageFrame("lost_job", 38, SceneArt.OFFICE, TimeOfDay.NIGHT,
                listOf(a(P, 0.5f, Facing.RIGHT, Emotion.TIRED, Gesture.HEAD_DOWN, seated = true)),
                "A hard year that tested everything.", Shot.PUSH_IN))
        }
        if (s.has(Flags.COMEBACK)) {
            add(90, 45, MontageFrame("comeback", 45, SceneArt.STREET, TimeOfDay.MORNING,
                listOf(a(P, 0.5f, Facing.RIGHT, Emotion.CONFIDENT, Gesture.FIST_PUMP)),
                "And then you rose again.", Shot.MEDIUM))
        }
        if (s.careerLevel >= 4 || s.lastCareerLevel >= 4) {
            add(55, 42, MontageFrame("career", 42, SceneArt.MEETING, TimeOfDay.DAY,
                listOf(a(P, 0.40f, Facing.RIGHT, Emotion.PROUD), a(ActorId.COWORKER, 0.66f, Facing.LEFT, Emotion.HAPPY, Gesture.CLAP)),
                "You earned respect, one honest day at a time."))
        }
        if (s.has(Flags.WENT_ABROAD)) {
            add(55, 30, MontageFrame("abroad", 30, SceneArt.AIRPORT, TimeOfDay.EVENING,
                listOf(a(P, 0.45f, Facing.RIGHT, Emotion.EXCITED, Gesture.WAVE)),
                "You took the leap, far from home.", Shot.MEDIUM))
        }
        if (s.has(Flags.FAMILY_PROTECTED) || s.has("supported_family")) {
            add(70, 40, MontageFrame("family_support", 40, SceneArt.LIVING_ROOM, TimeOfDay.NIGHT,
                listOf(a(P, 0.40f, Facing.RIGHT, Emotion.HAPPY, Gesture.HUG), a(ActorId.MOTHER, 0.60f, Facing.LEFT, Emotion.HAPPY, Gesture.HUG)),
                "When your family needed you, you were there."))
        }
        if (s.has(Flags.MENTORED)) {
            add(50, 55, MontageFrame("mentor", 55, SceneArt.OFFICE, TimeOfDay.DAY,
                listOf(a(P, 0.40f, Facing.RIGHT, Emotion.PROUD, Gesture.EXPLAIN), a(ActorId.COWORKER, 0.64f, Facing.LEFT, Emotion.EXCITED)),
                "You passed on what you had learned."))
        }
        if (s.trust(NpcRole.BEST_FRIEND) >= 75 && !s.has(Flags.BETRAYED_FRIEND)) {
            add(70, 58, MontageFrame("old_friend", 58, SceneArt.PARK, TimeOfDay.EVENING,
                listOf(a(P, 0.40f, Facing.RIGHT, Emotion.HAPPY, seated = true), a(ActorId.FRIEND, 0.62f, Facing.LEFT, Emotion.LAUGHING, seated = true)),
                "A friend who stayed for life."))
        }
        if (s.has(Flags.KINDNESS_RETURNED)) {
            add(75, 47, MontageFrame("kindness", 47, SceneArt.APARTMENT, TimeOfDay.NIGHT,
                listOf(a(P, 0.40f, Facing.RIGHT, Emotion.HAPPY), a(ActorId.FRIEND, 0.64f, Facing.LEFT, Emotion.PROUD, Gesture.GENTLE_SMILE)),
                "A kindness you gave long ago came back."))
        }
        // Regrets: at most two, only if they happened.
        val regrets = mutableListOf<Candidate>()
        if (s.has(Flags.BETRAYED_FRIEND)) regrets += Candidate(60, 32, MontageFrame("regret_friend", 32, SceneArt.COFFEE_SHOP, TimeOfDay.EVENING,
            listOf(a(P, 0.40f, Facing.RIGHT, Emotion.SAD, seated = true), a(ActorId.FRIEND, 0.66f, Facing.RIGHT, Emotion.DISAPPOINTED)), "A friendship you couldn’t keep."))
        if (s.has(Flags.IGNORED_PARENTS)) regrets += Candidate(60, 50, MontageFrame("regret_parents", 50, SceneArt.LIVING_ROOM, TimeOfDay.EVENING,
            listOf(a(P, 0.40f, Facing.RIGHT, Emotion.SAD), a(ActorId.MOTHER, 0.66f, Facing.LEFT, Emotion.SAD, Gesture.HEAD_DOWN)), "Calls you meant to make."))
        if (s.has(Flags.WORKED_TOO_MUCH) && s.stat(Stat.FAMILY) < 55) regrets += Candidate(55, 40, MontageFrame("regret_work", 40, SceneArt.OFFICE, TimeOfDay.NIGHT,
            listOf(a(P, 0.5f, Facing.RIGHT, Emotion.TIRED, Gesture.TYPE, seated = true)), "Years given to work, and not enough to home.", Shot.MEDIUM))
        c += regrets.take(2)

        val chosen = c.sortedByDescending { it.weight }.take(9).sortedBy { it.order }.map { it.frame }
        return EndingPlan(chosen, finale(s), "You look back on everything you’ve been through…")
    }

    /** The older player in a place that fits the life they lived. */
    fun finale(s: GameState): MontageFrame {
        val age = s.ageYears
        val married = s.relationship == RelationshipStatus.MARRIED
        val cast = mutableListOf<MontageActor>()
        val family = s.children > 0 && s.stat(Stat.FAMILY) >= 55
        val env = when {
            s.businessStage >= 2 || s.has("sold_business") || s.has("passed_on_business") -> SceneArt.SMALL_BUSINESS
            family -> SceneArt.LIVING_ROOM
            s.lastCareer == CareerTrack.SKILLED_WORKER -> SceneArt.WORKSHOP
            s.lastCareerLevel >= 4 && s.stat(Stat.FAMILY) < 55 -> SceneArt.SKYLINE
            s.has(Flags.FIRST_JOB) && s.lastCareer in listOf(CareerTrack.ENGINEER, CareerTrack.TECH, CareerTrack.MANAGER, CareerTrack.EMPLOYEE) && s.lastCareerLevel >= 4 -> SceneArt.OFFICE
            else -> SceneArt.PARK
        }
        val time = when (env) {
            SceneArt.LIVING_ROOM -> TimeOfDay.EVENING
            SceneArt.SKYLINE -> TimeOfDay.EVENING
            else -> TimeOfDay.EVENING
        }
        cast += MontageActor(ActorId.PLAYER, if (married) 0.42f else 0.5f, Facing.RIGHT, Emotion.THOUGHTFUL, Gesture.NONE, seated = true)
        if (married) cast += MontageActor(ActorId.PARTNER, 0.60f, Facing.LEFT, Emotion.HAPPY, Gesture.GENTLE_SMILE, seated = true)
        return MontageFrame("finale", age, env, time, cast, "", Shot.PUSH_IN)
    }
}
