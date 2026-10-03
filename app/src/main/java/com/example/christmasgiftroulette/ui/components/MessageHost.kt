package com.example.christmasgiftroulette.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.christmasgiftroulette.viewmodel.UserMessage

/** Shows one-shot ViewModel messages (validation hints etc.) in a snackbar, then consumes them. */
@Composable
fun rememberMessageSnackbar(message: UserMessage?, onConsumed: (Long) -> Unit): SnackbarHostState {
    val host = remember { SnackbarHostState() }
    LaunchedEffect(message?.id) {
        val current = message ?: return@LaunchedEffect
        host.showSnackbar(current.text)
        onConsumed(current.id)
    }
    return host
}
