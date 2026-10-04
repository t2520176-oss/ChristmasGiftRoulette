package com.lifeyourchoice.app.platform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.lifeyourchoice.app.ui.LifeApp
import com.lifeyourchoice.app.ui.state.Screen

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val session = viewModel.session
            // At the main menu, Back closes the app; everywhere else it steps back inside the game.
            BackHandler(enabled = session.screen !is Screen.Menu || session.showStats) {
                session.back()
            }
            LifeApp(session)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.audio.setForeground(true)
    }

    override fun onPause() {
        viewModel.audio.setForeground(false)
        super.onPause()
    }
}
