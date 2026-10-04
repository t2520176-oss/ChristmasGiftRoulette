package com.lifeyourchoice.app.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lifeyourchoice.core.engine.Achievements
import com.lifeyourchoice.core.engine.GameEngine
import com.lifeyourchoice.core.engine.PresentedScenario
import com.lifeyourchoice.core.model.ChoiceOutcome
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.LifeRecord
import com.lifeyourchoice.core.model.Progress
import com.lifeyourchoice.core.model.SceneArt
import com.lifeyourchoice.core.model.Settings
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.motto.LifeReportBuilder
import com.lifeyourchoice.core.motto.MottoEngine
import com.lifeyourchoice.core.save.SaveRepository
import com.lifeyourchoice.core.story.StoryLibrary

enum class Sfx { TAP, SELECT, GOOD, BAD, ACHIEVEMENT, PAGE, MOTTO }

/** Sound is optional and platform specific; the game works with [SilentAudio]. */
interface AudioPlayer {
    fun sfx(sfx: Sfx)
    fun applySettings(settings: Settings)
    fun release()
}

object SilentAudio : AudioPlayer {
    override fun sfx(sfx: Sfx) {}
    override fun applySettings(settings: Settings) {}
    override fun release() {}
}

sealed interface Screen {
    data object Menu : Screen
    data object Create : Screen
    data object Intro : Screen
    data object Playing : Screen
    data object Records : Screen
    data object Settings : Screen
    /** [fresh] is true right after a life ends (as opposed to re-opening an old record). */
    class Report(val record: LifeRecord, val fresh: Boolean) : Screen
    class Card(val record: LifeRecord) : Screen
}

/** A read-only snapshot of the life for the HUD (the engine's state is not observable by Compose). */
class Hud(
    val playerName: String,
    val gender: Gender,
    val appearance: Int,
    val ageYears: Int,
    val chapter: String,
    val jobTitle: String,
    val stats: Map<Stat, Int>,
    val recentEvents: List<String>
) {
    fun stat(s: Stat): Int = stats[s] ?: 0
}

