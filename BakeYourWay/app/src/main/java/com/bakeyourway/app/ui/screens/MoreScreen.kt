package com.bakeyourway.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bakeyourway.app.ui.components.BakeTopBar
import com.bakeyourway.app.ui.components.CenteredContent
import com.bakeyourway.app.ui.components.SoftCard
import com.bakeyourway.core.RecipeCalculator
import com.bakeyourway.core.UnitSystem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoreScreen(
    recipeCount: Int,
    unitSystem: UnitSystem,
    hasRecents: Boolean,
    onUnitSystem: (UnitSystem) -> Unit,
    onClearRecents: () -> Unit,
) {
    CenteredContent {
        BakeTopBar(title = "More", onBack = null)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SoftCard {
                Text("Preferred measurements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Used as the starting choice when you customize a recipe. You can always switch while you bake.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UnitSystem.entries.forEach { system ->
                        FilterChip(
                            selected = unitSystem == system,
                            onClick = { onUnitSystem(system) },
                            label = { Text(system.displayName) },
                            colors = chipColors(),
                        )
                    }
                }
            }

            SoftCard {
                Text("About Bake Your Way", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Version 1.0 · $recipeCount recipes")
                Text(
                    "Choose what you want to bake, tell us how much flour you want to use, pick how sweet you like it, " +
                        "add optional toppings and choose your cooking method. The recipe is calculated for you.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SoftCard {
                Text("Private and offline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Everything works without internet. There is no account, no login, no ads and no tracking. " +
                        "The app does not ask for the internet permission. Your favorites and recent recipes are stored only on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SoftCard {
                Text("Good to know", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(RecipeCalculator.RESULTS_VARY_NOTE, style = MaterialTheme.typography.bodyMedium)
                Text(RecipeCalculator.COOKING_DISCLAIMER, style = MaterialTheme.typography.bodyMedium)
                Text(RecipeCalculator.ALLERGEN_DISCLAIMER, style = MaterialTheme.typography.bodyMedium)
            }

            if (hasRecents) {
                OutlinedButton(
                    onClick = onClearRecents,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Text("Clear recently viewed", modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}
