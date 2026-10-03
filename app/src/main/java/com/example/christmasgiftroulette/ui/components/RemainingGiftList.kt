package com.example.christmasgiftroulette.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.example.christmasgiftroulette.model.GiftItem
import com.example.christmasgiftroulette.ui.theme.ChristmasColors

/** Gift lines "Chocolate Box — ₩15,000" as lazy list items (keyed by the gift id). */
fun LazyListScope.giftRows(gifts: List<GiftItem>, numbered: Boolean = false, keyPrefix: String = "gift") {
    itemsIndexed(gifts, key = { _, g -> "$keyPrefix-${g.id}" }) { index, gift ->
        GiftRow(gift, if (numbered) index + 1 else null)
    }
}

@Composable
fun GiftRow(gift: GiftItem, number: Int? = null, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ChristmasColors.Cream),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (number != null) {
                Text("$number.", style = MaterialTheme.typography.titleMedium, color = ChristmasColors.Red)
            } else {
                GiftIcon(Modifier.size(32.dp))
            }
            Text(
                gift.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            gift.formattedAmount?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = ChristmasColors.Green)
            }
        }
    }
}