class GameSession(
    private val repo: SaveRepository,
    val library: StoryLibrary,
    val audio: AudioPlayer = SilentAudio,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    var screen: Screen by mutableStateOf(Screen.Menu)
        private set
    var settings: Settings by mutableStateOf(repo.loadSettings())
        private set
    var progress: Progress by mutableStateOf(repo.loadProgress())
        private set
    var records: List<LifeRecord> by mutableStateOf(repo.loadRecords())
        private set
    var hasSave: Boolean by mutableStateOf(repo.hasSavedGame())
        private set

    // Current life.
    private var engine: GameEngine? = null
    var hud: Hud? by mutableStateOf(null)
        private set
    var presented: PresentedScenario? by mutableStateOf(null)
        private set
    var outcome: ChoiceOutcome? by mutableStateOf(null)
        private set
    /** Art and title of the scene on screen (kept while the consequence is shown). */
    var stageArt: SceneArt by mutableStateOf(SceneArt.CLASSROOM)
        private set
    var stageTitle: String by mutableStateOf("")
        private set
    var showStats: Boolean by mutableStateOf(false)
    /** Achievements earned for the first time on the last decision (titles), shown as a banner. */
    var achievementBanner: List<String> by mutableStateOf(emptyList())
        private set

    init {
        audio.applySettings(settings)
    }

    // ---------- navigation ----------

    fun goMenu() {
        showStats = false
        screen = Screen.Menu
    }

    fun openCreate() { audio.sfx(Sfx.TAP); screen = Screen.Create }
    fun openRecords() { audio.sfx(Sfx.TAP); records = repo.loadRecords(); screen = Screen.Records }
    fun openSettings() { audio.sfx(Sfx.TAP); screen = Screen.Settings }
    fun openReport(record: LifeRecord) { audio.sfx(Sfx.PAGE); screen = Screen.Report(record, fresh = false) }
    fun openCard(record: LifeRecord) { audio.sfx(Sfx.TAP); screen = Screen.Card(record) }

    /** Android back button / gesture. Returns false when the app should close. */
    fun back(): Boolean {
        if (showStats) { showStats = false; return true }
        return when (val s = screen) {
            Screen.Menu -> false
            is Screen.Card -> { screen = Screen.Report(s.record, fresh = false); true }
            else -> { goMenu(); true }
        }
    }

    // ---------- life flow ----------

    fun startNewLife(name: String, gender: Gender, appearance: Int) {
        audio.sfx(Sfx.SELECT)
        val e = GameEngine.newLife(library, name, gender, appearance)
        engine = e
        repo.saveGame(e.state)
        hasSave = true
        refreshFromEngine()
        screen = Screen.Intro
    }

    fun beginPlaying() { screen = Screen.Playing }

    fun continueLife() {
        audio.sfx(Sfx.TAP)
        val state = repo.loadGame()
        if (state == null) { hasSave = false; return }
        val e = GameEngine(state, library)
        engine = e
        if (e.isFinished) { finishLife(e); return }
        refreshFromEngine()
        screen = Screen.Playing
    }

    fun choose(choiceIndex: Int) {
        val e = engine ?: return
        if (outcome != null) return
        val out = try { e.choose(choiceIndex) } catch (ex: IllegalStateException) { return } catch (ex: IllegalArgumentException) { return }
        repo.saveGame(e.state)
        val newlyEarned = out.newAchievements.filter { it !in progress.achievements }
        if (newlyEarned.isNotEmpty()) {
            progress = Progress(progress.achievements + newlyEarned, progress.livesCompleted, progress.recentMottoIds)
            repo.saveProgress(progress)
            achievementBanner = newlyEarned.mapNotNull { Achievements.get(it)?.title }
        } else {
            achievementBanner = emptyList()
        }
        val good = out.deltas.sumOf { it.delta }
        audio.sfx(if (newlyEarned.isNotEmpty()) Sfx.ACHIEVEMENT else if (good >= 0) Sfx.GOOD else Sfx.BAD)
        outcome = out
        refreshHud(e)
    }

    /** CONTINUE on the consequence screen. */
    fun acknowledgeOutcome() {
        val e = engine ?: return
        audio.sfx(Sfx.PAGE)
        e.acknowledgeOutcome()
        outcome = null
        achievementBanner = emptyList()
        if (e.isFinished) {
            finishLife(e)
        } else {
            repo.saveGame(e.state)
            refreshFromEngine()
        }
    }

    private fun finishLife(e: GameEngine) {
        val record = LifeReportBuilder.build(e.state, progress.livesCompleted + 1, progress.recentMottoIds, clock())
        repo.addRecord(record)
        val earned = progress.achievements + e.state.achievementsThisLife
        progress = Progress(
            achievements = earned,
            livesCompleted = progress.livesCompleted + 1,
            recentMottoIds = (progress.recentMottoIds + record.mottoId).takeLast(MottoEngine.RECENT_LIMIT)
        )
        repo.saveProgress(progress)
        repo.clearGame()
        hasSave = false
        records = repo.loadRecords()
        engine = null
        presented = null
        outcome = null
        hud = null
        audio.sfx(Sfx.MOTTO)
        screen = Screen.Report(record, fresh = true)
    }

    private fun refreshFromEngine() {
        val e = engine ?: return
        outcome = e.pendingOutcome
        presented = if (outcome == null) e.present() else null
        val p = presented
        if (p != null) {
            stageArt = p.art
            stageTitle = p.title
        } else {
            val old = outcome?.let { library[it.scenarioId] }
            if (old != null) { stageArt = old.art; stageTitle = old.title }
        }
        refreshHud(e)
    }

    private fun refreshHud(e: GameEngine) {
        val s = e.state
        hud = Hud(
            playerName = s.playerName, gender = s.gender, appearance = s.appearance, ageYears = s.ageYears,
            chapter = s.chapter.label, jobTitle = s.jobTitle,
            stats = Stat.values().associateWith { s.stat(it) },
            recentEvents = s.recentEvents.toList()
        )
    }

    // ---------- settings ----------

    fun updateSettings(new: Settings) {
        settings = new
        repo.saveSettings(new)
        audio.applySettings(new)
    }

    fun resetProgress() {
        repo.wipeProgress()
        engine = null
        hud = null
        presented = null
        outcome = null
        progress = repo.loadProgress()
        records = repo.loadRecords()
        hasSave = false
        screen = Screen.Menu
    }

    fun release() = audio.release()
}
