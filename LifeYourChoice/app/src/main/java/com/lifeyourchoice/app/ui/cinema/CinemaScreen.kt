package com.lifeyourchoice.app.ui.cinema

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.lifeyourchoice.app.ui.components.ChoiceButton
import com.lifeyourchoice.app.ui.components.DeltaChip
import com.lifeyourchoice.app.ui.components.HudPill
import com.lifeyourchoice.app.ui.components.LyButton
import com.lifeyourchoice.app.ui.components.StatBar
import com.lifeyourchoice.app.ui.components.appear
import com.lifeyourchoice.app.ui.components.panel
import com.lifeyourchoice.app.ui.components.rememberAppear
import com.lifeyourchoice.app.ui.state.GameSession
import com.lifeyourchoice.app.ui.state.Hud
import com.lifeyourchoice.app.ui.state.Sfx
import com.lifeyourchoice.app.ui.state.VoiceLine
import com.lifeyourchoice.app.ui.state.VoiceProfiles
import com.lifeyourchoice.app.ui.theme.Ly
import com.lifeyourchoice.core.cinema.ActorId
import com.lifeyourchoice.core.cinema.Caption
import com.lifeyourchoice.core.cinema.CineDirector
import com.lifeyourchoice.core.cinema.Phase
import com.lifeyourchoice.core.cinema.TitleKind
import com.lifeyourchoice.core.cinema.TitleState
import com.lifeyourchoice.core.model.Stat

/**
 * The movie: a stage with characters acting out the scene, subtitles (and optional voice),
 * and the player's choices appearing at the natural pause.
 */
@Composable
fun CinemaScreen(session: GameSession) {
    val d = session.director
    val rt = session.stage
    val hud = session.hud
    if (d == null || rt == null || hud == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    // One clock drives the whole movie.
    LaunchedEffect(d) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) session.tickCinema(((now - last) / 1_000_000L).toInt().coerceIn(1, 100))
                last = now
            }
        }
    }

    // Speak each new line (recorded clip or offline TTS); subtitles always work without it.
    val caption = d.caption
    LaunchedEffect(d, caption?.serial) {
        val c = caption ?: return@LaunchedEffect
        if (c.text.isBlank()) return@LaunchedEffect
        val info = rt.cast[c.speaker]
        val (pitch, rate) = VoiceProfiles.forLine(
            c.speaker, info?.gender ?: com.lifeyourchoice.core.model.Gender.BOY, info?.age ?: 30,
            if (c.speaker == ActorId.PLAYER) session.playerVoiceType else 0, c.emotion
        )
        val started = session.speak(VoiceLine(c.text, c.audio, c.speaker, pitch, rate)) { d.voiceFinished() }
        if (started) d.holdForVoice()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val wide = maxWidth > 760.dp && maxWidth > maxHeight
        val choosing = d.phase == Phase.CHOOSING
        val tap = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            if (!choosing) session.tapCinema()
        }
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                StageWithOverlays(session, d, rt, hud, tap, Modifier.weight(1.5f).fillMaxHeight())
                ChoicePanel(session, d, Modifier.weight(1f).fillMaxHeight().background(Ly.Navy900))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                StageWithOverlays(session, d, rt, hud, tap, Modifier.weight(1f).fillMaxWidth())
                if (choosing) ChoicePanel(session, d, Modifier.fillMaxWidth().background(Ly.Navy900))
            }
        }
        ToastOverlay(session)
        if (session.showStats) StatsPanel(session, hud)
    }
}

@Composable
private fun StageWithOverlays(session: GameSession, d: CineDirector, rt: StageRuntime, hud: Hud, tap: Modifier, modifier: Modifier) {
    Box(modifier.then(tap)) {
        StageView(rt, Modifier.fillMaxSize())
        TopBar(session, d, hud)
        val show = session.settings.subtitles || !session.voice.available || !session.settings.voiceEnabled
        d.caption?.let { if (show) DialogueBox(it, d, Modifier.align(Alignment.BottomStart)) }
        d.title?.let { TitleCard(it) }
    }
}

