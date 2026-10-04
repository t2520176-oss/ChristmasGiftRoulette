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
    val timestamp: Long = 0L,
    /** Custom character look (null for lives created before Version 2A). */
    val look: PlayerLook? = null
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
data class Progress(
    val achievements: Set<String> = emptySet(),
    val livesCompleted: Int = 0,
    /** Motto ids already awarded, oldest first (used to avoid repeats). */
    val recentMottoIds: List<String> = emptyList(),
    /** Cinematic scenes the player has already watched (they can be skipped). */
    val viewedScenes: Set<String> = emptySet()
)

@Serializable
data class Settings(
    val soundEffects: Boolean = true,
    val music: Boolean = true,
    /** Legacy master volume from Version 1; kept so old settings files still load. */
    val volume: Float = 0.8f,
    val voiceEnabled: Boolean = true,
    val voiceVolume: Float = 0.9f,
    val musicVolume: Float = 0.7f,
    val sfxVolume: Float = 0.8f,
    /** Subtitles stay on by default; they are also the fallback when no voice is available. */
    val subtitles: Boolean = true,
    /** Cinematic presentation. Off = the Version 1 text-and-illustration screen. */
    val cinematic: Boolean = true
)

/**
 * Everything the shareable LIFE CARD shows: deliberately no statistics. A later version can render
 * this model to an image and share it; Version 1 only draws it on screen.
 */
class LifeCardModel(
    val playerName: String,
    val gender: Gender,
    val appearance: Int,
    val ageReached: Int,
    val careerTitle: String,
    val endingTitle: String,
    val motto: String,
    val look: PlayerLook? = null
) {
    val byline: String get() = "— Your Life, Age $ageReached"

    companion object {
        fun from(r: LifeRecord) =
            LifeCardModel(r.playerName, r.gender, r.appearance, r.ageReached, r.careerTitle, r.endingTitle, r.mottoText, r.look)
    }
}

/**
 * Character-creation choices. Indexes refer to palettes in the UI layer (skin tones, hair, outfit
 * colours, voice types), so the model stays free of rendering details.
 */
@Serializable
data class PlayerLook(
    val skin: Int = 0,
    val hairStyle: Int = 0,
    val hairColor: Int = 0,
    val outfit: Int = 0,
    /** 0 = warm, 1 = bright, 2 = deep (used to pitch the optional on-device voice). */
    val voice: Int = 0
) {
    companion object {
        /** The look a Version 1 preset corresponds to. */
        fun fromPreset(gender: Gender, appearance: Int): PlayerLook = when (appearance.coerceIn(0, 2)) {
            0 -> PlayerLook(skin = 1, hairStyle = 0, hairColor = if (gender == Gender.BOY) 0 else 1, outfit = 0, voice = 0)
            1 -> PlayerLook(skin = 3, hairStyle = 1, hairColor = if (gender == Gender.BOY) 2 else 0, outfit = 1, voice = 1)
            else -> PlayerLook(skin = 4, hairStyle = 2, hairColor = if (gender == Gender.BOY) 0 else 3, outfit = if (gender == Gender.BOY) 2 else 4, voice = 2)
        }
    }
}
