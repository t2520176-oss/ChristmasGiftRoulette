package com.example.christmasgiftfinder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftfinder.R
import com.example.christmasgiftfinder.data.FavoritesStore
import com.example.christmasgiftfinder.model.Gift
import com.example.christmasgiftfinder.ui.components.Disclaimer
import com.example.christmasgiftfinder.ui.components.GiftCard
import com.example.christmasgiftfinder.ui.components.ScreenScaffold
import com.example.christmasgiftfinder.ui.theme.InkSoft

@Composable
fun FavoritesScreen(allGifts: List<Gift>, favorites: FavoritesStore, onBack: () -> Unit) {
    val saved = allGifts.filter { favorites.isFavorite(it.id) }.sortedWith(compareBy({ it.country }, { it.name }))
    ScreenScaffold(stringResource(R.string.favorites_title), onBack) {
        if (saved.isEmpty()) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💝", fontSize = 64.sp)
                Text(
                    stringResource(R.string.favorites_empty),
                    modifier = Modifier.padding(top = 12.dp),
                    fontSize = 17.sp,
                    color = InkSoft,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth().fillMaxHeight().navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(saved, key = { it.id }) { gift ->
                    GiftCard(gift, true, { favorites.toggle(gift.id) })
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "disclaimer") {
                    Disclaimer(Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}
