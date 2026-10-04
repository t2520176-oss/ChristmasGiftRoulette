package com.lifeyourchoice.app.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lifeyourchoice.app.ui.cinema.StageRuntime
import com.lifeyourchoice.app.ui.cinema.castFor
import com.lifeyourchoice.core.cinema.ActorId
import com.lifeyourchoice.core.cinema.Beat
import com.lifeyourchoice.core.cinema.ChapterCards
import com.lifeyourchoice.core.cinema.Caption
import com.lifeyourchoice.core.cinema.CineDirector
import com.lifeyourchoice.core.cinema.CineLibrary
import com.lifeyourchoice.core.cinema.CineScript
import com.lifeyourchoice.core.cinema.EndingPlan
import com.lifeyourchoice.core.cinema.MontageBuilder
import com.lifeyourchoice.core.cinema.TitleState
import com.lifeyourchoice.core.cinema.Phase
import com.lifeyourchoice.core.cinema.scripts.CineContent
import com.lifeyourchoice.core.engine.Achievements
import com.lifeyourchoice.core.engine.GameEngine
import com.lifeyourchoice.core.engine.PresentedScenario
import com.lifeyourchoice.core.model.ChoiceOutcome
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.LifeRecord
import com.lifeyourchoice.core.model.PlayerLook
import com.lifeyourchoice.core.model.Progress
import com.lifeyourchoice.core.model.SceneArt
import com.lifeyourchoice.core.model.Settings
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.model.StatDelta
import com.lifeyourchoice.core.motto.LifeReportBuilder
import com.lifeyourchoice.core.motto.MottoEngine
import com.lifeyourchoice.core.save.SaveRepository
import com.lifeyourchoice.core.story.StoryLibrary

enum class Sfx { TAP, SELECT, GOOD, BAD, ACHIEVEMENT, PAGE, MOTTO, KNOCK, WHOOSH, PHONE, CHEER, DOOR, BELL }

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
    /** The cinematic that closes a life, before the report. */
    class Ending(val record: LifeRecord, val plan: EndingPlan, val state: com.lifeyourchoice.core.model.GameState) : Screen
    /** [fresh] is true right after a life ends (as opposed to re-opening an old record). */
    class Report(val record: LifeRecord, val fresh: Boolean) : Screen
    class Card(val record: LifeRecord) : Screen
}

/** A read-only snapshot of the life for the HUD (the engine's state is not observable by Compose). */
class Hud(
    val playerName: String,
    val gender: Gender,
    val appearance: Int,
    val look: PlayerLook?,
    val ageYears: Int,
    val chapter: String,
    val jobTitle: String,
    val stats: Map<Stat, Int>,
    val recentEvents: List<String>
) {
    fun stat(s: Stat): Int = stats[s] ?: 0
}

/** The brief result shown (and then faded) after a decision: stat changes, a hint, achievements. */
class Toast(val deltas: List<StatDelta>, val hint: Boolean, val achievements: List<String>, val serial: Int)

