package com.example.christmasgiftroulette.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** Single DataStore file shared by settings and the saved game. */
val Context.appDataStore by preferencesDataStore(name = "christmas_gift_roulette")
