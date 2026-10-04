package com.lifeyourchoice.app.platform

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lifeyourchoice.app.ui.state.CompositeVoiceManager
import com.lifeyourchoice.app.ui.state.GameSession
import com.lifeyourchoice.core.save.FileSaveRepository
import com.lifeyourchoice.core.story.StoryContent
import java.io.File

/** Owns the [GameSession] so a rotation or process-level config change never loses the current scene. */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    val audio = AndroidAudio(application)

    /** Recorded clips first (none shipped yet), then offline on-device speech, otherwise subtitles only. */
    private val voice = CompositeVoiceManager(AndroidClipVoice(application), AndroidTtsVoice(application))
    val session = GameSession(
        repo = FileSaveRepository(File(application.filesDir, "life_your_choice")),
        library = StoryContent.library(),
        audio = audio,
        voice = voice
    )

    override fun onCleared() {
        session.release()
    }
}
