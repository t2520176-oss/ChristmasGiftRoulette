package com.example.christmasgiftfinder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftfinder.R
import com.example.christmasgiftfinder.data.FavoritesStore
import com.example.christmasgiftfinder.logic.PriceFormatter
import com.example.christmasgiftfinder.model.GiftQuery
import com.example.christmasgiftfinder.model.GiftSearchResult
import com.example.christmasgiftfinder.ui.components.BigButton
import com.example.christmasgiftfinder.ui.components.ContentMaxWidth
import com.example.christmasgiftfinder.ui.components.Disclaimer
import com.example.christmasgiftfinder.ui.components.GiftCard
import com.example.christmasgiftfinder.ui.components.ScreenScaffold
import com.example.christmasgiftfinder.ui.components.SectionLabel
import com.example.christmasgiftfinder.ui.theme.InkSoft
import com.example.christmasgiftfinder.ui.theme.Snow

private val FullWidth: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan) = { GridItemSpan(maxLineSpan) }

@Composable
fun ResultsScreen(
    query: GiftQuery,
    result: GiftSearchResult,
    favorites: FavoritesStore,
    onBack: () -> Unit,
    onSurprise: () -> Unit,
    onChangeBudget: () -> Unit,
) {
    ScreenScaffold(stringResource(R.string.results_title), onBack) {
        if (result.isEmpty) {
            NoResults(onChangeBudget)
        } else {
            LazyVerticalGrid(
                // One column on phones, two or more on tablets.
                columns = GridCells.Adaptive(minSize = 320.dp),
                modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth().fillMaxHeight().navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = FullWidth, key = "summary") { Summary(query) }
                item(span = FullWidth, key = "surprise") {
                    BigButton(stringResource(R.string.surprise_me), onClick = onSurprise, emoji = "🎲")
                }
                items(result.affordable, key = { it.id }) { gift ->
                    GiftCard(gift, favorites.isFavorite(gift.id), { favorites.toggle(gift.id) })
                }
                if (result.stretch.isNotEmpty()) {
                    item(span = FullWidth, key = "stretch-title") {
                        SectionLabel(stringResource(R.string.stretch_section_title), Modifier.padding(top = 8.dp))
                    }
                    items(result.stretch, key = { "s|" + it.id }) { gift ->
                        GiftCard(gift, favorites.isFavorite(gift.id), { favorites.toggle(gift.id) }, mayExceedBudget = true)
                    }
                }
                item(span = FullWidth, key = "disclaimer") {
                    Disclaimer(Modifier.padding(top = 8.dp, bottom = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun Summary(query: GiftQuery) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Snow),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${query.country.flag} ${query.country.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.summary_budget, PriceFormatter.amount(query.country.currencySymbol, query.budget)),
                fontSize = 16.sp,
                color = InkSoft,
            )
            Text(stringResource(R.string.summary_recipient, stringResource(query.recipient.labelRes)), fontSize = 16.sp, color = InkSoft)
            Text(stringResource(R.string.summary_style, stringResource(query.style.labelRes)), fontSize = 16.sp, color = InkSoft)
        }
    }
}

@Composable
private fun NoResults(onChangeBudget: () -> Unit) {
    Column(
        Modifier
            .widthIn(max = ContentMaxWidth)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("🎁", fontSize = 72.sp)
        Text(
            stringResource(R.string.no_results_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.no_results_hint),
            fontSize = 17.sp,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        BigButton(stringResource(R.string.change_budget), onClick = onChangeBudget, emoji = "🪙")
        Spacer(Modifier.height(16.dp))
        Disclaimer()
    }
}