@Composable
private fun TopBar(session: GameSession, d: CineDirector, hud: Hud) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HudPill("Age ${hud.ageYears}  –  ${session.stageTitle}")
        Spacer(Modifier.weight(1f))
        if (session.canSkip) {
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xCC081028))
                    .border(BorderStroke(1.dp, Ly.Gold.copy(alpha = 0.7f)), RoundedCornerShape(10.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { session.skipCinema() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) { Text("SKIP ▸▸", color = Ly.Gold, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp) }
            Spacer(Modifier.width(8.dp))
        }
        HudPill(hud.stat(Stat.MONEY).toString(), glyph = "$", glyphColor = Ly.statColor(Stat.MONEY))
        Spacer(Modifier.width(6.dp))
        HudPill(hud.stat(Stat.ENERGY).toString(), glyph = "⚡", glyphColor = Ly.statColor(Stat.ENERGY))
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(Ly.Navy700).border(BorderStroke(1.5.dp, Ly.Gold), CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    session.audio.sfx(Sfx.TAP); session.showStats = true
                },
            contentAlignment = Alignment.Center
        ) { Text("≡", color = Ly.Gold, fontSize = 20.sp, fontWeight = FontWeight.Black) }
    }
}

/** Speaker name and subtitle, in the style of a film. Narration is italic and nameless. */
@Composable
private fun DialogueBox(c: Caption, d: CineDirector, modifier: Modifier) {
    val a = rememberAppear(0, key = c.serial)
    val narrator = c.speaker == ActorId.NARRATOR
    Box(Modifier.fillMaxWidth().then(modifier).padding(horizontal = 12.dp, vertical = 12.dp).appear(a, 8f)) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color(0xDD060C1E))
                .border(BorderStroke(1.dp, Ly.BlueLine.copy(alpha = 0.6f)), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (!narrator) Text("${c.name}:", color = if (c.speaker == ActorId.PLAYER) Ly.Gold else Ly.BlueSoft, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(
                if (narrator) c.text else "“${c.text}”",
                color = Ly.Text, fontSize = 16.sp, lineHeight = 22.sp,
                fontStyle = if (narrator) FontStyle.Italic else FontStyle.Normal,
                modifier = Modifier.padding(top = if (narrator) 0.dp else 2.dp)
            )
        }
    }
}

