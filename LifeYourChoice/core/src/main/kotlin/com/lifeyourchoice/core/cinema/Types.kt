package com.lifeyourchoice.core.cinema

import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.NpcRole

/** Facial/body mood. The renderer maps each one to eyebrows, eyes, mouth and posture. */
enum class Emotion {
    NEUTRAL, HAPPY, LAUGHING, CONFIDENT, WORRIED, SAD, ANGRY, SURPRISED, EMBARRASSED,
    DISAPPOINTED, PROUD, AFRAID, THOUGHTFUL, TIRED, EXCITED
}

/** Arm/head performances. Gestures are reusable across every scene and every character. */
enum class Gesture {
    NONE, EXPLAIN, POINT, SHRUG, WAVE, CROSS_ARMS, THINK, HEAD_DOWN, HANDS_UP, NOD, SHAKE_HEAD,
    HANDSHAKE, HUG, PHONE, TYPE, CLAP, FACEPALM, SCRATCH_HEAD, FIST_PUMP, GENTLE_SMILE
}

/** Camera framings the cinematic director can ask for. */
enum class Shot { ESTABLISHING, WIDE, MEDIUM, CLOSE_UP, OVER_SHOULDER, TWO_SHOT, REACTION, FOLLOW, PUSH_IN }

enum class Facing { LEFT, RIGHT }

enum class Edge { LEFT, RIGHT }

/** Simple hand-held objects: enough to make a scene feel lived in. */
enum class Prop { NONE, PHONE, LAPTOP, CUP, BAG, BOOK, DOCUMENTS, BALL, SUITCASE, FLOWERS, BOX }

enum class TimeOfDay { MORNING, DAY, EVENING, NIGHT }

enum class TitleKind { CHAPTER, CAPTION, CARD }

/**
 * Everyone who can appear on screen. NPC roles come from the life's persistent cast (so they keep their
 * name and remember the player); the others are everyday extras. [ageOffset] is added to the player's age.
 */
enum class ActorId(val label: String, val role: NpcRole?, val ageOffset: Int, val adult: Boolean = true) {
    PLAYER("You", null, 0),
    FRIEND("Friend", NpcRole.BEST_FRIEND, 0),
    UNDERDOG("Classmate", NpcRole.UNDERDOG, 0),
    RIVAL("Rival", NpcRole.RIVAL, 0),
    MENTOR("Mentor", NpcRole.MENTOR, 14),
    PARTNER("Partner", NpcRole.PARTNER, 0),
    SIBLING("Sibling", NpcRole.SIBLING, -1),
    BOSS("Boss", NpcRole.BOSS, 10),
    COWORKER("Coworker", NpcRole.COWORKER, 0),
    FATHER("Father", null, 28),
    MOTHER("Mother", null, 26),
    TEACHER("Teacher", null, 24),
    INTERVIEWER("Interviewer", null, 12),
    DOCTOR("Doctor", null, 10),
    CUSTOMER("Customer", null, 5),
    CHILD("Child", null, -28, adult = false),
    STRANGER("Stranger", null, 6),
    NARRATOR("", null, 0);

    val hasBody: Boolean get() = this != NARRATOR

    fun displayName(s: GameState): String = when {
        this == PLAYER -> s.playerName
        this == NARRATOR -> ""
        role != null -> s.npcs[role]?.name ?: label
        else -> label
    }

    fun gender(s: GameState): Gender = when {
        this == PLAYER -> s.gender
        role != null -> s.npcs[role]?.gender ?: Gender.BOY
        this == FATHER -> Gender.BOY
        this == MOTHER -> Gender.GIRL
        else -> if ((s.seed + ordinal) % 2L == 0L) Gender.BOY else Gender.GIRL
    }

    /** Apparent age for rendering (hair greying, glasses, size). */
    fun age(s: GameState): Int = (s.ageYears + ageOffset).coerceIn(if (adult) 14 else 3, 95)
}
