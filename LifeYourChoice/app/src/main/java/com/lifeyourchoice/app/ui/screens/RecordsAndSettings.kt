package com.lifeyourchoice.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeyourchoice.app.ui.art.CharacterPortrait
import com.lifeyourchoice.app.ui.components.ButtonKind
import com.lifeyourchoice.app.ui.components.LyButton
import com.lifeyourchoice.app.ui.components.appear
import com.lifeyourchoice.app.ui.components.panel
import com.lifeyourchoice.app.ui.components.rememberAppear
import com.lifeyourchoice.app.ui.state.GameSession
import com.lifeyourchoice.app.ui.theme.Ly
import com.lifeyourchoice.core.engine.Achievements
import com.lifeyourchoice.core.model.LifeRecord
import com.lifeyourchoice.core.model.Settings

@Composable
fun RecordsScreen(session: GameSession) {
    var tab by remember { mutableStateOf(0) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ly.Navy900, Ly.Navy950)))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LIFE RECORDS", color = Ly.Gold, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, modifier = Modifier.padding(top = 16.dp, bottom = 10.dp))
            Row(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabButton("LIVES (${session.records.size})", tab == 0, Modifier.weight(1f)) { tab = 0 }
                TabButton("ACHIEVEMENTS (${session.progress.achievements.size}/${Achievements.all.size})", tab == 1, Modifier.weight(1f)) { tab = 1 }
            }
            Box(Modifier.weight(1f).widthIn(max = 560.dp).fillMaxWidth()) {
                if (tab == 0) RecordList(session) else AchievementList(session)
            }
            LyButton("BACK", { session.goMenu() }, Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(16.dp), ButtonKind.GHOST, fontSize = 14)
        }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp))
            .background(if (selected) Ly.Blue.copy(alpha = 0.28f) else Ly.Navy800)
            .border(BorderStroke(1.2.dp, if (selected) Ly.Blue else Ly.BlueLine.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (selected) Color.White else Ly.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, textAlign = TextAlign.Center) }
}

@Composable
private fun RecordList(session: GameSession) {
    val records = session.records.sortedByDescending { it.number }
    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No lives completed yet.\nFinish a life and it will be remembered here, with its motto.",
                color = Ly.TextDim, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(records) { r -> RecordCard(r) { session.openReport(r) } }
    }
}

