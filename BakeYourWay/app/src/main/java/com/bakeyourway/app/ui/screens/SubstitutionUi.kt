package com.bakeyourway.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bakeyourway.app.ui.components.Pill
import com.bakeyourway.app.ui.components.PrimaryButton
import com.bakeyourway.app.ui.components.SoftCard
import com.bakeyourway.app.ui.theme.Butter
import com.bakeyourway.app.ui.theme.ButterDark
import com.bakeyourway.app.ui.theme.CaramelSoft
import com.bakeyourway.app.ui.theme.CoralSoft
import com.bakeyourway.app.ui.theme.Sage
import com.bakeyourway.app.ui.theme.SageSoft
import com.bakeyourway.core.CalculatedRecipe
import com.bakeyourway.core.Conclusion
import com.bakeyourway.core.MissingIngredient
import com.bakeyourway.core.MissingLookup
import com.bakeyourway.core.RecipeCatalog
import com.bakeyourway.core.SubstituteConfidence
import com.bakeyourway.core.SubstitutionChoice
import com.bakeyourway.core.SubstitutionEngine
import com.bakeyourway.core.SubstitutionOption
import com.bakeyourway.core.SubstitutionOptions

/**
 * Everything the "Missing an Ingredient?" feature needs from the result screen. The calculation is done by
 * the offline [SubstitutionEngine]; this only holds the user's choices.
 */
internal class SubstitutionUiState(
    /** The recipe at the current flour amount, before any substitution (what the lookups work from). */
    val baseline: CalculatedRecipe,
    /** The recipe with the accepted substitutions applied. */
    val calc: CalculatedRecipe,
    val catalog: RecipeCatalog,
    val choices: List<SubstitutionChoice>,
    /** Ingredient ids the user said they do not have and is still looking at. */
    val missingIds: List<String>,
    val onChoices: (List<SubstitutionChoice>) -> Unit,
    val onMissingIds: (List<String>) -> Unit,
) {
    fun markMissing(ingredientId: String) {
        if (ingredientId !in missingIds) onMissingIds(missingIds + ingredientId)
    }
}

/** Substitution choices are kept in a single string so they survive rotation: "butter~butter_oil_batter|egg~egg_flax_batter". */
internal fun encodeChoices(choices: List<SubstitutionChoice>): String =
    choices.joinToString("|") { "${it.originalIngredient}~${it.substitutionId}" }

internal fun decodeChoices(text: String): List<SubstitutionChoice> =
    text.split("|").mapNotNull { part ->
        val bits = part.split("~")
        if (bits.size == 2 && bits[0].isNotBlank() && bits[1].isNotBlank()) SubstitutionChoice(bits[0], bits[1]) else null
    }

internal fun encodeIds(ids: List<String>): String = ids.joinToString(",")

internal fun decodeIds(text: String): List<String> = text.split(",").filter { it.isNotBlank() }

