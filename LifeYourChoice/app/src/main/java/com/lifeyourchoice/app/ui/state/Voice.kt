package com.lifeyourchoice.app.ui.state

import com.lifeyourchoice.core.cinema.ActorId
import com.lifeyourchoice.core.cinema.Emotion
import com.lifeyourchoice.core.model.Gender

/**
 * One line of speech. [clipKey] names an optional bundled recording (e.g. "sch_skip_school_friend_1");
 * [pitch]/[rate] describe how an on-device voice should sound for this speaker and mood.
 */
class VoiceLine(
    val text: String,
    val clipKey: String?,
    val speaker: ActorId,
    val pitch: Float,
    val rate: Float
)

/**
 * Speech output, kept separate from story logic so voice packs can be added later without touching
 * scenes. Implementations may be bundled recordings, the device's offline text-to-speech, or nothing.
 * The game is fully playable with [SilentVoice] (subtitles only).
 */
interface VoiceManager {
    /** True when this manager may be able to speak (clips shipped, or an offline voice was found). */
    val available: Boolean

    /** Starts speaking. Returns false when nothing could be spoken (the caller then relies on subtitles). */
    fun speak(line: VoiceLine, volume: Float, onDone: () -> Unit): Boolean

    fun stop()

    fun release()
}

object SilentVoice : VoiceManager {
    override val available = false
    override fun speak(line: VoiceLine, volume: Float, onDone: () -> Unit) = false
    override fun stop() {}
    override fun release() {}
}

/** Tries recorded clips first, then on-device text-to-speech, otherwise reports "not spoken". */
class CompositeVoiceManager(private val clips: VoiceManager?, private val tts: VoiceManager?) : VoiceManager {
    override val available: Boolean get() = (clips?.available == true) || (tts?.available == true)

    override fun speak(line: VoiceLine, volume: Float, onDone: () -> Unit): Boolean {
        stop()
        if (clips != null && line.clipKey != null && clips.speak(line, volume, onDone)) return true
        if (tts != null && tts.available && tts.speak(line, volume, onDone)) return true
        return false
    }

    override fun stop() { clips?.stop(); tts?.stop() }

    override fun release() { clips?.release(); tts?.release() }
}

/** Chooses a voice character from who is speaking and how they feel. */
object VoiceProfiles {
    fun forLine(speaker: ActorId, gender: Gender, age: Int, voiceType: Int, emotion: Emotion): Pair<Float, Float> {
        var pitch = if (gender == Gender.GIRL) 1.12f else 0.92f
        if (speaker == ActorId.PLAYER) pitch += when (voiceType) { 1 -> 0.14f; 2 -> -0.16f; else -> 0f }
        if (age >= 55) pitch -= 0.08f
        if (speaker == ActorId.CHILD) pitch = 1.45f
        if (speaker == ActorId.FATHER) pitch = 0.78f
        if (speaker == ActorId.NARRATOR) pitch = 0.86f
        var rate = 1f
        when (emotion) {
            Emotion.SAD, Emotion.TIRED, Emotion.DISAPPOINTED -> { rate = 0.86f; pitch -= 0.05f }
            Emotion.EXCITED, Emotion.LAUGHING -> { rate = 1.12f; pitch += 0.06f }
            Emotion.ANGRY -> { rate = 1.06f; pitch -= 0.04f }
            Emotion.AFRAID, Emotion.WORRIED -> { rate = 1.05f; pitch += 0.04f }
            Emotion.THOUGHTFUL -> rate = 0.92f
            else -> {}
        }
        if (speaker == ActorId.NARRATOR) rate = 0.95f
        return pitch.coerceIn(0.5f, 2f) to rate.coerceIn(0.6f, 1.6f)
    }
}
