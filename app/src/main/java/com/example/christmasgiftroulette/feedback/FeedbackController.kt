package com.example.christmasgiftroulette.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.christmasgiftroulette.data.AppSettings
import java.util.concurrent.ConcurrentHashMap

/** Sound/vibration cues. Each effect maps to a raw resource name; missing files are skipped silently. */
enum class SoundEffect(val rawName: String) {
    TICK("sfx_tick"),
    SPIN_START("sfx_spin"),
    WIN("sfx_win"),
}

/**
 * Plays optional sound effects and haptics. Never throws: if an audio resource is missing, or the
 * device has no vibrator, the call is a no-op. Drop replacement files with the same names into
 * `res/raw` (see README) to change the sounds.
 */
class FeedbackController(context: Context) {

    private val appContext = context.applicationContext
    private val soundPool: SoundPool? = runCatching {
        SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    }.getOrNull()
    private val vibrator: Vibrator? = runCatching { appContext.getSystemService(Vibrator::class.java) }.getOrNull()

    private val soundIds = HashMap<SoundEffect, Int>()
    private val ready: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    private var lastTickAt = 0L

    init {
        soundPool?.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) ready += sampleId }
        SoundEffect.entries.forEach { effect ->
            runCatching {
                val resId = appContext.resources.getIdentifier(effect.rawName, "raw", appContext.packageName)
                if (resId != 0) soundPool?.load(appContext, resId, 1)?.let { soundIds[effect] = it }
            }
        }
    }

    fun tick(settings: AppSettings) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastTickAt < MIN_TICK_INTERVAL_MS) return
        lastTickAt = now
        play(SoundEffect.TICK, settings, volume = 0.5f)
        vibrate(settings, 12L, 70)
    }

    fun spinStarted(settings: AppSettings) {
        play(SoundEffect.SPIN_START, settings)
        vibrate(settings, 30L, 120)
    }

    fun celebrate(settings: AppSettings) {
        play(SoundEffect.WIN, settings)
        if (settings.vibrationEnabled) {
            runCatching {
                if (vibrator?.hasVibrator() == true) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 60, 60, 60, 140), -1))
                }
            }
        }
    }

    fun release() {
        runCatching { soundPool?.release() }
    }

    private fun play(effect: SoundEffect, settings: AppSettings, volume: Float = 1f) {
        if (!settings.soundEnabled) return
        val id = soundIds[effect] ?: return
        if (id !in ready) return
        runCatching { soundPool?.play(id, volume, volume, 1, 0, 1f) }
    }

    private fun vibrate(settings: AppSettings, millis: Long, amplitude: Int) {
        if (!settings.vibrationEnabled) return
        runCatching {
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createOneShot(millis, amplitude))
            }
        }
    }

    private companion object {
        const val MIN_TICK_INTERVAL_MS = 45L
    }
}
