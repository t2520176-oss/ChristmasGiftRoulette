package com.example.christmasgiftfinder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftfinder.R
import com.example.christmasgiftfinder.logic.PriceFormatter
import com.example.christmasgiftfinder.model.Gift
import com.example.christmasgiftfinder.ui.components.BigButton
import com.example.christmasgiftfinder.ui.components.ContentMaxWidth
import com.example.christmasgiftfinder.ui.components.Confetti
import com.example.christmasgiftfinder.ui.components.Disclaimer
import com.example.christmasgiftfinder.ui.components.FavoriteButton
import com.example.christmasgiftfinder.ui.components.IconTile
import com.example.christmasgiftfinder.ui.components.ScreenScaffold
import com.example.christmasgiftfinder.ui.components.tileTintFor
import com.example.christmasgiftfinder.ui.theme.ChristmasRed
import com.example.christmasgiftfinder.ui.theme.Evergreen
import com.example.christmasgiftfinder.ui.theme.InkSoft
import com.example.christmasgiftfinder.ui.theme.Snow

@Composable
fun SurpriseScreen(
    gift: Gift,
    mayExceedBudget: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onTryAgain: () -> Unit,
    onBackToResults: () -> Unit,
) {
    ScreenScaffold(stringResource(R.string.surprise_title), onBackToResults) {
        Box(Modifier.matchParentSize()) {
            Confetti(Modifier.matchParentSize())
            Column(
                Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Snow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconTile(gift.icon, 150.dp, tileTintFor(gift.name))
                        Text(
                            "🎁 " + stringResource(R.string.surprise_heading),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Evergreen,
                            letterSpacing = 1.sp,
                        )
                        Text(gift.name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        Text(PriceFormatter.range(gift), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ChristmasRed)
                        Text(
                            stringResource(R.string.good_for, gift.recipients.joinToString(" / ")),
                            fontSize = 16.sp,
                            color = InkSoft,
                            textAlign = TextAlign.Center,
                        )
                        if (gift.description.isNotBlank()) {
                            Text("“${gift.description}”", fontSize = 16.sp, color = InkSoft, textAlign = TextAlign.Center)
                        }
                        if (mayExceedBudget) {
                            Text(
                                stringResource(R.string.may_exceed_budget),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF9A5B00),
                                textAlign = TextAlign.Center,
                            )
                        }
                        FavoriteButton(isFavorite, onToggleFavorite)
                    }
                }
                BigButton(stringResource(R.string.try_again), onClick = onTryAgain, emoji = "🎲")
                BigButton(stringResource(R.string.back_to_results), onClick = onBackToResults, emoji = "←", outlined = true)
                Disclaimer()
            }
        }
    }
}
