package com.example.christmasgiftfinder

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.christmasgiftfinder.data.FavoritesStore
import com.example.christmasgiftfinder.data.GiftRepository
import com.example.christmasgiftfinder.navigation.AppNavigator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // All data is bundled with the app: gifts.json in assets, favorites in SharedPreferences.
        val gifts = loadGifts()
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val favorites = FavoritesStore(
            initial = prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toSet(),
            persist = { ids -> prefs.edit().putStringSet(KEY_FAVORITES, ids.toSet()).apply() },
        )

        setContent {
            val nav = rememberSaveable(saver = AppNavigator.Saver) { AppNavigator() }
            BackHandler(enabled = nav.canGoBack) { nav.pop() }
            GiftFinderApp(nav = nav, gifts = remember { gifts }, favorites = favorites)
        }
    }

    private fun loadGifts() = try {
        GiftRepository.parse(assets.open("gifts.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
    } catch (e: Exception) {
        emptyList() // never crash: the app then simply shows "no gift ideas".
    }

    private companion object {
        const val PREFS = "gift_finder"
        const val KEY_FAVORITES = "favorites"
    }
}
