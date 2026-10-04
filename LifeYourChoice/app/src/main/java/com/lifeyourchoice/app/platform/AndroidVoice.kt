package com.lifeyourchoice.app.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.lifeyourchoice.app.R
import com.lifeyourchoice.app.ui.state.VoiceLine
import com.lifeyourchoice.app.ui.state.VoiceManager
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Speaks with the device's own text-to-speech engine, and only with a voice that works offline.
 * If the device has no usable offline voice, [available] stays false and the game simply shows subtitles.
 * Nothing here ever contacts the internet.
 */
class AndroidTtsVoice(context: Context) : VoiceManager {
    private val main = Handler(Looper.getMainLooper())
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()
    private var tts: TextToSpeech? = null
    @Volatile private var usable = false
    private var counter = 0

    override val available: Boolean get() = usable

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) prepare() else usable = false
            }
        } catch (e: Exception) {
            tts = null
            usable = false
        }
    }

    private fun prepare() {
        try {
            val t = tts ?: return
            val offline = t.voices.orEmpty()
                .filter { v ->
                    v.locale.language == Locale.ENGLISH.language &&
                        !v.isNetworkConnectionRequired &&
                        !v.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                }
                .sortedWith(compareByDescending<android.speech.tts.Voice> { it.locale == Locale.US }.thenByDescending { it.quality })
            val chosen = offline.firstOrNull()
            if (chosen == null) {
                usable = false
                return
            }
            t.setVoice(chosen)
            t.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            )
            t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) = finish(utteranceId)
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = finish(utteranceId)
                override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
            })
            usable = true
        } catch (e: Exception) {
            usable = false
        }
    }

    private fun finish(id: String?) {
        val cb = callbacks.remove(id ?: return) ?: return
        main.post { cb() }
    }

    override fun speak(line: VoiceLine, volume: Float, onDone: () -> Unit): Boolean {
        val t = tts ?: return false
        if (!usable || line.text.isBlank()) return false
        return try {
            t.setPitch(line.pitch)
            t.setSpeechRate(line.rate)
            val id = "ly-${counter++}"
            val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }
            callbacks[id] = onDone
            val ok = t.speak(line.text, TextToSpeech.QUEUE_FLUSH, params, id) == TextToSpeech.SUCCESS
            if (!ok) callbacks.remove(id)
            ok
        } catch (e: Exception) {
            false
        }
    }

    override fun stop() {
        // Drop callbacks first so a stopped line can never release the hold of the next one.
        callbacks.clear()
        try { tts?.stop() } catch (e: Exception) { /* ignore */ }
    }

    override fun release() {
        stop()
        try { tts?.shutdown() } catch (e: Exception) { /* ignore */ }
        tts = null
        usable = false
    }
}

/**
 * Plays recorded dialogue bundled in the app as res/raw/voice_<clipKey>.(ogg|wav|mp3).
 * No recordings ship with this version, so [available] is false until a voice pack is added;
 * the lookup is by name, so adding files requires no code or scene changes.
 */
class AndroidClipVoice(private val context: Context) : VoiceManager {
    private val names: Set<String> = try {
        R.raw::class.java.fields.map { it.name }.filter { it.startsWith("voice_") }.toSet()
    } catch (e: Exception) {
        emptySet()
    }
    private var player: MediaPlayer? = null

    override val available: Boolean get() = names.isNotEmpty()

    override fun speak(line: VoiceLine, volume: Float, onDone: () -> Unit): Boolean {
        val key = line.clipKey ?: return false
        val name = "voice_" + key.lowercase().replace(Regex("[^a-z0-9_]"), "_")
        if (name !in names) return false
        return try {
            val id = context.resources.getIdentifier(name, "raw", context.packageName)
            if (id == 0) return false
            stop()
            val mp = MediaPlayer.create(context, id) ?: return false
            mp.setVolume(volume, volume)
            mp.setOnCompletionListener { m -> try { m.release() } catch (e: Exception) { /* ignore */ }; if (player === m) player = null; onDone() }
            player = mp
            mp.start()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun stop() {
        try { player?.let { if (it.isPlaying) it.stop(); it.release() } } catch (e: Exception) { /* ignore */ }
        player = null
    }

    override fun release() = stop()
}
