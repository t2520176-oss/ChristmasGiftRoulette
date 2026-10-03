package com.example.christmasgiftroulette.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val confirmBeforeReset: Boolean = true,
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            soundEnabled = prefs[SOUND] ?: true,
            vibrationEnabled = prefs[VIBRATION] ?: true,
            confirmBeforeReset = prefs[CONFIRM_RESET] ?: true,
        )
    }

    suspend fun setSoundEnabled(enabled: Boolean) = dataStore.edit { it[SOUND] = enabled }
    suspend fun setVibrationEnabled(enabled: Boolean) = dataStore.edit { it[VIBRATION] = enabled }
    suspend fun setConfirmBeforeReset(enabled: Boolean) = dataStore.edit { it[CONFIRM_RESET] = enabled }

    private companion object {
        val SOUND = booleanPreferencesKey("settings_sound")
        val VIBRATION = booleanPreferencesKey("settings_vibration")
        val CONFIRM_RESET = booleanPreferencesKey("settings_confirm_reset")
    }
}
