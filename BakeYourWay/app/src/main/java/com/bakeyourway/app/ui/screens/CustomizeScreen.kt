package com.bakeyourway.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bakeyourway.app.ui.components.BakeTopBar
import com.bakeyourway.app.ui.components.CenteredContent
import com.bakeyourway.app.ui.components.FoodArt
import com.bakeyourway.app.ui.components.Pill
import com.bakeyourway.app.ui.components.PrimaryButton
import com.bakeyourway.app.ui.components.SoftCard
import com.bakeyourway.app.ui.theme.Butter
import com.bakeyourway.app.ui.theme.ButterDark
import com.bakeyourway.core.FlourInput
import com.bakeyourway.core.MeasureFormatter
import com.bakeyourway.core.Recipe
import com.bakeyourway.core.RecipeCatalog
import com.bakeyourway.core.SweetnessType
import com.bakeyourway.core.UnitSystem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomizeScreen(
    recipe: Recipe,
    catalog: RecipeCatalog,
    defaultUnits: UnitSystem,
    onBack: () -> Unit,
    onViewed: (String) -> Unit,
    onNext: (flourCups: Double, units: UnitSystem, sweetness: SweetnessType, addIns: Set<String>) -> Unit,
) {
    LaunchedEffect(recipe.id) { onViewed(recipe.id) }

    val gramsPerCup = catalog.ingredient(recipe.flourIngredient).gramsPerCup ?: 120.0

    // All choices survive rotation and process death.
    var unitName by rememberSaveable { mutableStateOf(defaultUnits.name) }
    var quickCups by rememberSaveable { mutableStateOf(recipe.baseFlourAmount) }
    var useCustom by rememberSaveable { mutableStateOf(false) }
    var customText by rememberSaveable { mutableStateOf("") }
    var sweetnessName by rememberSaveable { mutableStateOf(SweetnessType.NORMAL_SUGAR.name) }
    var addInsCsv by rememberSaveable {
        mutableStateOf(recipe.addIns.filter { it.default }.joinToString(",") { it.id })
    }

    val units = UnitSystem.valueOf(unitName)
    val sweetness = SweetnessType.valueOf(sweetnessName)
    val selectedAddIns = addInsCsv.split(',').filter { it.isNotBlank() }.toSet()

    val validation: FlourInput.Result = if (useCustom) {
        if (customText.isBlank()) FlourInput.Result.Error("") else FlourInput.validate(customText, units, gramsPerCup)
    } else {
        FlourInput.validateCups(quickCups, units, gramsPerCup)
    }
    val flourCups = (validation as? FlourInput.Result.Ok)?.cups

    CenteredContent {
        BakeTopBar(title = "Customize Your Recipe", onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Recipe header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FoodArt(recipe.category, imageKey = recipe.imageKey, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)))
                Column(Modifier.weight(1f)) {
                    Text(recipe.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        recipe.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 1. Flour
            SectionTitle("1. How much flour do you want to use?")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                UnitSystem.entries.forEach { system ->
                    FilterChip(
                        selected = units == system,
                        onClick = {
                            if (units != system) {
                                unitName = system.name
                                customText = ""
                            }
                        },
                        label = { Text(system.displayName) },
                        colors = chipColors(),
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlourInput.QUICK_CUPS.forEach { cups ->
                    FilterChip(
                        selected = !useCustom && quickCups == cups,
                        onClick = {
                            useCustom = false
                            quickCups = cups
                        },
                        label = { Text(FlourInput.label(cups, units, gramsPerCup), fontWeight = FontWeight.SemiBold) },
                        colors = chipColors(),
                    )
                }
                FilterChip(
                    selected = useCustom,
                    onClick = { useCustom = true },
                    label = { Text("Custom", fontWeight = FontWeight.SemiBold) },
                    colors = chipColors(),
                )
            }
            if (useCustom) {
                val error = (validation as? FlourInput.Result.Error)?.message.orEmpty()
                val warning = (validation as? FlourInput.Result.Ok)?.warning
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it.take(8) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (units == UnitSystem.US_CUPS) "Flour amount (cups)" else "Flour amount (grams)") },
                    placeholder = { Text(if (units == UnitSystem.US_CUPS) "e.g. 1.25 or 2.5" else "e.g. 150 or 300") },
                    singleLine = true,
                    isError = error.isNotEmpty() && customText.isNotBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(18.dp),
                    supportingText = {
                        when {
                            error.isNotEmpty() && customText.isNotBlank() -> Text(error)
                            warning != null -> Text(warning)
                            else -> Text("Decimals are fine: 1.25, 1.75, 2.5 ...")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                    ),
                )
            } else {
                val warning = (validation as? FlourInput.Result.Ok)?.warning
                if (warning != null) Notice(warning)
            }
            if (flourCups != null) {
                val factor = flourCups / recipe.baseFlourAmount
                Text(
                    "Your recipe will use ${FlourInput.label(flourCups, UnitSystem.US_CUPS, gramsPerCup)} " +
                        "(${FlourInput.label(flourCups, UnitSystem.METRIC, gramsPerCup)}) of flour - " +
                        "${MeasureFormatter.trimNumber(factor)}× the base recipe. All ingredients scale automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 2. Sweetness
            SectionTitle("2. Choose sweetness level")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SweetnessCard(
                    type = SweetnessType.LESS_SUGAR,
                    selected = sweetness == SweetnessType.LESS_SUGAR,
                    onClick = { sweetnessName = SweetnessType.LESS_SUGAR.name },
                    modifier = Modifier.weight(1f),
                )
                SweetnessCard(
                    type = SweetnessType.NORMAL_SUGAR,
                    selected = sweetness == SweetnessType.NORMAL_SUGAR,
                    onClick = { sweetnessName = SweetnessType.NORMAL_SUGAR.name },
                    modifier = Modifier.weight(1f),
                )
            }
            val reduction = Math.round((1 - recipe.lessSugarMultiplier / recipe.normalSugarMultiplier) * 100).toInt()
            Text(
                if (sweetness == SweetnessType.LESS_SUGAR) {
                    "Less Sugar uses about $reduction% less sugar in this recipe. ${recipe.lessSugarNote.orEmpty()}"
                } else {
                    "Normal Sugar uses the traditional amount of sugar for this recipe."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 3. Add-ins
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("3. Optional add-ins / toppings", Modifier.weight(1f, fill = false))
                Pill("OPTIONAL")
            }
            if (recipe.addIns.isEmpty()) {
                Text("This recipe has no add-ins.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    "Choose any you like - the recipe works just as well with none.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recipe.addIns.mapNotNull { catalog.addIns[it.id] }.forEach { def ->
                        val isOn = def.id in selectedAddIns
                        FilterChip(
                            selected = isOn,
                            onClick = {
                                val next = if (isOn) selectedAddIns - def.id else selectedAddIns + def.id
                                addInsCsv = next.joinToString(",")
                            },
                            leadingIcon = { Text(def.emoji.orEmpty()) },
                            label = { Text(def.name) },
                            colors = chipColors(),
                        )
                    }
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            PrimaryButton(
                text = "Next",
                enabled = flourCups != null,
                onClick = { if (flourCups != null) onNext(flourCups, units, sweetness, selectedAddIns) },
            )
        }
    }
}

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
internal fun Notice(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Butter),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(text, color = ButterDark, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun SweetnessCard(type: SweetnessType, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) primary else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, if (selected) primary else MaterialTheme.colorScheme.outline),
    ) {
        Column(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                type.displayName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "(${type.subtitle.lowercase()})",
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
