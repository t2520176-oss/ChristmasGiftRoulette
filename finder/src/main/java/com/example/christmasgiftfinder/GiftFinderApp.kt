package com.example.christmasgiftfinder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.christmasgiftfinder.data.Countries
import com.example.christmasgiftfinder.data.FavoritesStore
import com.example.christmasgiftfinder.logic.BudgetParser
import com.example.christmasgiftfinder.logic.BudgetResult
import com.example.christmasgiftfinder.logic.GiftFilter
import com.example.christmasgiftfinder.logic.SurprisePicker
import com.example.christmasgiftfinder.model.Gift
import com.example.christmasgiftfinder.model.GiftQuery
import com.example.christmasgiftfinder.model.GiftStyle
import com.example.christmasgiftfinder.model.Recipient
import com.example.christmasgiftfinder.navigation.AppNavigator
import com.example.christmasgiftfinder.navigation.Route
import com.example.christmasgiftfinder.ui.screens.CountryScreen
import com.example.christmasgiftfinder.ui.screens.FavoritesScreen
import com.example.christmasgiftfinder.ui.screens.FinderScreen
import com.example.christmasgiftfinder.ui.screens.Option
import com.example.christmasgiftfinder.ui.screens.OptionListScreen
import com.example.christmasgiftfinder.ui.screens.ResultsScreen
import com.example.christmasgiftfinder.ui.screens.SurpriseScreen
import com.example.christmasgiftfinder.ui.screens.WelcomeScreen
import com.example.christmasgiftfinder.ui.theme.ChristmasTheme

/**
 * Root composable: owns the form state (saved across rotation / process death) and shows the screen
 * for the current [Route]. Everything runs offline from the bundled [gifts].
 */
@Composable
fun GiftFinderApp(nav: AppNavigator, gifts: List<Gift>, favorites: FavoritesStore) {
    var countryName by rememberSaveable { mutableStateOf<String?>(null) }
    var budgetText by rememberSaveable { mutableStateOf("") }
    var recipientKey by rememberSaveable { mutableStateOf(Recipient.ANYONE.key) }
    var styleKey by rememberSaveable { mutableStateOf(GiftStyle.ANY.key) }
    var budgetErrorRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var countryError by rememberSaveable { mutableStateOf(false) }
    var focusBudget by rememberSaveable { mutableStateOf(false) }
    var surpriseId by rememberSaveable { mutableStateOf<String?>(null) }

    val country = Countries.byName(countryName)
    val recipient = Recipient.fromKey(recipientKey)
    val style = GiftStyle.fromKey(styleKey)

    // The query/results exist only while the form is valid.
    val budget = (BudgetParser.parse(budgetText) as? BudgetResult.Valid)?.amount
    val query = if (country != null && budget != null) GiftQuery(country, budget, recipient, style) else null
    val result = remember(query, gifts) { query?.let { GiftFilter.search(gifts, it) } }

    fun validateAndFind() {
        countryError = country == null
        val parsed = BudgetParser.parse(budgetText)
        budgetErrorRes = (parsed as? BudgetResult.Invalid)?.error?.messageRes
        if (country != null && parsed is BudgetResult.Valid) nav.push(Route.Results)
    }

    ChristmasTheme {
        Box(Modifier.fillMaxSize()) {
            when (nav.current) {
                Route.Welcome -> WelcomeScreen(onGetStarted = { nav.push(Route.Finder) })

                Route.Finder -> FinderScreen(
                    country = country,
                    budgetText = budgetText,
                    budgetErrorRes = budgetErrorRes,
                    countryError = countryError,
                    recipient = recipient,
                    style = style,
                    focusBudget = focusBudget,
                    onFocusBudgetHandled = { focusBudget = false },
                    onBack = { nav.pop() },
                    onOpenFavorites = { nav.push(Route.Favorites) },
                    onOpenCountry = { nav.push(Route.Country) },
                    onBudgetChange = {
                        budgetText = it
                        budgetErrorRes = null
                    },
                    onOpenRecipient = { nav.push(Route.Recipient) },
                    onOpenStyle = { nav.push(Route.Style) },
                    onFind = ::validateAndFind,
                )

                Route.Country -> CountryScreen(
                    selected = country,
                    onSelect = { picked ->
                        // Pre-fill a typical budget in the new currency unless the user typed their own.
                        if (budgetText.isBlank() || budgetText == country?.typicalBudget) {
                            budgetText = picked.typicalBudget
                        }
                        countryName = picked.name
                        countryError = false
                        budgetErrorRes = null
                        nav.pop()
                    },
                    onBack = { nav.pop() },
                )

                Route.Recipient -> {
                    val options = Recipient.entries.map { Option(it.key, it.icon, stringResource(it.labelRes)) }
                    OptionListScreen(
                        title = stringResource(R.string.select_recipient_title),
                        options = options,
                        selectedKey = recipient.key,
                        onSelect = { recipientKey = it; nav.pop() },
                        onBack = { nav.pop() },
                    )
                }

                Route.Style -> {
                    val options = GiftStyle.entries.map { Option(it.key, it.icon, stringResource(it.labelRes)) }
                    OptionListScreen(
                        title = stringResource(R.string.select_style_title),
                        options = options,
                        selectedKey = style.key,
                        onSelect = { styleKey = it; nav.pop() },
                        onBack = { nav.pop() },
                    )
                }

                Route.Results -> {
                    if (query == null || result == null) {
                        // Form no longer valid (e.g. restored state): go back to the form.
                        LaunchedEffect(Unit) { nav.pop() }
                    } else {
                        ResultsScreen(
                            query = query,
                            result = result,
                            favorites = favorites,
                            onBack = { nav.pop() },
                            onSurprise = {
                                surpriseId = SurprisePicker.pick(result.surprisePool)?.id
                                if (surpriseId != null) nav.push(Route.Surprise)
                            },
                            onChangeBudget = {
                                focusBudget = true
                                nav.pop()
                            },
                        )
                    }
                }

                Route.Surprise -> {
                    val pool = result?.surprisePool.orEmpty()
                    val gift = pool.firstOrNull { it.id == surpriseId }
                    if (gift == null) {
                        LaunchedEffect(Unit) { nav.pop() }
                    } else {
                        SurpriseScreen(
                            gift = gift,
                            mayExceedBudget = result?.affordable.isNullOrEmpty(),
                            isFavorite = favorites.isFavorite(gift.id),
                            onToggleFavorite = { favorites.toggle(gift.id) },
                            onTryAgain = { surpriseId = SurprisePicker.pick(pool, surpriseId)?.id },
                            onBackToResults = { nav.pop() },
                        )
                    }
                }

                Route.Favorites -> FavoritesScreen(gifts, favorites, onBack = { nav.pop() })
            }
        }
    }
}
