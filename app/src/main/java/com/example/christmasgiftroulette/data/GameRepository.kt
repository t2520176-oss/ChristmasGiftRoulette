package com.example.christmasgiftroulette.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

class GameRepository(private val dataStore: DataStore<Preferences>) {

    suspend fun load(): PersistedSession? =
        SessionSerializer.decode(dataStore.data.first()[SESSION])

    suspend fun save(session: PersistedSession) {
        dataStore.edit { it[SESSION] = SessionSerializer.encode(session) }
    }

    private companion object {
        val SESSION = stringPreferencesKey("session_json")
    }
}
