package com.lifeyourchoice.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeyourchoice.app.ui.theme.Ly
import com.lifeyourchoice.core.model.Stat
import kotlinx.coroutines.delay

/** Eases a value from 0 to 1 after [delayMs]; restarts whenever [key] changes. */
@Composable
fun rememberAppear(delayMs: Int = 0, key: Any? = Unit): Float {
    var go by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        delay(delayMs.toLong())
        go = true
    }
    val a by animateFloatAsState(if (go) 1f else 0f, tween(420), label = "appear")
    return a
}

/** Fades and lifts content in; used for choices and cards to give scenes a sense of progress. */
fun Modifier.appear(progress: Float, lift: Float = 22f): Modifier = graphicsLayer {
    alpha = progress
    translationY = (1f - progress) * lift
}

fun Modifier.panel(radius: Int = 16, alpha: Float = 0.92f): Modifier = this
    .clip(RoundedCornerShape(radius.dp))
    .background(Ly.Panel.copy(alpha = alpha))
    .border(BorderStroke(1.dp, Ly.BlueLine.copy(alpha = 0.55f)), RoundedCornerShape(radius.dp))

enum class ButtonKind { PRIMARY, SECONDARY, GHOST }

@Composable
fun LyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.SECONDARY,
    enabled: Boolean = true,
    fontSize: Int = 16
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, tween(90), label = "press")
    val shape = RoundedCornerShape(14.dp)
    val bg: Brush = when (kind) {
        ButtonKind.PRIMARY -> Brush.verticalGradient(listOf(Ly.Gold, Ly.GoldDeep))
        ButtonKind.SECONDARY -> Brush.verticalGradient(listOf(Ly.Navy600, Ly.Navy700))
        ButtonKind.GHOST -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val border = when (kind) {
        ButtonKind.PRIMARY -> Ly.Gold
        ButtonKind.SECONDARY -> Ly.BlueLine
        ButtonKind.GHOST -> Ly.BlueLine.copy(alpha = 0.6f)
    }
    val fg = if (kind == ButtonKind.PRIMARY) Color(0xFF1A1200) else Ly.Text
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.4f }
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.5.dp, border), shape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, fontSize = fontSize.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, textAlign = TextAlign.Center)
    }
}

/** One answer in a scenario: letter badge, text, optional gold "unlocked by your past" caption. */
@Composable
fun ChoiceButton(
    letter: Char,
    text: String,
    enabled: Boolean,
    lockedHint: String?,
    badge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.98f else 1f, tween(90), label = "press")
    val shape = RoundedCornerShape(12.dp)
    val unlocked = badge != null
    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    if (unlocked) listOf(Color(0xFF3A2D12), Ly.Navy700) else listOf(Ly.Navy700, Color(0xFF0E2350))
                )
            )
            .border(BorderStroke(1.2.dp, if (unlocked) Ly.Gold.copy(alpha = 0.8f) else Ly.BlueLine), shape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(if (unlocked) Ly.Gold else Ly.Blue.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(letter.toString(), color = if (unlocked) Color(0xFF1A1200) else Ly.BlueSoft, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(text, color = Ly.Text, fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
        }
        if (badge != null) {
            Text("★ $badge", color = Ly.Gold, fontSize = 11.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(start = 38.dp, top = 3.dp))
        }
        if (!enabled && lockedHint != null) {
            Text("🔒 $lockedHint", color = Ly.TextDim, fontSize = 11.sp, modifier = Modifier.padding(start = 38.dp, top = 3.dp))
        }
    }
}

/** A coloured stat meter with an animated fill, as in the stats panel. */
@Composable
fun StatBar(stat: Stat, value: Int, modifier: Modifier = Modifier) {
    val fill by animateFloatAsState(value / 100f, tween(700), label = "stat")
    val color = Ly.statColor(stat)
    Row(modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)).background(color.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
            Text(Ly.statGlyph(stat), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(10.dp))
        Text(stat.label, color = Ly.Text, fontSize = 14.sp, modifier = Modifier.width(92.dp))
        Box(
            Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF16254A))
        ) {
            Box(
                Modifier.fillMaxWidth(fill.coerceIn(0.01f, 1f)).height(9.dp).clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)))
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(value.toString(), color = Ly.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
    }
}

/** "+5 Friendship" style result chip for the consequence screen. */
@Composable
fun DeltaChip(stat: Stat, delta: Int, modifier: Modifier = Modifier) {
    val good = delta > 0
    val tint = if (good) Ly.Good else Ly.Bad
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, tint.copy(alpha = 0.55f)), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(Ly.statGlyph(stat), color = Ly.statColor(stat), fontSize = 12.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(6.dp))
        Text(stat.label, color = Ly.Text, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Text((if (good) "+" else "") + delta, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

/** Small pill used in the top bar (age/title, money, energy). */
@Composable
fun HudPill(text: String, modifier: Modifier = Modifier, glyph: String? = null, glyphColor: Color = Ly.Gold) {
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xCC081028))
            .border(BorderStroke(1.dp, Ly.BlueLine.copy(alpha = 0.6f)), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (glyph != null) {
            Text(glyph, color = glyphColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = Ly.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, color: Color = Ly.Gold) {
    Row(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(26.dp).height(2.dp).background(color.copy(alpha = 0.7f)))
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, modifier = Modifier.padding(horizontal = 10.dp))
        Box(Modifier.weight(1f).height(2.dp).background(color.copy(alpha = 0.25f)))
    }
}

/** The game title lock-up used on the main menu. */
@Composable
fun GameLogo(modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.Start) {
        Text(
            "LIFE", color = Color.White, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic,
            fontSize = if (compact) 44.sp else 72.sp, letterSpacing = 2.sp, lineHeight = if (compact) 44.sp else 72.sp
        )
        Text(
            "YOUR CHOICE", color = Ly.Gold, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic,
            fontSize = if (compact) 22.sp else 34.sp, letterSpacing = 1.sp, lineHeight = if (compact) 24.sp else 36.sp
        )
    }
}

/** A thin gold underline accent. */
fun Modifier.goldRule(): Modifier = drawBehind {
    drawRoundRect(
        Brush.horizontalGradient(listOf(Color.Transparent, Ly.Gold, Color.Transparent)),
        Offset(0f, size.height - 3f), Size(size.width, 3f), CornerRadius(2f)
    )
}
