package com.lifeyourchoice.core.save

import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.LifeRecord
import com.lifeyourchoice.core.model.Progress
import com.lifeyourchoice.core.model.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** Everything is stored on the device. No account, no network. */
interface SaveRepository {
    fun loadGame(): GameState?
    fun saveGame(state: GameState)
    fun clearGame()
    fun hasSavedGame(): Boolean

    fun loadRecords(): List<LifeRecord>
    fun addRecord(record: LifeRecord)

    fun loadProgress(): Progress
    fun saveProgress(progress: Progress)

    fun loadSettings(): Settings
    fun saveSettings(settings: Settings)

    /** Deletes the saved life, all records and progress (settings are kept). */
    fun wipeProgress()
}

@Serializable
private class SaveEnvelope(val version: Int, val state: GameState)

/** JSON files in [dir]; every write goes to a temp file first so a crash never corrupts a save. */
class FileSaveRepository(private val dir: File) : SaveRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
    private val gameFile get() = File(dir, "current_life.json")
    private val recordsFile get() = File(dir, "life_records.json")
    private val progressFile get() = File(dir, "progress.json")
    private val settingsFile get() = File(dir, "settings.json")

    init { dir.mkdirs() }

    private fun writeAtomically(target: File, text: String) {
        dir.mkdirs()
        val tmp = File(dir, target.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            target.writeText(text)
            tmp.delete()
        }
    }

    private inline fun <T> readOrNull(file: File, parse: (String) -> T): T? =
        try { if (file.exists()) parse(file.readText()) else null } catch (e: Exception) { null }

    override fun loadGame(): GameState? =
        readOrNull(gameFile) { json.decodeFromString(SaveEnvelope.serializer(), it).state }

    override fun saveGame(state: GameState) =
        writeAtomically(gameFile, json.encodeToString(SaveEnvelope.serializer(), SaveEnvelope(SAVE_VERSION, state)))

    override fun clearGame() { gameFile.delete() }

    override fun hasSavedGame(): Boolean = loadGame() != null

    override fun loadRecords(): List<LifeRecord> =
        readOrNull(recordsFile) { json.decodeFromString(ListSerializer(LifeRecord.serializer()), it) } ?: emptyList()

    override fun addRecord(record: LifeRecord) {
        val all = loadRecords() + record
        writeAtomically(recordsFile, json.encodeToString(ListSerializer(LifeRecord.serializer()), all))
    }

    override fun loadProgress(): Progress =
        readOrNull(progressFile) { json.decodeFromString(Progress.serializer(), it) } ?: Progress()

    override fun saveProgress(progress: Progress) =
        writeAtomically(progressFile, json.encodeToString(Progress.serializer(), progress))

    override fun loadSettings(): Settings =
        readOrNull(settingsFile) { json.decodeFromString(Settings.serializer(), it) } ?: Settings()

    override fun saveSettings(settings: Settings) =
        writeAtomically(settingsFile, json.encodeToString(Settings.serializer(), settings))

    override fun wipeProgress() {
        gameFile.delete(); recordsFile.delete(); progressFile.delete()
    }

    companion object { const val SAVE_VERSION = 1 }
}
