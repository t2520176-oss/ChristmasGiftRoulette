package com.lifeyourchoice.app.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.lifeyourchoice.app.R
import com.lifeyourchoice.app.ui.state.AudioPlayer
import com.lifeyourchoice.app.ui.state.Sfx
import com.lifeyourchoice.core.model.Settings

/**
 * Sound effects (SoundPool) and calm looping background music (MediaPlayer), all bundled in the app.
 * Every call is guarded: a device without audio support must never crash the game.
 */
class AndroidAudio(private val context: Context) : AudioPlayer {
    private var pool: SoundPool? = null
    private val ids = mutableMapOf<Sfx, Int>()
    private var music: MediaPlayer? = null
    private var settings = Settings()
    private var inForeground = true

    init {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val p = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
            ids[Sfx.TAP] = p.load(context, R.raw.sfx_tap, 1)
            ids[Sfx.SELECT] = p.load(context, R.raw.sfx_select, 1)
            ids[Sfx.GOOD] = p.load(context, R.raw.sfx_good, 1)
            ids[Sfx.BAD] = p.load(context, R.raw.sfx_bad, 1)
            ids[Sfx.ACHIEVEMENT] = p.load(context, R.raw.sfx_achievement, 1)
            ids[Sfx.PAGE] = p.load(context, R.raw.sfx_page, 1)
            ids[Sfx.MOTTO] = p.load(context, R.raw.sfx_motto, 1)
            pool = p
        } catch (e: Exception) {
            pool = null
        }
    }

    override fun sfx(sfx: Sfx) {
        if (!settings.soundEffects) return
        try {
            val id = ids[sfx] ?: return
            val v = settings.volume.coerceIn(0f, 1f)
            pool?.play(id, v, v, 1, 0, 1f)
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun applySettings(settings: Settings) {
        this.settings = settings
        updateMusic()
    }

    /** Called from the activity lifecycle so music never plays in the background. */
    fun setForeground(foreground: Boolean) {
        inForeground = foreground
        updateMusic()
    }

    private fun updateMusic() {
        try {
            val shouldPlay = settings.music && inForeground
            if (shouldPlay) {
                val mp = music ?: MediaPlayer.create(context, R.raw.music_calm)?.apply { isLooping = true }.also { music = it }
                mp?.setVolume(settings.volume * 0.4f, settings.volume * 0.4f)
                if (mp != null && !mp.isPlaying) mp.start()
            } else {
                music?.let { if (it.isPlaying) it.pause() }
            }
        } catch (e: Exception) {
            music = null
        }
    }

    override fun release() {
        try { pool?.release() } catch (e: Exception) {}
        try { music?.release() } catch (e: Exception) {}
        pool = null
        music = null
    }
}
