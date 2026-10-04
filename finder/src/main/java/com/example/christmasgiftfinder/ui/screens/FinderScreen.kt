package com.example.christmasgiftfinder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftfinder.R
import com.example.christmasgiftfinder.model.Country
import com.example.christmasgiftfinder.model.GiftStyle
import com.example.christmasgiftfinder.model.Recipient
import com.example.christmasgiftfinder.ui.components.BigButton
import com.example.christmasgiftfinder.ui.components.ContentMaxWidth
import com.example.christmasgiftfinder.ui.components.ScreenScaffold
import com.example.christmasgiftfinder.ui.components.SelectorField
import com.example.christmasgiftfinder.ui.theme.ChristmasRed
import com.example.christmasgiftfinder.ui.theme.InkSoft
import com.example.christmasgiftfinder.ui.theme.Snow
import androidx.compose.runtime.remember

@Composable
fun FinderScreen(
    country: Country?,
    budgetText: String,
    budgetErrorRes: Int?,
    countryError: Boolean,
    recipient: Recipient,
    style: GiftStyle,
    focusBudget: Boolean,
    onFocusBudgetHandled: () -> Unit,
    onBack: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenCountry: () -> Unit,
    onBudgetChange: (String) -> Unit,
    onOpenRecipient: () -> Unit,
    onOpenStyle: () -> Unit,
    onFind: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(focusBudget) {
        if (focusBudget) {
            runCatching { focusRequester.requestFocus() }
            onFocusBudgetHandled()
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.finder_title),
        onBack = onBack,
        action = {
            IconButton(onClick = onOpenFavorites, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Favorite, stringResource(R.string.open_favorites), tint = Snow)
            }
        },
    ) {
        Column(
            Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // --- Country (currency follows automatically) ---
            FieldLabel("🌐", stringResource(R.string.label_country))
            SelectorField(
                text = country?.name ?: stringResource(R.string.select_country_prompt),
                leading = country?.flag,
                isPlaceholder = country == null,
                onClick = onOpenCountry,
            )
            if (country != null) {
                Text(
                    stringResource(R.string.label_currency, country.currencyCode, country.currencySymbol),
                    fontSize = 14.sp,
                    color = InkSoft,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            if (countryError) {
                Text(stringResource(R.string.country_required), fontSize = 14.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 4.dp))
            }

            Spacer(Modifier.height(10.dp))

            // --- Budget in the selected country's currency ---
            FieldLabel("🪙", stringResource(R.string.label_budget))
            OutlinedTextField(
                value = budgetText,
                onValueChange = onBudgetChange,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
                prefix = country?.let { { Text(it.currencySymbol + " ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ChristmasRed) } },
                placeholder = country?.let { { Text(stringResource(R.string.budget_hint_example, it.typicalBudget), fontSize = 18.sp) } },
                isError = budgetErrorRes != null,
                supportingText = budgetErrorRes?.let { { Text(stringResource(it)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onFind()
                }),
            )

            Spacer(Modifier.height(10.dp))

            // --- Recipient (optional, default Anyone) ---
            FieldLabel("🙂", stringResource(R.string.label_recipient))
            SelectorField(
                text = stringResource(recipient.labelRes),
                leading = recipient.icon,
                onClick = onOpenRecipient,
            )

            Spacer(Modifier.height(10.dp))

            // --- Gift style (default Any) ---
            FieldLabel("🎁", stringResource(R.string.label_style))
            SelectorField(
                text = stringResource(style.labelRes),
                leading = style.icon,
                onClick = onOpenStyle,
            )

            Spacer(Modifier.height(14.dp))
            BigButton(
                text = stringResource(R.string.find_gift_ideas),
                emoji = "🎁",
                onClick = {
                    focusManager.clearFocus()
                    onFind()
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("🎄  🎁  ⭐  🎁  🎄", fontSize = 26.sp)
            }
        }
    }
}

@Composable
private fun FieldLabel(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.size(10.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
