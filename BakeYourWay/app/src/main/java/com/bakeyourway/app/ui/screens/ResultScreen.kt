package com.bakeyourway.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakeyourway.app.ui.components.BakeTopBar
import com.bakeyourway.app.ui.components.CenteredContent
import com.bakeyourway.app.ui.components.FavoriteButton
import com.bakeyourway.app.ui.components.FoodArt
import com.bakeyourway.app.ui.components.NumberBadge
import com.bakeyourway.app.ui.components.Pill
import com.bakeyourway.app.ui.components.SoftCard
import com.bakeyourway.app.ui.components.methodEmoji
import com.bakeyourway.app.ui.theme.Butter
import com.bakeyourway.app.ui.theme.ButterDark
import com.bakeyourway.app.ui.theme.CaramelSoft
import com.bakeyourway.app.ui.theme.CoralSoft
import com.bakeyourway.app.ui.theme.Sage
import com.bakeyourway.app.ui.theme.SageSoft
import com.bakeyourway.core.CalculatedIngredient
import com.bakeyourway.core.CalculatedRecipe
import com.bakeyourway.core.CookingInfo
import com.bakeyourway.core.CookingMethod
import com.bakeyourway.core.Recipe
import com.bakeyourway.core.RecipeCalculator
import com.bakeyourway.core.RecipeCatalog
import com.bakeyourway.core.RecipeSelection
import com.bakeyourway.core.SweetnessType
import com.bakeyourway.core.UnitSystem
import kotlinx.coroutines.launch

