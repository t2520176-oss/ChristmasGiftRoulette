package com.lifeyourchoice.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeyourchoice.app.ui.art.CharacterPortrait
import com.lifeyourchoice.app.ui.art.SceneCanvas
import com.lifeyourchoice.app.ui.components.ButtonKind
import com.lifeyourchoice.app.ui.components.LyButton
import com.lifeyourchoice.app.ui.components.SectionTitle
import com.lifeyourchoice.app.ui.components.appear
import com.lifeyourchoice.app.ui.components.panel
import com.lifeyourchoice.app.ui.components.rememberAppear
import com.lifeyourchoice.app.ui.state.GameSession
import com.lifeyourchoice.app.ui.theme.Ly
import com.lifeyourchoice.core.model.LifeCardModel
import com.lifeyourchoice.core.engine.Achievements
import com.lifeyourchoice.core.model.LifeRecord
import com.lifeyourchoice.core.model.SceneArt
import com.lifeyourchoice.core.model.Stat

/**
 * YOUR LIFE REPORT, in this order:
 * report card → YOUR STORY → IMPORTANT CHOICES → WHAT YOU LEARNED → YOUR LIFE'S MOTTO → buttons.
 */
@Composable
fun ReportScreen(session: GameSession, record: LifeRecord, fresh: Boolean) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ly.Navy900, Ly.Navy950)))) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
        ) {
            item { ReportHeader(record) }
            item { Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp)) { StatsTable(record) } }
            item {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp)) {
                    SectionTitle("YOUR STORY")
                    Box(Modifier.fillMaxWidth().panel().padding(16.dp)) {
                        Text("“${record.summary}”", color = Ly.Text, fontSize = 15.sp, lineHeight = 23.sp, fontStyle = FontStyle.Italic)
                    }
                }
            }
            item {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp)) {
                    SectionTitle("IMPORTANT CHOICES")
                    Column(Modifier.fillMaxWidth().panel().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (record.importantChoices.isEmpty()) {
                            Text("A quiet life with no single turning point.", color = Ly.TextDim, fontSize = 14.sp, fontStyle = FontStyle.Italic)
                        }
                        record.importantChoices.forEach { m ->
                            Row {
                                Text("Age ${m.ageYears}", color = Ly.Gold, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(62.dp))
                                Text("—  ${m.text}", color = Ly.Text, fontSize = 14.sp, lineHeight = 19.sp)
                            }
                        }
                    }
                }
            }
            item {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp)) {
                    SectionTitle("WHAT YOU LEARNED")
                    Column(Modifier.fillMaxWidth().panel().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        record.lessons.forEachIndexed { i, lesson ->
                            val a = rememberAppear(150 + i * 350, key = lesson)
                            Row(Modifier.appear(a, 12f), verticalAlignment = Alignment.Top) {
                                Text("◆", color = Ly.Gold, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp, end = 10.dp))
                                Text(lesson, color = Ly.Text, fontSize = 15.sp, lineHeight = 21.sp)
                            }
                        }
                    }
                }
            }
            item {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Spacer(Modifier.height(14.dp))
                    MottoCard(record)
                }
            }
            item {
                Column(
                    Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (fresh) {
                        LyButton("SAVE LIFE", { session.openCard(record) }, Modifier.fillMaxWidth())
                        LyButton("PLAY ANOTHER LIFE", { session.openCreate() }, Modifier.fillMaxWidth(), ButtonKind.PRIMARY, fontSize = 18)
                        LyButton("MAIN MENU", { session.goMenu() }, Modifier.fillMaxWidth(), ButtonKind.GHOST, fontSize = 13)
                    } else {
                        LyButton("VIEW LIFE CARD", { session.openCard(record) }, Modifier.fillMaxWidth())
                        LyButton("BACK TO RECORDS", { session.openRecords() }, Modifier.fillMaxWidth(), ButtonKind.GHOST, fontSize = 13)
                    }
                    if (fresh) {
                        Text("This life is saved in LIFE RECORDS.", color = Ly.TextDim, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportHeader(record: LifeRecord) {
    val a = rememberAppear(0)
    Box(Modifier.fillMaxWidth().height(250.dp)) {
        SceneCanvas(SceneArt.SKYLINE, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66040913), Color.Transparent, Ly.Navy900))))
        Column(Modifier.fillMaxSize().padding(top = 14.dp).appear(a, 12f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("YOUR LIFE REPORT", color = Ly.Gold, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            CharacterPortrait(record.gender, record.appearance, record.ageReached, Modifier.height(150.dp).aspectRatio(5f / 6f), happiness = record.happiness, look = record.look)
            Spacer(Modifier.weight(1f))
            // Ending ribbon.
            Box(
                Modifier.fillMaxWidth(0.88f).widthIn(max = 440.dp).clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(listOf(Ly.GoldDeep, Ly.Gold, Ly.GoldDeep)))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(record.endingTitle, color = Color(0xFF1A1200), fontSize = 19.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, textAlign = TextAlign.Center)
            }
            Text(record.endingTagline, color = Ly.TextDim, fontSize = 12.sp, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 16.dp, end = 16.dp))
        }
    }
}

