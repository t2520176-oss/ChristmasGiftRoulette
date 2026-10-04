package com.bakeyourway.app

import android.app.Application
import com.bakeyourway.app.data.UserPrefsRepository

/** Holds the few app-wide singletons. Everything runs locally; nothing here touches the network. */
class BakeApp : Application() {
    lateinit var prefs: UserPrefsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = UserPrefsRepository(this)
    }
}
