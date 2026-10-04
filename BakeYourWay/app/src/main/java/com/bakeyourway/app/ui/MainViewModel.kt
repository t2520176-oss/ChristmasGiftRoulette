package com.bakeyourway.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bakeyourway.app.BakeApp
import com.bakeyourway.core.CatalogLoader
import com.bakeyourway.core.RecipeCatalog
import com.bakeyourway.core.UnitSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CatalogState {
    data object Loading : CatalogState
    data class Ready(val catalog: RecipeCatalog) : CatalogState
    data class Failed(val message: String) : CatalogState
}

data class MainUiState(
    val catalog: CatalogState = CatalogState.Loading,
    val favorites: Set<String> = emptySet(),
    val recents: List<String> = emptyList(),
    val unitSystem: UnitSystem = UnitSystem.US_CUPS,
)

/** Single source of truth for the screens: the offline recipe catalog plus the user's local data. */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = (application as BakeApp).prefs
    private val catalogState = MutableStateFlow<CatalogState>(CatalogState.Loading)

    val uiState: StateFlow<MainUiState> = combine(
        catalogState, prefs.favorites, prefs.recents, prefs.unitSystem,
    ) { catalog, favorites, recents, units ->
        MainUiState(catalog, favorites, recents, units)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val assets = application.assets
            catalogState.value = try {
                CatalogLoader.load { path -> assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() } }
                    .let { CatalogState.Ready(it) }
            } catch (e: Exception) {
                CatalogState.Failed(e.message ?: "The recipe data could not be read.")
            }
        }
    }

    fun toggleFavorite(recipeId: String) {
        viewModelScope.launch { prefs.toggleFavorite(recipeId) }
    }

    fun onRecipeViewed(recipeId: String) {
        viewModelScope.launch { prefs.addRecent(recipeId) }
    }

    fun clearRecents() {
        viewModelScope.launch { prefs.clearRecents() }
    }

    fun setUnitSystem(system: UnitSystem) {
        viewModelScope.launch { prefs.setUnitSystem(system) }
    }
}