/** One completed life: who, how it ended, and the motto it left behind. */
@Composable
private fun RecordCard(r: LifeRecord, onClick: () -> Unit) {
    val a = rememberAppear(0, key = r.number)
    Row(
        Modifier.fillMaxWidth().appear(a, 14f).panel(16)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(14.dp)
    ) {
        Box(Modifier.size(62.dp).clip(CircleShape).background(Ly.Navy700).border(BorderStroke(2.dp, Ly.Gold.copy(alpha = 0.8f)), CircleShape)) {
            CharacterPortrait(r.gender, r.appearance, r.ageReached, Modifier.fillMaxSize().padding(top = 5.dp), happiness = r.happiness, look = r.look)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("LIFE #${r.number}", color = Ly.Gold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(r.playerName, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text("Age ${r.ageReached}  •  ${r.careerTitle}", color = Ly.Text, fontSize = 13.sp)
            Text("Ending: ${r.endingTitle.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { c -> c.uppercase() } }}", color = Ly.BlueSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Life’s Motto:", color = Ly.TextDim, fontSize = 11.sp, letterSpacing = 1.sp)
            Text("“${r.mottoText}”", color = Ly.Text, fontSize = 14.sp, fontStyle = FontStyle.Italic, lineHeight = 20.sp)
            Text("Family ${r.familyLabel}  •  Wealth ${r.wealth}  •  Happiness ${r.happiness}", color = Ly.TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun AchievementList(session: GameSession) {
    val unlocked = session.progress.achievements
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(Achievements.all) { ach ->
            val got = ach.id in unlocked
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (got) Ly.Gold.copy(alpha = 0.12f) else Ly.Navy800)
                    .border(BorderStroke(1.2.dp, if (got) Ly.Gold.copy(alpha = 0.8f) else Ly.BlueLine.copy(alpha = 0.35f)), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (got) "🏆" else "🔒", fontSize = 24.sp, modifier = Modifier.width(40.dp))
                Column(Modifier.weight(1f)) {
                    Text(ach.title, color = if (got) Ly.Gold else Ly.TextDim, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text(ach.description, color = if (got) Ly.Text else Ly.TextDim.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(session: GameSession) {
    val s = session.settings
    var confirmReset by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ly.Navy900, Ly.Navy950)))) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SETTINGS", color = Ly.Gold, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, modifier = Modifier.padding(vertical = 12.dp))
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().panel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("STORY")
                SettingRow("Cinematic mode (animated scenes)", s.cinematic) { session.updateSettings(s.copy(cinematic = it)) }
                SettingRow("Subtitles", s.subtitles) { session.updateSettings(s.copy(subtitles = it)) }
                if (!s.voiceEnabled || !session.voiceAvailable) {
                    Text("Subtitles are always shown when no voice is available.", color = Ly.TextDim, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().panel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("VOICE")
                SettingRow("Spoken dialogue", s.voiceEnabled) { session.updateSettings(s.copy(voiceEnabled = it)) }
                VolumeRow("Voice volume", s.voiceVolume, s.voiceEnabled) { session.updateSettings(s.copy(voiceVolume = it)) }
                Text(
                    if (session.voiceAvailable) "Voices are produced on this device and work offline."
                    else "No offline voice was found on this device. The story plays with subtitles only.",
                    color = Ly.TextDim, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().panel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("SOUND")
                SettingRow("Sound effects", s.soundEffects) { session.updateSettings(s.copy(soundEffects = it)) }
                VolumeRow("Effects volume", s.sfxVolume, s.soundEffects) { session.updateSettings(s.copy(sfxVolume = it)) }
                SettingRow("Background music", s.music) { session.updateSettings(s.copy(music = it)) }
                VolumeRow("Music volume", s.musicVolume, s.music) { session.updateSettings(s.copy(musicVolume = it)) }
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().panel().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ABOUT", color = Ly.Gold, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("LIFE: YOUR CHOICE  ·  Version 2A · The Cinematic Life", color = Ly.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Works completely offline. Every scene is performed by a built-in animation engine; there is no AI service, no cloud voice, no account, no ads and no purchases. Your lives, records and achievements are stored only on this device.",
                    color = Ly.TextDim, fontSize = 13.sp, lineHeight = 19.sp)
                Text("“Every decision builds the life you will live.”", color = Ly.TextDim, fontSize = 13.sp, fontStyle = FontStyle.Italic)
            }
            Spacer(Modifier.height(16.dp))
            LyButton("RESET ALL PROGRESS", { confirmReset = true }, Modifier.widthIn(max = 520.dp).fillMaxWidth(), ButtonKind.GHOST, fontSize = 13)
            Spacer(Modifier.height(10.dp))
            LyButton("BACK", { session.goMenu() }, Modifier.widthIn(max = 520.dp).fillMaxWidth(), ButtonKind.PRIMARY, fontSize = 16)
        }
        if (confirmReset) {
            AlertDialog(
                onDismissRequest = { confirmReset = false },
                containerColor = Ly.Navy800,
                title = { Text("Reset all progress?", color = Ly.Text) },
                text = { Text("This deletes your current life, all Life Records and achievements. This cannot be undone.", color = Ly.TextDim) },
                confirmButton = { TextButton(onClick = { confirmReset = false; session.resetProgress() }) { Text("RESET", color = Ly.Bad, fontWeight = FontWeight.Black) } },
                dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("CANCEL", color = Ly.Text) } }
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ly.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Ly.Gold, checkedTrackColor = Ly.Blue, uncheckedTrackColor = Ly.Navy600, uncheckedThumbColor = Ly.TextDim)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Ly.Gold, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
}

@Composable
private fun VolumeRow(label: String, value: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (enabled) Ly.Text else Ly.TextDim, fontSize = 14.sp, modifier = Modifier.width(120.dp))
        Slider(
            value = value, onValueChange = onChange, enabled = enabled, modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = Ly.Gold, activeTrackColor = Ly.Blue, inactiveTrackColor = Ly.Navy600)
        )
    }
}