// ----------------------------------------------------------------------------------------------
// "Missing an Ingredient?" card
// ----------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun MissingIngredientCard(state: SubstitutionUiState) {
    var query by rememberSaveable { mutableStateOf("") }
    var notFound by remember { mutableStateOf(false) }
    var ambiguous by remember { mutableStateOf<List<MissingIngredient>>(emptyList()) }
    // Which of the possible substitutes is currently shown for each missing ingredient.
    val shown = remember { mutableStateMapOf<String, Int>() }

    val applied = state.calc.appliedSubstitutions
    val missing = state.missingIds.mapNotNull { SubstitutionEngine.describe(state.baseline, state.catalog, it) }

    /** Looks the typed text up in THIS recipe; returns true when the text was accepted or empty. */
    fun addFromQuery(): Boolean {
        notFound = false
        ambiguous = emptyList()
        return when (val found = SubstitutionEngine.lookup(query, state.baseline, state.catalog)) {
            MissingLookup.Blank -> true
            is MissingLookup.NotInRecipe -> {
                notFound = true
                false
            }
            is MissingLookup.Found -> {
                state.markMissing(found.ingredient.ingredientId)
                query = ""
                true
            }
            is MissingLookup.Ambiguous -> {
                ambiguous = found.choices
                false
            }
        }
    }

    SoftCard(container = CaramelSoft, border = null) {
        Text("Missing an Ingredient?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Tell us what you don't have and we'll check for suitable substitutes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it.take(40)
                    notFound = false
                    ambiguous = emptyList()
                },
                modifier = Modifier.weight(1f),
                label = { Text("Ingredient you don't have") },
                placeholder = { Text("e.g. butter, milk, eggs") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
            )
            OutlinedButton(
                onClick = { addFromQuery() },
                enabled = query.isNotBlank(),
                shape = RoundedCornerShape(24.dp),
            ) { Text("Add", fontWeight = FontWeight.Bold) }
        }
        if (notFound) Notice(SubstitutionEngine.NOT_IN_RECIPE)
        if (ambiguous.isNotEmpty()) {
            Text("Which one do you mean?", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ambiguous.forEach { option ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            state.markMissing(option.ingredientId)
                            query = ""
                            ambiguous = emptyList()
                        },
                        label = { Text(option.name) },
                        colors = chipColors(),
                    )
                }
            }
        }
        if (missing.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                missing.forEach { m ->
                    FilterChip(
                        selected = true,
                        onClick = { state.onMissingIds(state.missingIds - m.ingredientId) },
                        label = { Text("${m.name} ×") },
                        colors = chipColors(),
                    )
                }
            }
        }
        val searching = missing.isNotEmpty()
        PrimaryButton(
            text = if (missing.size > 1) "FIND SUBSTITUTES" else "FIND SUBSTITUTE",
            onClick = { addFromQuery() },
            enabled = query.isNotBlank() || searching,
        )
        if (applied.isNotEmpty()) {
            AppliedSubstitutions(state)
        }
    }

    if (missing.isNotEmpty()) {
        SubstitutionEngine.combinationWarning(missing)?.let { Notice("⚠ $it") }
        val allIds = (state.missingIds + state.choices.map { it.originalIngredient }).toSet()
        missing.forEach { m ->
            key(m.ingredientId) {
                val result = remember(state.baseline, allIds, m.ingredientId) {
                    SubstitutionEngine.options(state.baseline, state.catalog, m, allIds)
                }
                SubstituteResultCard(
                    result = result,
                    index = shown[m.ingredientId] ?: 0,
                    onUse = { option ->
                        state.onChoices(
                            state.choices.filter { it.originalIngredient != m.ingredientId } +
                                SubstitutionChoice(m.ingredientId, option.rule.id),
                        )
                        state.onMissingIds(state.missingIds - m.ingredientId)
                        shown.remove(m.ingredientId)
                    },
                    onTryAnother = { shown[m.ingredientId] = (shown[m.ingredientId] ?: 0) + 1 },
                    onCancel = {
                        state.onMissingIds(state.missingIds - m.ingredientId)
                        shown.remove(m.ingredientId)
                    },
                )
            }
        }
    }
}

