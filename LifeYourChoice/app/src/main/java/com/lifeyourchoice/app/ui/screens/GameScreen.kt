package com.lifeyourchoice.app.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeyourchoice.app.ui.art.CharacterPortrait
import com.lifeyourchoice.app.ui.art.SceneCanvas
import com.lifeyourchoice.app.ui.components.ButtonKind
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
import com.lifeyourchoice.app.ui.theme.Ly
import com.lifeyourchoice.core.engine.Achievements
import com.lifeyourchoice.core.engine.PresentedScenario
import com.lifeyourchoice.core.model.ChoiceOutcome
import com.lifeyourchoice.core.model.SceneArt
import com.lifeyourchoice.core.model.Stat

@Composable
fun GameScreen(session: GameSession) {
    val hud = session.hud ?: return
    val scenario = session.presented
    val outcome = session.outcome

    Box(Modifier.fillMaxSize().background(Ly.Navy950)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > 720.dp && maxWidth > maxHeight
            val stageH = stageHeight(maxHeight)
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    Stage(session, hud, Modifier.weight(1.1f).fillMaxHeight())
                    StoryPanel(session, scenario, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Stage(session, hud, Modifier.fillMaxWidth().height(stageH))
                    StoryPanel(session, scenario, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
        if (outcome != null) OutcomeOverlay(session, outcome)
        if (session.showStats) StatsOverlay(session, hud)
    }
}

private fun stageHeight(total: Dp): Dp = (total * 0.37f).coerceIn(190.dp, 360.dp)

/** Scene illustration with the hero in the foreground and the HUD (age, title, money, energy) on top. */
@Composable
private fun Stage(session: GameSession, hud: Hud, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp))) {
        SceneCanvas(session.stageArt, Modifier.fillMaxSize())
        // Hero in the foreground, lit from the scene.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val h = maxHeight * 0.78f
            CharacterPortrait(
                hud.gender, hud.appearance, hud.ageYears,
                Modifier.align(Alignment.BottomStart).padding(start = 6.dp).height(h).aspectRatio(5f / 6f),
                happiness = hud.stat(Stat.HAPPINESS), look = hud.look
            )
        }
        // Fade the art into the story panel.
        Box(
            Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Ly.Navy900)))
        )
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudPill("Age ${hud.ageYears}  |  ${session.stageTitle}")
            Spacer(Modifier.weight(1f))
            HudPill(hud.stat(Stat.MONEY).toString(), glyph = "$", glyphColor = Ly.statColor(Stat.MONEY))
            Spacer(Modifier.width(6.dp))
            HudPill(hud.stat(Stat.ENERGY).toString(), glyph = "⚡", glyphColor = Ly.statColor(Stat.ENERGY))
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Ly.Navy700)
                    .border(BorderStroke(1.5.dp, Ly.Gold), CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        session.audio.sfx(Sfx.TAP); session.showStats = true
                    },
                contentAlignment = Alignment.Center
            ) { Text("≡", color = Ly.Gold, fontSize = 20.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun StoryPanel(session: GameSession, scenario: PresentedScenario?, modifier: Modifier) {
    Column(
        modifier.background(Ly.Navy900).navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (scenario == null) {
            Spacer(Modifier.height(80.dp))
            return@Column
        }
        val textA = rememberAppear(80, key = scenario.id)
        Column(Modifier.fillMaxWidth().panel().padding(16.dp).appear(textA, 14f)) {
            if (scenario.fromPast) {
                Text("◆ A CHOICE FROM YOUR PAST", color = Ly.Gold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
            }
            val paragraphs = scenario.text.split("\n\n")
            paragraphs.forEachIndexed { i, para ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                Text(para, color = Ly.Text, fontSize = 16.sp, lineHeight = 23.sp)
            }
        }
        scenario.choices.forEachIndexed { i, c ->
            val a = rememberAppear(380 + i * 110, key = scenario.id)
            ChoiceButton(
                letter = 'A' + i, text = c.text, enabled = c.enabled, lockedHint = c.lockedHint, badge = c.badge,
                onClick = { session.choose(c.index) },
                modifier = Modifier.fillMaxWidth().appear(a, 18f)
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

/** The "YOU CHOSE" screen: what the player did, what changed, and a hint that more may follow. */
@Composable
private fun OutcomeOverlay(session: GameSession, outcome: ChoiceOutcome) {
    val a = rememberAppear(0, key = outcome)
    Box(
        Modifier.fillMaxSize().background(Color(0xCC030610))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxHeight)
                    .appear(a, 60f).panel(18, 0.98f)
                    .verticalScroll(rememberScrollState()).padding(18.dp)
            ) {
                Text("YOU CHOSE:", color = Ly.Gold, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Spacer(Modifier.height(6.dp))
                Text("“${outcome.choiceText}”", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, lineHeight = 23.sp)
                Spacer(Modifier.height(12.dp))
                outcome.result.split("\n\n").forEachIndexed { i, para ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    Text(para, color = Ly.Text, fontSize = 15.sp, lineHeight = 22.sp)
                }
                val deltas = outcome.deltas
                if (deltas.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    deltas.chunked(2).forEachIndexed { row, pair ->
                        val ra = rememberAppear(250 + row * 120, key = outcome)
                        Row(Modifier.fillMaxWidth().padding(bottom = 7.dp).appear(ra, 10f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { DeltaChip(it.stat, it.delta) }
                        }
                    }
                }
                if (outcome.notes.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    outcome.notes.forEach { Text("⏳ $it", color = Ly.TextDim, fontSize = 13.sp, fontStyle = FontStyle.Italic, lineHeight = 18.sp) }
                }
                if (outcome.hint) {
                    Spacer(Modifier.height(12.dp))
                    Text("Your decision may have consequences later…", color = Ly.Gold, fontSize = 13.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold)
                }
                if (session.achievementBanner.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    session.achievementBanner.forEach { title ->
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(10.dp)).background(Ly.Gold.copy(alpha = 0.14f))
                                .border(BorderStroke(1.dp, Ly.Gold.copy(alpha = 0.7f)), RoundedCornerShape(10.dp)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏆", fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("ACHIEVEMENT UNLOCKED", color = Ly.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                Text(title, color = Ly.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                LyButton("CONTINUE", { session.acknowledgeOutcome() }, Modifier.fillMaxWidth(), ButtonKind.PRIMARY, fontSize = 17)
            }
        }
    }
}

/** Stats and recent events, opened from the avatar button. */
@Composable
private fun StatsOverlay(session: GameSession, hud: Hud) {
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
                HudPill(hud.stat(Stat.MONEY).toString(), glyph = "$", glyphColor = Ly.statColor(Stat.MONEY))
                Spacer(Modifier.width(6.dp))
                HudPill(hud.stat(Stat.ENERGY).toString(), glyph = "⚡", glyphColor = Ly.statColor(Stat.ENERGY))
            }
            Spacer(Modifier.height(10.dp))
            Stat.panel.forEach { StatBar(it, hud.stat(it)) }
            if (hud.stat(Stat.CAREER) > 0) StatBar(Stat.CAREER, hud.stat(Stat.CAREER))

            // Notebook of recent decisions.
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFFEFE6CF)).padding(14.dp)
            ) {
                Text("Recent Events:", color = Color(0xFF2B2113), fontSize = 14.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(4.dp))
                val recent = hud.recentEvents.takeLast(4).reversed()
                if (recent.isEmpty()) {
                    Text("Your story is just beginning.", color = Color(0xFF4B3D28), fontSize = 13.sp, fontStyle = FontStyle.Italic)
                } else {
                    recent.forEach { Text("•  $it", color = Color(0xFF3B2F1C), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LyButton("MAIN MENU", { session.goMenu() }, Modifier.weight(1f), ButtonKind.GHOST, fontSize = 13)
                LyButton("RESUME", { session.showStats = false }, Modifier.weight(1f), ButtonKind.PRIMARY, fontSize = 14)
            }
            Text("Your progress is saved automatically.", color = Ly.TextDim, fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}
