package com.example.christmasgiftfinder.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver

enum class Route { Welcome, Finder, Country, Recipient, Style, Results, Surprise, Favorites }

/** A tiny back stack. Screens are plain composables, so no navigation library is needed. */
class AppNavigator(initial: List<Route> = listOf(Route.Welcome)) {
    val stack = mutableStateListOf<Route>().apply { addAll(initial.ifEmpty { listOf(Route.Welcome) }) }

    val current: Route get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun push(route: Route) {
        stack.add(route)
    }

    /** Replaces the whole stack, e.g. leaving the welcome screen for good. */
    fun replaceAll(route: Route) {
        stack.clear()
        stack.add(route)
    }

    fun pop(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    companion object {
        val Saver = listSaver<AppNavigator, String>(
            save = { nav -> nav.stack.map { it.name } },
            restore = { names -> AppNavigator(names.mapNotNull { n -> Route.entries.firstOrNull { it.name == n } }) },
        )
    }
}
