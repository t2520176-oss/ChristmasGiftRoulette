package com.example.christmasgiftfinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftfinder.R
import com.example.christmasgiftfinder.logic.PriceFormatter
import com.example.christmasgiftfinder.model.Gift
import com.example.christmasgiftfinder.ui.theme.ChristmasRed
import com.example.christmasgiftfinder.ui.theme.DeepRed
import com.example.christmasgiftfinder.ui.theme.Gold
import com.example.christmasgiftfinder.ui.theme.InkSoft
import com.example.christmasgiftfinder.ui.theme.Snow

/** Max content width so layouts stay pleasant on tablets. */
val ContentMaxWidth = 640.dp

/** Red Christmas header (with subtle snow) + a content area. Handles the status-bar inset. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Brush.verticalGradient(listOf(ChristmasRed, DeepRed))),
        ) {
            Snowfall(Modifier.matchParentSize(), count = 14, seed = title.length)
            Row(
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Snow)
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    color = Snow,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                if (action != null) action() else Spacer(Modifier.size(48.dp))
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter, content = content)
    }
}

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    outlined: Boolean = false,
    containerColor: Color = ChristmasRed,
    contentColor: Color = Snow,
) {
    val shape = RoundedCornerShape(30.dp)
    val sizeModifier = modifier.fillMaxWidth().heightIn(min = 58.dp)
    val padding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    val label: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (emoji != null) Text(emoji, fontSize = 22.sp)
            Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, textAlign = TextAlign.Center)
        }
    }
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = sizeModifier,
            shape = shape,
            border = BorderStroke(2.dp, containerColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = containerColor),
            contentPadding = padding,
        ) { label() }
    } else {
        Button(
            onClick = onClick,
            modifier = sizeModifier,
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            contentPadding = padding,
        ) { label() }
    }
}

/** A tappable "dropdown-looking" field that opens a selection screen. */
@Composable
fun SelectorField(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: String? = null, isPlaceholder: Boolean = false) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 58.dp),
        shape = RoundedCornerShape(16.dp),
        color = Snow,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Text(leading, fontSize = 22.sp)
                Spacer(Modifier.size(12.dp))
            }
            Text(
                text,
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                color = if (isPlaceholder) InkSoft else MaterialTheme.colorScheme.onSurface,
            )
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = InkSoft)
        }
    }
}

/** The bundled "illustration": an emoji on a soft rounded tile (no downloaded images). */
@Composable
fun IconTile(emoji: String, size: Dp, tint: Color = Color(0xFFFFE9E6)) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(tint),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value * 0.5f).sp)
    }
}

private val TileTints = listOf(Color(0xFFFFE9E6), Color(0xFFE4F2E5), Color(0xFFFFF3D6))

fun tileTintFor(name: String): Color = TileTints[Math.floorMod(name.hashCode(), TileTints.size)]

@Composable
fun GiftCard(
    gift: Gift,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    mayExceedBudget: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Snow),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(gift.icon, 68.dp, tileTintFor(gift.name))
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(gift.name, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(PriceFormatter.range(gift), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ChristmasRed)
                Text(
                    stringResource(R.string.good_for, gift.recipients.joinToString(", ")),
                    fontSize = 14.sp,
                    color = InkSoft,
                )
                if (gift.description.isNotBlank()) {
                    Text(gift.description, fontSize = 14.sp, color = InkSoft, fontStyle = FontStyle.Italic)
                }
                if (mayExceedBudget) {
                    Text(
                        stringResource(R.string.may_exceed_budget),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9A5B00),
                    )
                }
            }
            FavoriteButton(isFavorite, onToggleFavorite)
        }
    }
}

@Composable
fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.size(48.dp)) {
        Icon(
            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
            tint = if (isFavorite) ChristmasRed else InkSoft,
        )
    }
}

@Composable
fun Disclaimer(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.price_disclaimer),
        modifier = modifier.fillMaxWidth(),
        fontSize = 13.sp,
        color = InkSoft,
        textAlign = TextAlign.Center,
        fontStyle = FontStyle.Italic,
    )
}

/** Small gold-accented section label. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 6.dp, height = 20.dp).clip(RoundedCornerShape(3.dp)).background(Gold))
        Spacer(Modifier.size(10.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
    }
}