private val TAB_TITLES = listOf("Ingredients", "Instructions", "Tips", "Notes")
private const val TABS_ITEM_INDEX = 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(
    recipe: Recipe,
    catalog: RecipeCatalog,
    flourCups: Double,
    sweetness: SweetnessType,
    method: CookingMethod,
    addIns: Set<String>,
    initialUnits: UnitSystem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onViewed: (String) -> Unit,
) {
    var unitName by rememberSaveable { mutableStateOf(initialUnits.name) }
    var tab by rememberSaveable { mutableStateOf(0) }
    val units = UnitSystem.valueOf(unitName)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(recipe.id) { onViewed(recipe.id) }

    val calc: CalculatedRecipe? = remember(recipe, flourCups, sweetness, method, addIns, units) {
        runCatching {
            RecipeCalculator.calculate(recipe, catalog, RecipeSelection(flourCups, sweetness, method, addIns, units))
        }.getOrNull()
    }

    if (calc == null) {
        CenteredContent {
            BakeTopBar(title = recipe.name, onBack = onBack)
            Notice("Sorry, this combination could not be calculated. Please go back and pick a different cooking method.", Modifier.padding(16.dp))
        }
        return
    }

    CenteredContent(maxWidth = 820.dp) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // 0: photo / placeholder with back + favorite
            item {
                Box(
                    Modifier.fillMaxWidth().height(230.dp)
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
                ) {
                    FoodArt(recipe.category, imageKey = recipe.imageKey, modifier = Modifier.fillMaxSize())
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        RoundButton {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                        RoundButton { FavoriteButton(isFavorite = isFavorite, onToggle = onToggleFavorite) }
                    }
                }
            }
            // 1: title + summary
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(recipe.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    SoftCard {
                        SummaryRow("Using", "${calc.flourText} flour")
                        SummaryRow("Sweetness", sweetness.displayName)
                        SummaryRow("Cooking Method", method.displayName)
                        SummaryRow("Estimated Yield", calc.yieldEstimate.text)
                        calc.yieldEstimate.note?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            // 2: warnings (always an item so the indices stay stable)
            item {
                if (calc.warnings.isNotEmpty()) {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        calc.warnings.forEach { Notice("⚠ $it") }
                    }
                }
            }
            // 3: tabs
            item {
                ScrollableTabRow(
                    selectedTabIndex = tab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 8.dp,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    TAB_TITLES.forEachIndexed { index, title ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
                            text = { Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            // 4: selected tab
            item {
                when (tab) {
                    0 -> IngredientsTab(calc, units, onUnits = { unitName = it.name })
                    1 -> InstructionsTab(calc, onSeeNotes = {
                        tab = 3
                        scope.launch { listState.animateScrollToItem(TABS_ITEM_INDEX) }
                    })
                    2 -> TipsTab(calc)
                    else -> NotesTab(calc)
                }
            }
        }
    }
}

@Composable
private fun RoundButton(content: @Composable () -> Unit) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.92f)) { content() }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(value, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Bullets(items: List<String>, marker: String = "•", markerColor: Color = MaterialTheme.colorScheme.primary) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(marker, color = markerColor, fontWeight = FontWeight.Bold)
                Text(item, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ----------------------------------------------------------------------------------------------
// Ingredients
// ----------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun IngredientsTab(calc: CalculatedRecipe, units: UnitSystem, onUnits: (UnitSystem) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            UnitSystem.entries.forEach { system ->
                FilterChip(
                    selected = units == system,
                    onClick = { onUnits(system) },
                    label = { Text(system.displayName) },
                    colors = chipColors(),
                )
            }
        }
        calc.panGuidance?.let { pan ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CaramelSoft),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    "Recommended pan for your amount: $pan",
                    modifier = Modifier.padding(14.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        calc.batchInfo?.let { Notice(it.note) }

        SoftCard {
            val groups = calc.ingredients.groupBy { it.group.orEmpty() }
            groups.entries.forEachIndexed { groupIndex, (group, items) ->
                if (group.isNotEmpty()) {
                    Text(
                        group,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = if (groupIndex == 0) 0.dp else 8.dp),
                    )
                }
                items.forEachIndexed { i, ingredient ->
                    IngredientRow(ingredient, lessSugar = calc.selection.sweetness == SweetnessType.LESS_SUGAR)
                    if (i < items.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        if (calc.addInIngredients.isNotEmpty()) {
            SoftCard(container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Your optional add-ins", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Pill("OPTIONAL")
                }
                calc.addInIngredients.forEachIndexed { i, ingredient ->
                    IngredientRow(ingredient, lessSugar = false, showAddInLabel = true)
                    if (i < calc.addInIngredients.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        if (calc.allergens.isNotEmpty()) {
            SoftCard(container = Butter, border = null) {
                Text("Allergen information", fontWeight = FontWeight.Bold, color = ButterDark)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    calc.conclusion.allergenNotices.forEach { Pill(it, container = Color.White, content = ButterDark) }
                }
                Text(calc.conclusion.allergenDisclaimer, style = MaterialTheme.typography.bodySmall, color = ButterDark)
            }
        }
    }
}

@Composable
private fun IngredientRow(ingredient: CalculatedIngredient, lessSugar: Boolean, showAddInLabel: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(ingredient.name, style = MaterialTheme.typography.bodyLarge)
            if (showAddInLabel && ingredient.note != null) {
                Text("for ${ingredient.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (lessSugar && ingredient.isSugar) {
                Text("reduced for Less Sugar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            ingredient.displayText,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp).widthIn(max = 200.dp),
        )
    }
}

// ----------------------------------------------------------------------------------------------
// Instructions + cooking time & temperature
// ----------------------------------------------------------------------------------------------

@Composable
private fun InstructionsTab(calc: CalculatedRecipe, onSeeNotes: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        calc.steps.forEach { step ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                NumberBadge(step.number)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(step.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(step.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Text("Cooking Time & Temperature", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        calc.methodComparison.forEach { MethodCompareCard(it) }
        calc.batchInfo?.let { Notice(it.note) }
        Text(
            calc.cookingDisclaimer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onSeeNotes,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
        ) {
            Text("See Conclusion & Notes", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

@Composable
private fun MethodCompareCard(info: CookingInfo) {
    val selected = info.isSelected
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) CoralSoft else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) primary else MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Text(methodEmoji(info.method), fontSize = 30.sp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(info.method.displayName.uppercase(), fontWeight = FontWeight.ExtraBold, color = if (selected) primary else MaterialTheme.colorScheme.onSurface)
                    if (selected) Pill("Your choice")
                }
                if (info.temperatureText.isNotEmpty()) {
                    Text(info.temperatureText, fontWeight = FontWeight.SemiBold)
                }
                Text("${info.timeText} per batch", fontWeight = FontWeight.SemiBold)
                Text(
                    if (info.preheat) "Preheat first" else if (info.method == CookingMethod.STEAM) "Keep a steady medium steam" else "No preheat needed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selected) {
                    info.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------------------
// Tips + topping variations
// ----------------------------------------------------------------------------------------------

@Composable
private fun TipsTab(calc: CalculatedRecipe) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (calc.tips.isNotEmpty()) {
            SoftCard {
                Text("Baker's tips", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Bullets(calc.tips)
            }
        }
        if (calc.methodTips.isNotEmpty()) {
            SoftCard {
                Text("Tips for ${calc.selection.method.displayName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Bullets(calc.methodTips)
            }
        }
        Text("Topping Variations (Optional)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Ideas to try - these are suggestions, not required ingredients.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val columns = if (LocalConfiguration.current.screenWidthDp >= 600) 3 else 2
        calc.toppingSuggestions.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { topping ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(topping.emoji.orEmpty(), fontSize = 30.sp)
                            Text(topping.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                            Text(
                                topping.description,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ----------------------------------------------------------------------------------------------
// Conclusion & notes (personalised to the sweetness choice)
// ----------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotesTab(calc: CalculatedRecipe) {
    val c = calc.conclusion
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Conclusion & Notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        SoftCard(container = Butter, border = null) {
            Text("💡  ${c.title}", fontWeight = FontWeight.ExtraBold, color = ButterDark, style = MaterialTheme.typography.titleMedium)
            Text(c.summary, color = ButterDark)
            c.recipeNote?.let { Text(it, color = ButterDark, style = MaterialTheme.typography.bodyMedium) }
            if (c.sugarChanges.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Bullets(c.sugarChanges, markerColor = ButterDark)
            }
        }

        SoftCard(container = SageSoft, border = null) {
            Text(c.whatToExpectTitle, fontWeight = FontWeight.ExtraBold, color = Sage, style = MaterialTheme.typography.titleMedium)
            Bullets(c.whatToExpect, marker = "✓", markerColor = Sage)
        }

        if (c.naturalSweetnessTitle != null && c.naturalSweetnessIdeas.isNotEmpty()) {
            SoftCard(container = CaramelSoft, border = null) {
                Text(c.naturalSweetnessTitle!!, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleMedium)
                Bullets(c.naturalSweetnessIdeas, marker = "✓", markerColor = MaterialTheme.colorScheme.secondary)
                c.closing?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }

        SoftCard(container = CoralSoft, border = null) {
            Text("Important Notes", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            Bullets(c.importantNotes, marker = "!", markerColor = MaterialTheme.colorScheme.primary)
        }

        if (c.allergenNotices.isNotEmpty()) {
            SoftCard {
                Text("Allergen information", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    c.allergenNotices.forEach { Pill(it, container = Butter, content = ButterDark) }
                }
                Text(c.allergenDisclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(c.generalNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
