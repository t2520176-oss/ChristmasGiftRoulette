package com.lifeyourchoice.core.model

import kotlinx.serialization.Serializable

/** A completed life, kept locally in LIFE RECORDS. Includes the lessons and the life's motto. */
@Serializable
class LifeRecord(
    val number: Int,
    val playerName: String,
    val gender: Gender,
    val appearance: Int,
    val ageReached: Int,
    val careerTitle: String,
    val endingId: String,
    val endingTitle: String,
    val endingTagline: String,
    val wealth: Int,
    val family: Int,
    val health: Int,
    val reputation: Int,
    val friendship: Int,
    val happiness: Int,
    /** "YOUR STORY": the personalised paragraph. */
    val summary: String,
    val importantChoices: List<Milestone>,
    /** "WHAT YOU LEARNED": exactly three short lessons. */
    val lessons: List<String>,
    val mottoId: String,
    val mottoText: String,
    val mottoCategory: String,
    val achievements: List<String> = emptyList(),
    val timestamp: Long = 0L
) {
    val familyLabel: String get() = valueLabel(family)

    companion object {
        fun valueLabel(v: Int): String = when {
            v >= 75 -> "Strong"
            v >= 50 -> "Steady"
            v >= 30 -> "Distant"
            else -> "Broken"
        }
    }
}

/** Everything that persists across lives. */
@Serializable
class Progress(
    val achievements: Set<String> = emptySet(),
    val livesCompleted: Int = 0,
    /** Motto ids already awarded, oldest first (used to avoid repeats). */
    val recentMottoIds: List<String> = emptyList()
)

@Serializable
class Settings(
    val soundEffects: Boolean = true,
    val music: Boolean = true,
    val volume: Float = 0.8f
)