class GameSession(
    private val repo: SaveRepository,
    val library: StoryLibrary,
    val audio: AudioPlayer = SilentAudio,
    val voice: VoiceManager = SilentVoice,
    private val cinema: CineLibrary = CineContent.library(),
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

    // Cinematic playback.
    var director: CineDirector? by mutableStateOf(null)
        private set
    var stage: StageRuntime? by mutableStateOf(null)
        private set
    var script: CineScript? by mutableStateOf(null)
        private set
    var toast: Toast? by mutableStateOf(null)
        private set

    // The director is plain Kotlin (no Compose), so the screen cannot observe it directly. These mirrors are
    // refreshed every frame and only change (and so only recompose the UI) when a line, card or phase changes.
    var cineCaption: Caption? by mutableStateOf(null)
        private set
    var cineTitle: TitleState? by mutableStateOf(null)
        private set
    var cinePhase: Phase by mutableStateOf(Phase.PLAYING)
        private set
    private var toastSerial = 0
    private var reactionEnded = false
    private var cineOutcome: ChoiceOutcome? = null

    init {
        audio.applySettings(settings)
    }

    // ---------- navigation ----------

    fun goMenu() {
        showStats = false
        voice.stop()
        screen = Screen.Menu
    }

    fun openCreate() { audio.sfx(Sfx.TAP); screen = Screen.Create }
    fun openRecords() { audio.sfx(Sfx.TAP); records = repo.loadRecords(); screen = Screen.Records }
    fun openSettings() { audio.sfx(Sfx.TAP); screen = Screen.Settings }
    fun openReport(record: LifeRecord) { audio.sfx(Sfx.PAGE); screen = Screen.Report(record, fresh = false) }
    fun openCard(record: LifeRecord) { audio.sfx(Sfx.TAP); screen = Screen.Card(record) }

    /** Called by the ending screen when it is finished (or skipped). */
    fun endingFinished() {
        val s = screen
        if (s is Screen.Ending) screen = Screen.Report(s.record, fresh = true)
    }

    /** Android back button / gesture. Returns false when the app should close. */
    fun back(): Boolean {
        if (showStats) { showStats = false; return true }
        return when (val s = screen) {
            Screen.Menu -> false
            is Screen.Card -> { screen = Screen.Report(s.record, fresh = false); true }
            is Screen.Ending -> { screen = Screen.Report(s.record, fresh = true); true }
            else -> { goMenu(); true }
        }
    }

    // ---------- life flow ----------

    fun startNewLife(name: String, gender: Gender, appearance: Int, look: PlayerLook? = null) {
        audio.sfx(Sfx.SELECT)
        val e = GameEngine.newLife(library, name, gender, appearance, look = look)
        engine = e
        repo.saveGame(e.state)
        hasSave = true
        refreshFromEngine()
        screen = Screen.Intro
    }

    fun beginPlaying() {
        screen = Screen.Playing
        if (settings.cinematic) startScene()
    }

    fun continueLife() {
        audio.sfx(Sfx.TAP)
        val state = repo.loadGame()
        if (state == null) { hasSave = false; return }
        val e = GameEngine(state, library)
        engine = e
        if (e.isFinished) { finishLife(e); return }
        refreshFromEngine()
        screen = Screen.Playing
        if (settings.cinematic) {
            // A consequence that was waiting when the app closed is simply shown as a toast.
            if (e.pendingOutcome != null) {
                cineOutcome = e.pendingOutcome
                endCineScene()
            } else startScene()
        }
    }

    /** Version 1 (classic) decision: applies the choice and shows the consequence overlay. */
    fun choose(choiceIndex: Int) {
        if (outcome != null) return
        val out = commitChoice(choiceIndex) ?: return
        outcome = out
        engine?.let { refreshHud(it) }
    }

    /** CONTINUE on the classic consequence screen. */
    fun acknowledgeOutcome() {
        val e = engine ?: return
        audio.sfx(Sfx.PAGE)
        e.acknowledgeOutcome()
        outcome = null
        achievementBanner = emptyList()
        if (e.isFinished) finishLife(e) else { repo.saveGame(e.state); refreshFromEngine() }
    }

    /** Applies a choice in the engine, saves, awards achievements. Shared by classic and cinematic play. */
    private fun commitChoice(choiceIndex: Int): ChoiceOutcome? {
        val e = engine ?: return null
        val out = try { e.choose(choiceIndex) } catch (ex: IllegalStateException) { return null } catch (ex: IllegalArgumentException) { return null }
        repo.saveGame(e.state)
        val newlyEarned = out.newAchievements.filter { it !in progress.achievements }
        if (newlyEarned.isNotEmpty()) {
            progress = progress.copy(achievements = progress.achievements + newlyEarned)
            repo.saveProgress(progress)
            achievementBanner = newlyEarned.mapNotNull { Achievements.get(it)?.title }
        } else achievementBanner = emptyList()
        val good = out.deltas.sumOf { it.delta }
        audio.sfx(if (newlyEarned.isNotEmpty()) Sfx.ACHIEVEMENT else if (good >= 0) Sfx.GOOD else Sfx.BAD)
        return out
    }

    // ---------- cinematic play ----------

    /** Builds the movie for the current scenario: title cards, cast, director. */
    private fun startScene() {
        val e = engine ?: return
        val p = e.present() ?: run { refreshFromEngine(); return }
        val sc = library[p.id] ?: return
        val st = e.state
        val sc2 = cinema.scriptFor(sc, st)
        val pre = ArrayList<Beat>()
        // Branch scenes continue the same moment: no cards.
        if (!sc.branchOnly) {
            (ChapterCards.cardFor(st) ?: ChapterCards.timeSkipCard(st))?.let { pre += it }
        }
        ChapterCards.markShown(st)
        val d = CineDirector(sc2, st, pre)
        val cast = castFor(st, st.ageYears, sc2.env)
        val rt = StageRuntime(cast)
        rt.reset(sc2.env, sc2.time, cast)
        script = sc2
        director = d
        cineCaption = null
        syncCinemaUi(d)
        stage = rt
        presented = p
        stageArt = p.art
        stageTitle = p.title
        reactionEnded = false
        cineOutcome = null
        refreshHud(e)
    }

    /** Called every frame by the cinema screen. */
    fun tickCinema(dtMs: Int) {
        val d = director ?: return
        val rt = stage ?: return
        d.update(dtMs)
        rt.update(dtMs, d)
        syncCinemaUi(d)
        for (cue in d.drainCues()) cueSfx(cue)
        if (d.phase == Phase.CHOOSING) markViewed()
        if (d.phase == Phase.DONE && cineOutcome != null && !reactionEnded) endCineScene()
    }

    private fun syncCinemaUi(d: CineDirector) {
        // The last spoken line stays on screen while characters walk, and goes away with a title card.
        val c = d.caption
        if (c != null) cineCaption = c else if (d.title != null) cineCaption = null
        cineTitle = d.title
        cinePhase = d.phase
    }

    private fun cueSfx(cue: String) {
        audio.sfx(when (cue) {
            "door" -> Sfx.DOOR
            "knock" -> Sfx.KNOCK
            "whoosh", "train" -> Sfx.WHOOSH
            "phone", "beep" -> Sfx.PHONE
            "cheer" -> Sfx.CHEER
            "bell", "bells" -> Sfx.BELL
            else -> Sfx.TAP
        })
    }

    private fun markViewed() {
        val id = script?.scenarioId ?: return
        if (id !in progress.viewedScenes) {
            progress = progress.copy(viewedScenes = progress.viewedScenes + id)
            repo.saveProgress(progress)
        }
    }

    /** True when this cinematic was watched before, so SKIP may be offered. */
    val canSkip: Boolean
        get() = script?.scenarioId?.let { it in progress.viewedScenes } == true && cinePhase != Phase.CHOOSING

    fun tapCinema() { director?.let { it.tap(); syncCinemaUi(it) } }

    fun skipCinema() { director?.let { it.skip(); syncCinemaUi(it) } }

    /** The player picked one of the choices at the end of the scene. */
    fun chooseCinematic(choiceIndex: Int) {
        val d = director ?: return
        if (d.phase != Phase.CHOOSING) return
        audio.sfx(Sfx.SELECT)
        val out = commitChoice(choiceIndex) ?: return
        cineOutcome = out
        d.choose(choiceIndex, out)
        syncCinemaUi(d)
        engine?.let { refreshHud(it) }
    }

    private fun endCineScene() {
        if (reactionEnded) return
        reactionEnded = true
        val e = engine ?: return
        val out = cineOutcome
        voice.stop()
        if (out != null) {
            val showHint = out.hint && (e.state.step % 3 == 0)
            toast = Toast(out.deltas, showHint, achievementBanner, ++toastSerial)
        }
        e.acknowledgeOutcome()
        achievementBanner = emptyList()
        if (e.isFinished) {
            finishLife(e)
        } else {
            repo.saveGame(e.state)
            refreshHud(e)
            startScene()
        }
    }

    fun clearToast(serial: Int) { if (toast?.serial == serial) toast = null }

    /** The live life state, for filling name/pronoun placeholders in line labels. */
    fun stateForText() = engine!!.state

    /** The Look the cinematic voice should use for the player. */
    val playerVoiceType: Int get() = engine?.state?.playerLook?.voice ?: 0

    /** True when recorded clips or an offline device voice can speak; otherwise the game is subtitle-only. */
    val voiceAvailable: Boolean get() = voice.available

    fun speak(line: VoiceLine, onDone: () -> Unit): Boolean {
        if (!settings.voiceEnabled || !voice.available) return false
        return voice.speak(line, settings.voiceVolume, onDone)
    }

    fun stopVoice() = voice.stop()

    // ---------- ending ----------

    private fun finishLife(e: GameEngine) {
        val state = e.state
        val record = LifeReportBuilder.build(state, progress.livesCompleted + 1, progress.recentMottoIds, clock())
        val plan = MontageBuilder.build(state)
        repo.addRecord(record)
        val earned = progress.achievements + state.achievementsThisLife
        progress = progress.copy(
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
        director = null
        stage = null
        script = null
        voice.stop()
        screen = if (settings.cinematic) Screen.Ending(record, plan, state) else Screen.Report(record, fresh = true).also { audio.sfx(Sfx.MOTTO) }
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
            playerName = s.playerName, gender = s.gender, appearance = s.appearance, look = s.look, ageYears = s.ageYears,
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
        if (!new.voiceEnabled) voice.stop()
    }

    fun resetProgress() {
        repo.wipeProgress()
        engine = null
        hud = null
        presented = null
        outcome = null
        director = null
        stage = null
        progress = repo.loadProgress()
        records = repo.loadRecords()
        hasSave = false
        screen = Screen.Menu
    }

    fun release() { audio.release(); voice.release() }
}
