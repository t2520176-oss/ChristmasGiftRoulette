package com.example.christmasgiftfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftfinder.R
import com.example.christmasgiftfinder.ui.components.BigButton
import com.example.christmasgiftfinder.ui.components.ContentMaxWidth
import com.example.christmasgiftfinder.ui.components.Snowfall
import com.example.christmasgiftfinder.ui.components.WelcomeScene
import com.example.christmasgiftfinder.ui.theme.DeepRed
import com.example.christmasgiftfinder.ui.theme.ChristmasRed
import com.example.christmasgiftfinder.ui.theme.Gold
import com.example.christmasgiftfinder.ui.theme.Ink
import com.example.christmasgiftfinder.ui.theme.Snow

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ChristmasRed, DeepRed))),
    ) {
        Snowfall(Modifier.fillMaxSize(), count = 40, seed = 11)
        // Scene height follows the available height so nothing is clipped; the column scrolls if it must.
        val sceneHeight = (maxHeight * 0.38f).coerceIn(170.dp, 340.dp)
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🎄", fontSize = 44.sp)
                Text(
                    stringResource(R.string.welcome_title),
                    color = Snow,
                    fontSize = 40.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                WelcomeScene(Modifier.fillMaxWidth().height(sceneHeight))
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.welcome_subtitle),
                    color = Snow,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                BigButton(
                    text = stringResource(R.string.get_started),
                    onClick = onGetStarted,
                    containerColor = Gold,
                    contentColor = Ink,
                )
            }
        }
    }
}
