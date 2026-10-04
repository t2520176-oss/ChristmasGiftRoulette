package com.lifeyourchoice.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeyourchoice.app.ui.art.CharacterPortrait
import com.lifeyourchoice.app.ui.art.Looks
import com.lifeyourchoice.app.ui.art.SceneCanvas
import com.lifeyourchoice.app.ui.components.ButtonKind
import com.lifeyourchoice.app.ui.components.GameLogo
import com.lifeyourchoice.app.ui.components.LyButton
import com.lifeyourchoice.app.ui.components.appear
import com.lifeyourchoice.app.ui.components.rememberAppear
import com.lifeyourchoice.app.ui.state.GameSession
import com.lifeyourchoice.app.ui.theme.Ly
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.SceneArt
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MainMenuScreen(session: GameSession) {
    Box(Modifier.fillMaxSize().background(Ly.Navy950)) {
        SceneCanvas(SceneArt.NIGHT_CITY, Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0x33040913), Color(0x99040913), Color(0xF2040913)))
            )
        )
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > 680.dp && maxWidth > maxHeight
            if (wide) {
                BackSilhouette(Modifier.align(Alignment.BottomEnd).fillMaxHeight(0.85f).aspectRatio(0.62f).padding(end = 40.dp))
            } else {
                BackSilhouette(
                    Modifier.align(Alignment.BottomStart).height(maxHeight * 0.34f).aspectRatio(0.62f).padding(start = 8.dp).appear(0.9f, 0f)
                )
            }
            val a = rememberAppear(0)
            Column(
                Modifier
                    .then(if (wide) Modifier.fillMaxHeight().width(440.dp) else Modifier.fillMaxSize())
                    .statusBarsPadding().navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 20.dp)
                    .appear(a, 18f),
                horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(if (wide) 28.dp else 12.dp))
                GameLogo()
                Spacer(Modifier.height(8.dp))
                Text("SAME WORLD. DIFFERENT STORY.", color = Ly.Text, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(if (wide) 40.dp else 28.dp))
                // Leave room for the silhouette on narrow screens.
                Spacer(Modifier.height(if (wide) 0.dp else 90.dp))
                Column(Modifier.widthIn(max = 380.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LyButton("NEW LIFE", { session.openCreate() }, Modifier.fillMaxWidth(), ButtonKind.PRIMARY, fontSize = 19)
                    LyButton("CONTINUE", { session.continueLife() }, Modifier.fillMaxWidth(), enabled = session.hasSave)
                    LyButton("LIFE RECORDS", { session.openRecords() }, Modifier.fillMaxWidth())
                    LyButton("SETTINGS", { session.openSettings() }, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(26.dp))
                Text(
                    "“Every decision builds the life you will live.”",
                    color = Ly.TextDim, fontSize = 13.sp, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 380.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("Offline • No account • Your progress stays on this device", color = Ly.TextDim.copy(alpha = 0.6f), fontSize = 10.sp)
            }
        }
    }
}

