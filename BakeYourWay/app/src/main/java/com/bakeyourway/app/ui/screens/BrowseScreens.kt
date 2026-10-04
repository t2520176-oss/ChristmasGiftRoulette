package com.bakeyourway.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bakeyourway.app.ui.components.BakeTopBar
import com.bakeyourway.app.ui.components.CenteredContent
import com.bakeyourway.app.ui.components.SoftCard
import com.bakeyourway.core.Recipe
import com.bakeyourway.core.RecipeCatalog
import com.bakeyourway.core.RecipeCategory

/** Recipes of one category (opened from the Home grid). */
@Composable
fun CategoryScreen(
    category: RecipeCategory,
    catalog: RecipeCatalog,
    favorites: Set<String>,
    onBack: () -> Unit,
    onRecipe: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
) {
    val recipes = remember(category, catalog) { catalog.recipesIn(category) }
    CenteredContent(maxWidth = 1000.dp) {
        BakeTopBar(title = category.displayName, onBack = onBack)
        RecipeGrid(
            recipes = recipes,
            favorites = favorites,
            onOpen = onRecipe,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            header = {
                fullWidthItem {
                    Text(
                        "${recipes.size} recipes - ${category.tagline}. Pick one to customize.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            },
        )
    }
}

/** The "Recipes" tab: every recipe, searchable and filterable by category. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRecipesScreen(
    catalog: RecipeCatalog,
    favorites: Set<String>,
    onRecipe: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf("") }
    val selected = RecipeCategory.entries.firstOrNull { it.name == categoryName }
    val recipes = remember(query, selected, catalog) {
        catalog.search(query).filter { selected == null || it.category == selected }
    }

    CenteredContent(maxWidth = 1000.dp) {
        BakeTopBar(title = "Recipes", onBack = null)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchField(query = query, onQueryChange = { query = it })
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                item {
                    FilterChip(
                        selected = selected == null,
                        onClick = { categoryName = "" },
                        label = { Text("All") },
                        colors = chipColors(),
                    )
                }
                items(RecipeCategory.entries.toList(), key = { it.name }) { c ->
                    FilterChip(
                        selected = selected == c,
                        onClick = { categoryName = c.name },
                        label = { Text(c.displayName) },
                        colors = chipColors(),
                    )
                }
            }
        }
        if (recipes.isEmpty()) {
            Text(
                "No recipes match your search.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        } else {
            RecipeGrid(
                recipes = recipes,
                favorites = favorites,
                onOpen = onRecipe,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = androidx.compose.ui.graphics.Color.White,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.primary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
)

@Composable
fun FavoritesScreen(
    catalog: RecipeCatalog,
    favorites: Set<String>,
    recents: List<String>,
    onRecipe: (Recipe) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
) {
    val favoriteRecipes = remember(favorites, catalog) {
        catalog.recipes.filter { it.id in favorites }
    }
    val recentRecipes = remember(recents, catalog) { recents.mapNotNull { catalog.recipe(it) } }

    CenteredContent(maxWidth = 1000.dp) {
        BakeTopBar(title = "Favorites", onBack = null)
        RecipeGrid(
            recipes = favoriteRecipes,
            favorites = favorites,
            onOpen = onRecipe,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            header = {
                if (favoriteRecipes.isEmpty()) {
                    fullWidthItem {
                        SoftCard {
                            Text("No favorites yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Tap the heart on any recipe to save it here. Favorites are stored on this device only - " +
                                    "no account or internet needed.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            footer = {
                if (recentRecipes.isNotEmpty()) {
                    fullWidthItem {
                        Text(
                            "Recently viewed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    gridItems(recentRecipes, key = { "recent_${it.id}" }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            isFavorite = recipe.id in favorites,
                            onOpen = { onRecipe(recipe) },
                            onToggleFavorite = { onToggleFavorite(recipe) },
                        )
                    }
                }
            },
        )
    }
}
