package com.bakeyourway.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bakeyourway.core.UnitSystem
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.bakeDataStore: DataStore<Preferences> by preferencesDataStore(name = "bake_your_way")

/**
 * Favorites, recently viewed recipes and the preferred measuring system, all stored on the device
 * with DataStore. No account, no cloud.
 */
class UserPrefsRepository(private val context: Context) {
    private object Keys {
        val FAVORITES = stringSetPreferencesKey("favorites")
        val RECENTS = stringPreferencesKey("recents")
        val UNITS = stringPreferencesKey("units")
    }

    private val data: Flow<Preferences> = context.bakeDataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val favorites: Flow<Set<String>> = data.map { it[Keys.FAVORITES] ?: emptySet() }

    val recents: Flow<List<String>> = data.map { prefs ->
        prefs[Keys.RECENTS].orEmpty().split(',').filter { it.isNotBlank() }
    }

    val unitSystem: Flow<UnitSystem> = data.map { prefs ->
        runCatching { UnitSystem.valueOf(prefs[Keys.UNITS].orEmpty()) }.getOrDefault(UnitSystem.US_CUPS)
    }

    suspend fun toggleFavorite(recipeId: String) {
        context.bakeDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES] ?: emptySet()
            prefs[Keys.FAVORITES] = if (recipeId in current) current - recipeId else current + recipeId
        }
    }

    suspend fun addRecent(recipeId: String) {
        context.bakeDataStore.edit { prefs ->
            val current = prefs[Keys.RECENTS].orEmpty().split(',').filter { it.isNotBlank() }
            val updated = (listOf(recipeId) + current.filter { it != recipeId }).take(MAX_RECENTS)
            prefs[Keys.RECENTS] = updated.joinToString(",")
        }
    }

    suspend fun clearRecents() {
        context.bakeDataStore.edit { it.remove(Keys.RECENTS) }
    }

    suspend fun setUnitSystem(system: UnitSystem) {
        context.bakeDataStore.edit { it[Keys.UNITS] = system.name }
    }

    private companion object {
        const val MAX_RECENTS = 10
    }
}
