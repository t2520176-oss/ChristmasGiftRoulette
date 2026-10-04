package com.lifeyourchoice.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lifeyourchoice.app.ui.screens.CharacterCreationScreen
import com.lifeyourchoice.app.ui.screens.GameScreen
import com.lifeyourchoice.app.ui.screens.IntroScreen
import com.lifeyourchoice.app.ui.screens.LifeCardScreen
import com.lifeyourchoice.app.ui.screens.MainMenuScreen
import com.lifeyourchoice.app.ui.screens.RecordsScreen
import com.lifeyourchoice.app.ui.screens.ReportScreen
import com.lifeyourchoice.app.ui.screens.SettingsScreen
import com.lifeyourchoice.app.ui.state.GameSession
import com.lifeyourchoice.app.ui.state.Screen
import com.lifeyourchoice.app.ui.theme.LifeTheme
import com.lifeyourchoice.app.ui.theme.Ly

/** Root of the shared UI. Platform code (MainActivity) only creates the [GameSession] and handles Back. */
@Composable
fun LifeApp(session: GameSession) {
    LifeTheme {
        Box(Modifier.fillMaxSize().background(Ly.Navy950)) {
            Crossfade(targetState = session.screen, animationSpec = tween(320), label = "screen") { screen ->
                when (screen) {
                    Screen.Menu -> MainMenuScreen(session)
                    Screen.Create -> CharacterCreationScreen(session)
                    Screen.Intro -> IntroScreen(session)
                    Screen.Playing -> GameScreen(session)
                    Screen.Records -> RecordsScreen(session)
                    Screen.Settings -> SettingsScreen(session)
                    is Screen.Report -> ReportScreen(session, screen.record, screen.fresh)
                    is Screen.Card -> LifeCardScreen(session, screen.record)
                }
            }
        }
    }
}
