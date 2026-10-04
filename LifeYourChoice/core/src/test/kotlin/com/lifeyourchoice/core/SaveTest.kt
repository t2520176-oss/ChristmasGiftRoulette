package com.lifeyourchoice.core

import com.lifeyourchoice.core.engine.GameEngine
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.Progress
import com.lifeyourchoice.core.model.Settings
import com.lifeyourchoice.core.motto.LifeReportBuilder
import com.lifeyourchoice.core.save.FileSaveRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SaveTest {
    private fun tmp(): File = Files.createTempDirectory("lyc").toFile()

    /** Always picks the first enabled choice so two runs are comparable. */
    private fun step(e: GameEngine) {
        val p = e.present()!!
        e.choose(p.choices.first { it.enabled }.index)
        e.acknowledgeOutcome()
    }

    @Test fun savingAndResumingMidLifeGivesTheSameLife() {
        val straight = GameEngine.newLife(TestSupport.library, "Sam", Gender.GIRL, 1, 4242)
        while (!straight.isFinished) step(straight)

        val repo = FileSaveRepository(tmp())
        val first = GameEngine.newLife(TestSupport.library, "Sam", Gender.GIRL, 1, 4242)
        repeat(20) { step(first) }
        repo.saveGame(first.state)
        assertTrue(repo.hasSavedGame())

        val resumed = GameEngine(repo.loadGame()!!, TestSupport.library)
        while (!resumed.isFinished) step(resumed)

        assertEquals(straight.state.playedOrder, resumed.state.playedOrder)
        assertEquals(straight.state.ageYears, resumed.state.ageYears)
        assertEquals(straight.state.stats, resumed.state.stats)
        assertEquals(straight.state.flags, resumed.state.flags)
        assertEquals(straight.state.milestones.map { it.text }, resumed.state.milestones.map { it.text })
    }

    @Test fun pendingConsequenceSurvivesAnAppRestart() {
        val repo = FileSaveRepository(tmp())
        val e = GameEngine.newLife(TestSupport.library, "Sam", Gender.BOY, 0, 5)
        val p = e.present()!!
        val out = e.choose(p.choices.first().index)
        repo.saveGame(e.state)
        val back = GameEngine(repo.loadGame()!!, TestSupport.library)
        assertNotNull(back.pendingOutcome)
        assertEquals(out.result, back.pendingOutcome!!.result)
        back.acknowledgeOutcome()
        assertNotNull(back.present())
    }

    @Test fun recordsKeepTheMottoAndLessons() {
        val repo = FileSaveRepository(tmp())
        val st = TestSupport.playLife(99, TestSupport.Style.BALANCED).engine.state
        val rec = LifeReportBuilder.build(st, 1, emptyList(), 1_700_000_000_000L)
        repo.addRecord(rec)
        repo.addRecord(LifeReportBuilder.build(st, 2, listOf(rec.mottoId), 1_700_000_100_000L))
        val loaded = repo.loadRecords()
        assertEquals(2, loaded.size)
        assertEquals(rec.mottoText, loaded[0].mottoText)
        assertEquals(rec.lessons, loaded[0].lessons)
        assertEquals(rec.endingTitle, loaded[0].endingTitle)
        assertTrue(loaded[1].mottoId != loaded[0].mottoId)
    }

    @Test fun progressSettingsAndWipe() {
        val dir = tmp()
        val repo = FileSaveRepository(dir)
        assertEquals(0, repo.loadProgress().livesCompleted)
        repo.saveProgress(Progress(setOf("first_paycheck"), 3, listOf("family_1")))
        repo.saveSettings(Settings(soundEffects = false, music = true, volume = 0.3f))
        val fresh = FileSaveRepository(dir)
        assertEquals(setOf("first_paycheck"), fresh.loadProgress().achievements)
        assertEquals(listOf("family_1"), fresh.loadProgress().recentMottoIds)
        assertFalse(fresh.loadSettings().soundEffects)
        fresh.wipeProgress()
        assertEquals(0, fresh.loadProgress().livesCompleted)
        assertTrue(fresh.loadRecords().isEmpty())
        assertFalse(fresh.loadSettings().soundEffects)   // settings are kept
    }

    @Test fun corruptFilesFallBackToDefaultsInsteadOfCrashing() {
        val dir = tmp()
        File(dir, "current_life.json").writeText("{ not json")
        File(dir, "life_records.json").writeText("garbage")
        File(dir, "progress.json").writeText("")
        val repo = FileSaveRepository(dir)
        assertNull(repo.loadGame())
        assertTrue(repo.loadRecords().isEmpty())
        assertEquals(0, repo.loadProgress().livesCompleted)
    }

    /** Files written by Version 1 lack every Version 2A field; they must still load. */
    @Test fun versionOneSavesStillLoad() {
        val dir = tmp()
        val repo = FileSaveRepository(dir)
        val e = GameEngine.newLife(TestSupport.library, "Old", Gender.BOY, 0, 99)
        repeat(6) { step(e) }
        repo.saveGame(e.state)
        val record = LifeReportBuilder.build(e.state, 1, emptyList(), 0L)
        repo.addRecord(record)
        repo.saveProgress(Progress(setOf("first_paycheck"), 1, listOf("x")))
        repo.saveSettings(Settings())

        fun strip(file: String, vararg keys: String) {
            val f = File(dir, file)
            fun clean(el: kotlinx.serialization.json.JsonElement): kotlinx.serialization.json.JsonElement = when (el) {
                is kotlinx.serialization.json.JsonObject -> kotlinx.serialization.json.JsonObject(el.filterKeys { it !in keys }.mapValues { clean(it.value) })
                is kotlinx.serialization.json.JsonArray -> kotlinx.serialization.json.JsonArray(el.map { clean(it) })
                else -> el
            }
            f.writeText(clean(kotlinx.serialization.json.Json.parseToJsonElement(f.readText())).toString())
        }
        strip("current_life.json", "look", "playerLook", "chapterShown", "lastSceneAge", "outcomeIndex", "outcomeText")
        strip("life_records.json", "look")
        strip("progress.json", "viewedScenes")
        strip("settings.json", "voiceEnabled", "voiceVolume", "musicVolume", "sfxVolume", "subtitles", "cinematic")

        val fresh = FileSaveRepository(dir)
        val loaded = fresh.loadGame()
        assertNotNull(loaded)
        assertEquals(e.state.playerName, loaded!!.playerName)
        assertEquals(1, fresh.loadRecords().size)
        assertNull(fresh.loadRecords().first().look)
        assertTrue(fresh.loadProgress().viewedScenes.isEmpty())
        val s = fresh.loadSettings()
        assertTrue(s.subtitles && s.voiceEnabled && s.cinematic)
        // and the old life can be played on
        val resumed = GameEngine(loaded, TestSupport.library)
        while (!resumed.isFinished) step(resumed)
        assertTrue(resumed.isFinished)
    }
}