@Composable
private fun AppliedSubstitutions(state: SubstitutionUiState) {
    Text("Substitutions in your recipe", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
    state.calc.appliedSubstitutions.forEach { applied ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("↻ ${applied.summary}", fontWeight = FontWeight.SemiBold)
                if (!applied.rule.isOmission) {
                    Text(applied.useText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = {
                state.onChoices(state.choices.filter { it.originalIngredient != applied.originalIngredient })
            }) { Text("Undo", fontWeight = FontWeight.Bold) }
        }
    }
}

// ----------------------------------------------------------------------------------------------
// One missing ingredient -> its best substitute
// ----------------------------------------------------------------------------------------------

@Composable
private fun SubstituteResultCard(
    result: SubstitutionOptions,
    index: Int,
    onUse: (SubstitutionOption) -> Unit,
    onTryAnother: () -> Unit,
    onCancel: () -> Unit,
) {
    val missing = result.missing
    SoftCard {
        SmallLabel("MISSING INGREDIENT")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(missing.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f, fill = false))
            Pill(missing.importance.label.uppercase())
        }
        Text("Your recipe needs ${missing.originalText}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        result.structuralWarning?.let { Notice("⚠ $it") }

        if (result.options.isEmpty()) {
            Notice(result.message ?: SubstitutionEngine.NO_RELIABLE_SUBSTITUTE)
            result.notRecommended.filter { it.substituteName.isNotBlank() }.forEach { NotRecommended(it.substituteName, it.reason) }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("CANCEL", fontWeight = FontWeight.Bold) }
            return@SoftCard
        }

        val current = index % result.options.size
        val option = result.options[current]
        val rule = option.rule

        SmallLabel("RECOMMENDED SUBSTITUTE")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(rule.substituteName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f, fill = false))
            RatingPill(option)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CoralSoft),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                SmallLabel("USE")
                Text(option.useText, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }
        }
        Text(option.suitability, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        option.liquidAdjustment?.let { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        option.warnings.forEach { Notice("⚠ $it") }

        SmallLabel("WHAT WILL CHANGE")
        ChangeLine("Texture", rule.effectOnTexture)
        ChangeLine("Flavor", rule.effectOnFlavor)
        ChangeLine("Browning", rule.effectOnBrowning)

        rule.specialInstructions?.let {
            SmallLabel("HOW TO USE IT")
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        rule.tip?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Butter),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Text("TIP: $it", color = ButterDark, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
            }
        }
        result.notRecommended.filter { it.substituteName.isNotBlank() }.forEach { NotRecommended(it.substituteName, it.reason) }

        PrimaryButton("USE THIS SUBSTITUTE", onClick = { onUse(option) })
        if (result.options.size > 1) {
            OutlinedButton(
                onClick = onTryAnother,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text("TRY ANOTHER SUBSTITUTE (${current + 1} of ${result.options.size})", fontWeight = FontWeight.Bold)
            }
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("CANCEL", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SmallLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ChangeLine(label: String, text: String?) {
    if (text.isNullOrBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NotRecommended(name: String, reason: String) {
    Text(
        "Not recommended: $name. $reason",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RatingPill(option: SubstitutionOption) {
    val (container, content) = when (option.confidence) {
        SubstituteConfidence.RECOMMENDED -> SageSoft to Sage
        SubstituteConfidence.ACCEPTABLE -> Butter to ButterDark
        else -> CoralSoft to MaterialTheme.colorScheme.primary
    }
    Pill(option.rating, container = container, content = content)
}

// ----------------------------------------------------------------------------------------------
// Conclusion: summary of the recipe including the substitutions
// ----------------------------------------------------------------------------------------------

@Composable
internal fun SubstitutionNotes(conclusion: Conclusion) {
    if (conclusion.substitutionSummary.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SoftCard {
            Text("YOUR RECIPE SUMMARY", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            conclusion.recipeSummary.forEach { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(line.label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        line.value,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 12.dp).weight(1f),
                    )
                }
            }
        }
        conclusion.substitutionExpect?.let { expect ->
            SoftCard(container = SageSoft, border = null) {
                Text("WHAT TO EXPECT", fontWeight = FontWeight.ExtraBold, color = Sage, style = MaterialTheme.typography.titleMedium)
                Text(expect, style = MaterialTheme.typography.bodyMedium)
            }
        }
        conclusion.substitutionImprovement?.let { improvement ->
            SoftCard(container = Butter, border = null) {
                Text("OPTIONAL IMPROVEMENTS", fontWeight = FontWeight.ExtraBold, color = ButterDark, style = MaterialTheme.typography.titleMedium)
                Text(improvement, color = ButterDark, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (conclusion.labelChecks.isNotEmpty()) {
            SoftCard {
                Text("Check the label", fontWeight = FontWeight.Bold)
                conclusion.labelChecks.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
