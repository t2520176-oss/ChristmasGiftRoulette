package com.lifeyourchoice.app.ui.cinema

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeyourchoice.app.ui.components.GameLogo
import com.lifeyourchoice.app.ui.components.appear
import com.lifeyourchoice.app.ui.components.rememberAppear
import com.lifeyourchoice.app.ui.screens.MottoCard
import com.lifeyourchoice.app.ui.state.GameSession
import com.lifeyourchoice.app.ui.state.Sfx
import com.lifeyourchoice.app.ui.theme.Ly
import com.lifeyourchoice.core.cinema.CineDirector
import com.lifeyourchoice.core.cinema.EndingPlan
import com.lifeyourchoice.core.cinema.MontageFrame
import com.lifeyourchoice.core.cinema.toScript
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.LifeRecord
import kotlinx.coroutines.delay

private sealed interface EndStep {
    val ms: Int
    class Finale(override val ms: Int, val close: Boolean) : EndStep
    class Memory(val frame: MontageFrame, override val ms: Int = 3900) : EndStep
    data object Motto : EndStep { override val ms = 7500 }
    data object Logo : EndStep { override val ms = 3600 }
}

/**
 * The closing movie of a life: the older player looks back, memories of what actually happened
 * play as a montage, the screen fades, and the life's motto appears before the report.
 */
@Composable
fun EndingScreen(session: GameSession, record: LifeRecord, plan: EndingPlan, state: GameState) {
    val steps = remember(plan) {
        buildList {
            add(EndStep.Finale(6500, close = false))
            plan.memories.forEach { add(EndStep.Memory(it)) }
            add(EndStep.Finale(5200, close = true))
            add(EndStep.Motto)
            add(EndStep.Logo)
        }
    }
    var index by remember { mutableIntStateOf(0) }
    val step = steps.getOrNull(index)
    LaunchedEffect(index) {
        val s = steps.getOrNull(index) ?: run { session.endingFinished(); return@LaunchedEffect }
        if (s is EndStep.Memory) session.audio.sfx(Sfx.PAGE)
        if (s is EndStep.Motto) session.audio.sfx(Sfx.MOTTO)
        delay(s.ms.toLong())
        index++
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (step != null && step !is EndStep.Motto) index++ else if (step is EndStep.Motto) index++
            }
    ) {
        when (step) {
            is EndStep.Finale -> FinaleScene(plan, state, step, record)
            is EndStep.Memory -> MemoryScene(state, step.frame, step.ms)
            EndStep.Motto -> MottoScene(record)
            EndStep.Logo -> LogoScene()
            null -> {}
        }
        Box(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp).clip(RoundedCornerShape(10.dp))
                .background(Color(0x99000000)).border(BorderStroke(1.dp, Ly.BlueLine.copy(alpha = 0.6f)), RoundedCornerShape(10.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { session.endingFinished() }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) { Text("SKIP ▸▸", color = Ly.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp) }
    }
}

/** Runs a [CineDirector] for a static frame (no choices) and draws it. */
@Composable
private fun RunFrame(state: GameState, frame: MontageFrame, pushScale: Float, modifier: Modifier = Modifier) {
    val rt = remember(frame.id, frame.playerAge) {
        val cast = castFor(state, frame.playerAge)
        StageRuntime(cast).also { it.reset(frame.env, frame.time, cast); it.pushScale = pushScale }
    }
    val d = remember(frame.id, frame.playerAge) { CineDirector(frame.toScript(), state) }
    LaunchedEffect(d) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000L).toInt().coerceIn(1, 100)
                    d.update(dt); rt.update(dt, d)
                }
                last = now
            }
        }
    }
    StageView(rt, modifier)
}

@Composable
private fun FinaleScene(plan: EndingPlan, state: GameState, step: EndStep.Finale, record: LifeRecord) {
    val fadeOut by animateFloatAsState(if (step.close) 1f else 0f, tween(step.ms - 500), label = "fade")
    Box(Modifier.fillMaxSize()) {
        RunFrame(state, plan.finale, pushScale = if (step.close) 3.0f else 1.7f, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000)))))
        if (!step.close) {
            val a = rememberAppear(900)
            Text(
                plan.finaleCaption, color = Color.White, fontSize = 20.sp, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(32.dp).appear(a, 12f)
            )
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = fadeOut * 0.96f)))
    }
}

@Composable
private fun MemoryScene(state: GameState, frame: MontageFrame, ms: Int) {
    val a = rememberAppear(450, key = frame.id)
    Box(Modifier.fillMaxSize()) {
        RunFrame(state, frame, pushScale = 1.1f, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(28.dp).appear(a, 14f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("AGE ${frame.playerAge}", color = Ly.Gold, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text(frame.caption, color = Color.White, fontSize = 21.sp, lineHeight = 28.sp, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp).widthIn(max = 480.dp))
        }
        // film-like dip between memories
        val dip by animateFloatAsState(1f, tween(ms), label = "dip")
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = (1f - dip).coerceIn(0f, 1f) * 0f)))
    }
}

@Composable
private fun MottoScene(record: LifeRecord) {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 560.dp).padding(24.dp)) {
            MottoCard(record)
        }
    }
}

@Composable
private fun LogoScene() {
    val a = rememberAppear(300)
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(Modifier.appear(a, 12f), horizontalAlignment = Alignment.CenterHorizontally) {
            GameLogo()
            Box(Modifier.padding(top = 12.dp).fillMaxWidth(0.3f).height(2.dp).background(Ly.Gold.copy(alpha = 0.7f)))
            Text("“Every decision builds the life you will live.”", color = Ly.TextDim, fontSize = 14.sp, fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp, start = 24.dp, end = 24.dp), fontFamily = FontFamily.Serif)
        }
    }
}
