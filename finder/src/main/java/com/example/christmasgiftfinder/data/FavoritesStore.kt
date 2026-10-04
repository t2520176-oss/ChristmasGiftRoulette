package com.example.christmasgiftfinder.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Favorite gift ids, observable from Compose. [persist] is called after each change (the Android
 * app saves to SharedPreferences; tests and previews can pass nothing).
 */
class FavoritesStore(
    initial: Set<String> = emptySet(),
    private val persist: (Set<String>) -> Unit = {},
) {
    var ids: Set<String> by mutableStateOf(initial)
        private set

    fun isFavorite(id: String): Boolean = id in ids

    fun toggle(id: String) {
        ids = if (id in ids) ids - id else ids + id
        persist(ids)
    }
}