@Composable
private fun TitleCard(t: TitleState) {
    val a = rememberAppear(0, key = t.serial)
    val chapter = t.kind == TitleKind.CHAPTER
    Box(
        Modifier.fillMaxSize().background(if (chapter) Color.Black else Color(0x99000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.appear(a, 14f).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (t.kind) {
                TitleKind.CHAPTER -> {
                    Text(t.text, color = Ly.Gold, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
                    t.sub?.split("\n")?.forEachIndexed { i, line ->
                        Spacer(Modifier.height(if (i == 0) 10.dp else 6.dp))
                        Text(line, color = if (i == 0) Ly.TextDim else Color.White, fontSize = if (i == 0) 15.sp else 28.sp,
                            fontWeight = if (i == 0) FontWeight.Medium else FontWeight.Black, letterSpacing = if (i == 0) 4.sp else 3.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.width(60.dp).height(2.dp).background(Ly.Gold.copy(alpha = 0.7f)))
                }
                TitleKind.CAPTION -> {
                    Text(t.text, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, textAlign = TextAlign.Center)
                    t.sub?.let { Text(it, color = Ly.TextDim, fontSize = 13.sp, letterSpacing = 3.sp, modifier = Modifier.padding(top = 6.dp)) }
                }
                TitleKind.CARD -> {
                    Text(t.text, color = Ly.Bad, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, textAlign = TextAlign.Center)
                    t.sub?.let { Text(it, color = Ly.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
        }
    }
}

/** The choices, each phrased as what the player says (or does). */
@Composable
private fun ChoicePanel(session: GameSession, d: CineDirector, modifier: Modifier) {
    val p = session.presented
    Column(
        modifier.navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (p == null || d.phase != Phase.CHOOSING) {
            Spacer(Modifier.height(10.dp))
            return@Column
        }
        if (p.fromPast) Text("◆ A CHOICE FROM YOUR PAST", color = Ly.Gold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        p.choices.forEachIndexed { i, c ->
            val a = rememberAppear(80 + i * 100, key = p.id)
            // The scene's own line for this choice (what the player says), else the story text.
            val said = d.script.choices[c.index]?.say?.text
            val label = said?.let { com.lifeyourchoice.core.engine.TextTemplate.render(it, session.stateForText()) } ?: c.text
            ChoiceButton(
                letter = 'A' + i, text = label, enabled = c.enabled, lockedHint = c.lockedHint, badge = c.badge,
                onClick = { session.chooseCinematic(c.index) },
                modifier = Modifier.fillMaxWidth().appear(a, 16f)
            )
        }
    }
}

/** Important changes shown briefly, then faded. */
@Composable
private fun ToastOverlay(session: GameSession) {
    val t = session.toast ?: return
    LaunchedEffect(t.serial) {
        kotlinx.coroutines.delay(3200)
        session.clearToast(t.serial)
    }
    val a = rememberAppear(0, key = t.serial)
    Column(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 54.dp, start = 16.dp, end = 16.dp).appear(a, -14f),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        t.deltas.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { DeltaChip(it.stat, it.delta) } }
        }
        if (t.hint) Text("Your decision may matter someday.", color = Ly.Gold, fontSize = 13.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold)
        t.achievements.forEach { title ->
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xDD2A2008))
                    .border(BorderStroke(1.dp, Ly.Gold.copy(alpha = 0.8f)), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏆", fontSize = 16.sp); Spacer(Modifier.width(8.dp))
                Column {
                    Text("ACHIEVEMENT UNLOCKED", color = Ly.Gold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text(title, color = Ly.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Stats and recent events, opened from the avatar button. */
@Composable
private fun StatsPanel(session: GameSession, hud: Hud) {
    val a = rememberAppear(0)
    Box(
        Modifier.fillMaxSize().background(Color(0xDD030610))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { session.showStats = false },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.statusBarsPadding().navigationBarsPadding().padding(14.dp).widthIn(max = 480.dp).fillMaxWidth()
                .appear(a, 30f).panel(18, 0.98f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Ly.Navy700).border(BorderStroke(2.dp, Ly.Gold), CircleShape)) {
                    CharacterPortrait(hud.gender, hud.appearance, hud.ageYears, Modifier.fillMaxSize().padding(top = 5.dp), happiness = hud.stat(Stat.HAPPINESS), look = hud.look)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(hud.playerName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Age ${hud.ageYears}  |  ${hud.chapter}", color = Ly.TextDim, fontSize = 12.sp)
                    Text(hud.jobTitle, color = Ly.BlueSoft, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Stat.panel.forEach { StatBar(it, hud.stat(it)) }
            if (hud.stat(Stat.CAREER) > 0) StatBar(Stat.CAREER, hud.stat(Stat.CAREER))
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFFEFE6CF)).padding(14.dp)) {
                Text("Recent Events:", color = Color(0xFF2B2113), fontSize = 14.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(4.dp))
                val recent = hud.recentEvents.takeLast(4).reversed()
                if (recent.isEmpty()) Text("Your story is just beginning.", color = Color(0xFF4B3D28), fontSize = 13.sp, fontStyle = FontStyle.Italic)
                else recent.forEach { Text("•  $it", color = Color(0xFF3B2F1C), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 2.dp)) }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LyButton("MAIN MENU", { session.goMenu() }, Modifier.weight(1f), com.lifeyourchoice.app.ui.components.ButtonKind.GHOST, fontSize = 13)
                LyButton("RESUME", { session.showStats = false }, Modifier.weight(1f), com.lifeyourchoice.app.ui.components.ButtonKind.PRIMARY, fontSize = 14)
            }
            Text("Your progress is saved automatically.", color = Ly.TextDim, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}