@Composable
private fun StatsTable(record: LifeRecord) {
    Column(Modifier.fillMaxWidth().panel().padding(16.dp)) {
        InfoRow("Name", record.playerName)
        InfoRow("Final Age", record.ageReached.toString())
        InfoRow("Career", record.careerTitle)
        Spacer(Modifier.height(6.dp))
        ValueRow("Wealth", record.wealth, Stat.MONEY)
        ValueRow("Family", record.family, Stat.FAMILY)
        ValueRow("Health", record.health, Stat.HEALTH)
        ValueRow("Reputation", record.reputation, Stat.REPUTATION)
        ValueRow("Friendship", record.friendship, Stat.FRIENDSHIP)
        ValueRow("Happiness", record.happiness, Stat.HAPPINESS)
        val titles = record.achievements.mapNotNull { Achievements.get(it)?.title }
        if (titles.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Major achievements:", color = Ly.TextDim, fontSize = 15.sp)
            titles.take(6).forEach { t ->
                Row(Modifier.padding(top = 3.dp)) {
                    Text("★", color = Ly.Gold, fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp, top = 1.dp))
                    Text(t, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label:", color = Ly.TextDim, fontSize = 15.sp, modifier = Modifier.width(110.dp))
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ValueRow(label: String, value: Int, stat: Stat) {
    val fill by animateFloatAsState(value / 100f, tween(900), label = "report-bar")
    val color = Ly.statColor(stat)
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$label:", color = Ly.TextDim, fontSize = 15.sp, modifier = Modifier.width(110.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF16254A))) {
            Box(Modifier.fillMaxWidth(fill.coerceIn(0.01f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
        Text("$value / 100", color = color, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(78.dp), textAlign = TextAlign.End)
    }
}

/**
 * The closing card of the report. It appears slowly (fade, small rise and scale) so it feels like
 * a thoughtful last page rather than a notification.
 */
@Composable
fun MottoCard(record: LifeRecord, modifier: Modifier = Modifier) {
    val a = rememberAppear(450, key = record.mottoId)
    val glow by animateFloatAsState(1f, tween(2200), label = "motto-glow")
    Column(
        modifier.fillMaxWidth()
            .graphicsLayer { alpha = a; translationY = (1f - a) * 28f; scaleX = 0.96f + 0.04f * a; scaleY = 0.96f + 0.04f * a }
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF14264F), Color(0xFF0A1430))))
            .border(BorderStroke(1.5.dp, Ly.Gold.copy(alpha = 0.35f + 0.45f * glow)), RoundedCornerShape(20.dp))
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("YOUR LIFE’S MOTTO", color = Ly.Gold, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.width(56.dp).height(2.dp).background(Ly.Gold.copy(alpha = 0.6f)))
        Spacer(Modifier.height(16.dp))
        Text("“", color = Ly.Gold.copy(alpha = 0.55f), fontSize = 44.sp, fontFamily = FontFamily.Serif, lineHeight = 30.sp)
        Text(
            record.mottoText, color = Color.White, fontSize = 23.sp, lineHeight = 32.sp,
            fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text("— Your Life, Age ${record.ageReached}", color = Ly.TextDim, fontSize = 13.sp, letterSpacing = 1.sp)
    }
}

/**
 * The shareable LIFE CARD. It deliberately shows no statistics, only identity, ending and motto.
 * Version 1 renders it on screen; a later version can capture this composable as an image to share.
 */
@Composable
fun LifeCardView(model: LifeCardModel, modifier: Modifier = Modifier) {
    Box(
        modifier.aspectRatio(0.8f).clip(RoundedCornerShape(24.dp))
            .border(BorderStroke(2.dp, Brush.verticalGradient(listOf(Ly.Gold, Ly.BlueLine))), RoundedCornerShape(24.dp))
    ) {
        SceneCanvas(SceneArt.NIGHT_CITY, Modifier.fillMaxSize(), animate = false)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x99040913), Color(0xE6070E1F), Color(0xF2040913)))))
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LIFE: YOUR CHOICE", color = Ly.Gold, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.size(84.dp).clip(CircleShape).background(Ly.Navy700).border(BorderStroke(2.dp, Ly.Gold), CircleShape)) {
                CharacterPortrait(model.gender, model.appearance, model.ageReached, Modifier.fillMaxSize().padding(top = 6.dp), look = model.look)
            }
            Spacer(Modifier.height(8.dp))
            Text(model.playerName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Age ${model.ageReached}  •  ${model.careerTitle}", color = Ly.TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(Ly.Gold).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(model.endingTitle, color = Color(0xFF1A1200), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.weight(1f))
            Text("YOUR LIFE’S MOTTO", color = Ly.Gold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Spacer(Modifier.height(8.dp))
            Text("“${model.motto}”", color = Color.White, fontSize = 19.sp, lineHeight = 26.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(model.byline, color = Ly.TextDim, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text("SAME WORLD. DIFFERENT STORY.", color = Ly.TextDim.copy(alpha = 0.7f), fontSize = 9.sp, letterSpacing = 3.sp)
        }
    }
}

@Composable
fun LifeCardScreen(session: GameSession, record: LifeRecord) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ly.Navy900, Ly.Navy950)))) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            item {
                Text("YOUR LIFE CARD", color = Ly.Gold, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, modifier = Modifier.padding(vertical = 8.dp))
                Text("Saved in your LIFE RECORDS.", color = Ly.TextDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp))
            }
            item { LifeCardView(LifeCardModel.from(record), Modifier.widthIn(max = 380.dp).fillMaxWidth()) }
            item {
                Column(Modifier.widthIn(max = 380.dp).fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LyButton("SHARE AS IMAGE (COMING SOON)", {}, Modifier.fillMaxWidth(), enabled = false, fontSize = 13)
                    LyButton("PLAY ANOTHER LIFE", { session.openCreate() }, Modifier.fillMaxWidth(), ButtonKind.PRIMARY, fontSize = 17)
                    LyButton("BACK TO REPORT", { session.openReport(record) }, Modifier.fillMaxWidth(), ButtonKind.GHOST, fontSize = 13)
                }
            }
        }
    }
}
