package com.example.christmasgiftroulette

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.christmasgiftroulette.feedback.FeedbackController
import com.example.christmasgiftroulette.navigation.AppNavigation
import com.example.christmasgiftroulette.ui.components.ChristmasBackground
import com.example.christmasgiftroulette.ui.theme.ChristmasTheme
import com.example.christmasgiftroulette.viewmodel.GiftRouletteViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChristmasTheme {
                val viewModel: GiftRouletteViewModel = viewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle()
                val isLoaded by remember { derivedStateOf { uiState.value.isLoaded } }
                val feedback = remember { FeedbackController(applicationContext) }
                DisposableEffect(feedback) { onDispose { feedback.release() } }

                // One persistent festive background behind every screen; content appears once the
                // saved session has been read from DataStore.
                ChristmasBackground {
                    if (isLoaded) AppNavigation(viewModel, feedback)
                }
            }
        }
    }
}