/** A teenager seen from behind with a backpack, looking out over the city. */
@Composable
private fun BackSilhouette(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val body = Color(0xFF0A1226)
        val rim = Color(0xFF3B6BD6)
        // backpack
        drawRoundRect(Color(0xFF111B38), Offset(w * 0.18f, h * 0.44f), androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.38f),
            androidx.compose.ui.geometry.CornerRadius(w * 0.1f))
        drawRoundRect(rim.copy(alpha = 0.35f), Offset(w * 0.18f, h * 0.44f), androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.38f),
            androidx.compose.ui.geometry.CornerRadius(w * 0.1f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        drawRoundRect(Color(0xFF16244A), Offset(w * 0.3f, h * 0.6f), androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.14f),
            androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
        // torso (hoodie)
        val torso = Path().apply {
            moveTo(w * 0.1f, h)
            cubicTo(w * 0.1f, h * 0.62f, w * 0.18f, h * 0.46f, w * 0.34f, h * 0.4f)
            lineTo(w * 0.66f, h * 0.4f)
            cubicTo(w * 0.82f, h * 0.46f, w * 0.9f, h * 0.62f, w * 0.9f, h)
            close()
        }
        drawPath(torso, body)
        drawPath(torso, rim.copy(alpha = 0.35f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        // hood and head
        drawCircle(body, w * 0.2f, Offset(w * 0.5f, h * 0.3f))
        drawCircle(rim.copy(alpha = 0.3f), w * 0.2f, Offset(w * 0.5f, h * 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        drawRoundRect(body, Offset(w * 0.36f, h * 0.36f), androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.08f),
            androidx.compose.ui.geometry.CornerRadius(w * 0.06f))
    }
}

private val boyNames = listOf("Alex", "Ryan", "Leo", "Ethan", "Noah", "Caleb", "Jake", "Daniel", "Marcus", "Kai", "Owen", "Theo")
private val girlNames = listOf("Mia", "Sofia", "Chloe", "Ava", "Grace", "Zoe", "Hannah", "Naomi", "Elena", "Maya", "Nora", "Ivy")

@Composable
fun CharacterCreationScreen(session: GameSession) {
    var gender by remember { mutableStateOf(Gender.BOY) }
    var appearance by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    val looks = if (gender == Gender.BOY) Looks.boys else Looks.girls

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ly.Navy900, Ly.Navy950)))) {
        SceneCanvas(SceneArt.NIGHT_CITY, Modifier.fillMaxSize().appear(0.28f, 0f))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CHOOSE YOUR CHARACTER", color = Ly.Text, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, textAlign = TextAlign.Center)
            Text("Who will you be?", color = Ly.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

            Row(Modifier.widthIn(max = 520.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GenderCard("BOY", Gender.BOY, gender == Gender.BOY, appearance, Modifier.weight(1f)) { gender = Gender.BOY; appearance = 0; session.audio.sfx(com.lifeyourchoice.app.ui.state.Sfx.TAP) }
                GenderCard("GIRL", Gender.GIRL, gender == Gender.GIRL, appearance, Modifier.weight(1f)) { gender = Gender.GIRL; appearance = 0; session.audio.sfx(com.lifeyourchoice.app.ui.state.Sfx.TAP) }
            }

            Text("CUSTOMIZE YOUR LOOK", color = Ly.Text, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                looks.forEachIndexed { i, look ->
                    val selected = i == appearance
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Ly.Navy700)
                                .border(BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) Ly.Gold else Ly.BlueLine), RoundedCornerShape(12.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    appearance = i; session.audio.sfx(com.lifeyourchoice.app.ui.state.Sfx.TAP)
                                }
                        ) {
                            CharacterPortrait(gender, i, 17, Modifier.fillMaxSize().padding(top = 6.dp))
                        }
                        Text(look.name, color = if (selected) Ly.Gold else Ly.TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            Row(Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(12.dp)).background(Ly.Navy800)
                        .border(BorderStroke(1.2.dp, Ly.BlueLine), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { v -> name = v.filter { it.isLetter() || it == ' ' || it == '\'' || it == '-' }.take(14) },
                        singleLine = true,
                        textStyle = TextStyle(color = Ly.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                        cursorBrush = SolidColor(Ly.Gold),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        decorationBox = { inner ->
                            if (name.isEmpty()) Text("Enter your name…", color = Ly.TextDim, fontSize = 16.sp)
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Ly.Navy700)
                        .border(BorderStroke(1.2.dp, Ly.BlueLine), RoundedCornerShape(12.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            val pool = if (gender == Gender.BOY) boyNames else girlNames
                            name = pool[Random.nextInt(pool.size)]
                            session.audio.sfx(com.lifeyourchoice.app.ui.state.Sfx.TAP)
                        },
                    contentAlignment = Alignment.Center
                ) { Text("🎲", fontSize = 22.sp) }
            }

            LyButton(
                "CONFIRM", { session.startNewLife(name, gender, appearance) },
                Modifier.padding(top = 22.dp).widthIn(max = 320.dp).fillMaxWidth(), ButtonKind.PRIMARY, fontSize = 18
            )
            LyButton("BACK", { session.goMenu() }, Modifier.padding(top = 10.dp).widthIn(max = 320.dp).fillMaxWidth(), ButtonKind.GHOST, fontSize = 14)
        }
    }
}

@Composable
private fun GenderCard(label: String, gender: Gender, selected: Boolean, appearance: Int, modifier: Modifier, onClick: () -> Unit) {
    val glow by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "sel")
    val accent = if (gender == Gender.BOY) Ly.Blue else Color(0xFFD65C9A)
    Column(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.18f + 0.2f * glow), Ly.Navy800)))
            .border(BorderStroke((1 + 2 * glow).dp, if (selected) accent else Ly.BlueLine.copy(alpha = 0.5f)), RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CharacterPortrait(gender, if (selected) appearance else 0, 17, Modifier.fillMaxWidth().aspectRatio(0.9f).padding(top = 10.dp, start = 10.dp, end = 10.dp))
        Box(
            Modifier.fillMaxWidth().background(if (selected) accent else Ly.Navy700).padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) { Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp) }
    }
}

/** "Your life begins now." Tap to skip; otherwise continues on its own. */
@Composable
fun IntroScreen(session: GameSession) {
    val hud = session.hud
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(2600)
        session.beginPlaying()
    }
    val a by animateFloatAsState(if (visible) 1f else 0f, tween(1100), label = "intro")
    Box(
        Modifier.fillMaxSize().background(Ly.Navy950)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { session.beginPlaying() },
        contentAlignment = Alignment.Center
    ) {
        SceneCanvas(SceneArt.SKYLINE, Modifier.fillMaxSize().appear(0.35f * a, 0f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.appear(a, 16f).padding(24.dp)) {
            if (hud != null) {
                CharacterPortrait(hud.gender, hud.appearance, hud.ageYears, Modifier.size(150.dp, 180.dp))
                Spacer(Modifier.height(10.dp))
                Text(hud.playerName.uppercase(), color = Ly.Gold, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 3.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text("Your life begins now.", color = Ly.Text, fontSize = 28.sp, fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Age ${hud?.ageYears ?: 15}", color = Ly.TextDim, fontSize = 14.sp, letterSpacing = 2.sp)
        }
    }
}
