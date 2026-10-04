package com.bakeyourway.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakeyourway.app.ui.components.BakeTopBar
import com.bakeyourway.app.ui.components.CenteredContent
import com.bakeyourway.app.ui.components.PrimaryButton
import com.bakeyourway.app.ui.components.methodEmoji
import com.bakeyourway.app.ui.theme.Butter
import com.bakeyourway.app.ui.theme.ButterDark
import com.bakeyourway.core.CookingMethod
import com.bakeyourway.core.Recipe
import com.bakeyourway.core.RecipeCalculator
import kotlinx.coroutines.launch

@Composable
fun MethodScreen(
    recipe: Recipe,
    summary: String,
    onBack: () -> Unit,
    onShowRecipe: (CookingMethod) -> Unit,
) {
    val supported = recipe.supportedCookingMethods
    var selectedName by rememberSaveable { mutableStateOf(recipe.recommendedMethod?.name.orEmpty()) }
    val selected = CookingMethod.entries.firstOrNull { it.name == selectedName && it in supported }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        CenteredContent {
            BakeTopBar(title = "Choose Cooking Method", onBack = onBack)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "${recipe.name} · $summary",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CookingMethod.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { method ->
                            MethodCard(
                                method = method,
                                supported = method in supported,
                                selected = method == selected,
                                onClick = {
                                    if (method in supported) {
                                        selectedName = method.name
                                    } else {
                                        scope.launch {
                                            snackbar.currentSnackbarData?.dismiss()
                                            snackbar.showSnackbar(RecipeCalculator.UNSUPPORTED_METHOD_MESSAGE)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                MethodInfoCard(recipe, selected)
            }
            Column(Modifier.padding(16.dp)) {
                PrimaryButton(
                    text = "Show Recipe",
                    enabled = selected != null,
                    onClick = { if (selected != null) onShowRecipe(selected) },
                )
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }
}

@Composable
private fun MethodCard(
    method: CookingMethod,
    supported: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val container = when {
        !supported -> MaterialTheme.colorScheme.surfaceVariant
        selected -> primary
        else -> Color.White
    }
    val content = when {
        !supported -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        selected -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        modifier = modifier.aspectRatio(1.05f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, if (selected) primary else MaterialTheme.colorScheme.outline),
    ) {
        Column(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                methodEmoji(method),
                fontSize = 44.sp,
                color = Color.Unspecified,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                method.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = content,
                textAlign = TextAlign.Center,
            )
            if (!supported) {
                Text(
                    "Not recommended",
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun MethodInfoCard(recipe: Recipe, selected: CookingMethod?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Butter),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = ButterDark)
                Text("Cooking Method Info", fontWeight = FontWeight.Bold, color = ButterDark)
            }
            val profile = selected?.let { recipe.profileFor(it) }
            if (profile != null) {
                val temp = RecipeCalculator.temperatureText(profile)
                Text(
                    "${selected?.displayName.orEmpty()}: ${if (temp.isNotEmpty()) "$temp, " else ""}about ${RecipeCalculator.timeText(profile)} per batch.",
                    fontWeight = FontWeight.SemiBold,
                    color = ButterDark,
                )
            }
            Text(
                "Cooking time and temperature depend on your selected method - they are not just multiplied by the flour amount. " +
                    "Some recipes are not suitable for every method, so those are greyed out.",
                color = ButterDark,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
