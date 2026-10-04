package com.bakeyourway.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bakeyourway.app.ui.screens.AllRecipesScreen
import com.bakeyourway.app.ui.screens.CategoryScreen
import com.bakeyourway.app.ui.screens.CustomizeScreen
import com.bakeyourway.app.ui.screens.FavoritesScreen
import com.bakeyourway.app.ui.screens.HomeScreen
import com.bakeyourway.app.ui.screens.MethodScreen
import com.bakeyourway.app.ui.screens.MoreScreen
import com.bakeyourway.app.ui.screens.ResultScreen
import com.bakeyourway.core.CookingMethod
import com.bakeyourway.core.FlourInput
import com.bakeyourway.core.Recipe
import com.bakeyourway.core.RecipeCatalog
import com.bakeyourway.core.RecipeCategory
import com.bakeyourway.core.SweetnessType
import com.bakeyourway.core.UnitSystem

private object Routes {
    /** Route value meaning "no add-ins" - an empty query value would not match reliably. */
    const val NO_ADD_INS = "none"

    const val HOME = "home"
    const val RECIPES = "recipes"
    const val FAVORITES = "favorites"
    const val MORE = "more"
    const val CATEGORY = "category/{categoryId}"
    const val CUSTOMIZE = "customize/{recipeId}"

    // Every choice the user made travels in the route, so the recipe screen can always be rebuilt
    // (rotation, process death) from nothing but the route.
    const val OPTIONS = "flour={flour}&units={units}&sweet={sweet}&addins={addins}"
    const val METHOD = "method/{recipeId}?$OPTIONS"
    const val RESULT = "result/{recipeId}?$OPTIONS&method={method}"

    fun category(c: RecipeCategory) = "category/${c.name}"
    fun customize(recipeId: String) = "customize/$recipeId"

    fun options(flourCups: Double, units: UnitSystem, sweetness: SweetnessType, addIns: Set<String>) =
        "flour=$flourCups&units=${units.name}&sweet=${sweetness.name}&addins=${addIns.sorted().joinToString(",").ifEmpty { NO_ADD_INS }}"

    fun method(recipeId: String, options: String) = "method/$recipeId?$options"
    fun result(recipeId: String, options: String, method: CookingMethod) = "result/$recipeId?$options&method=${method.name}"
}

private val TOP_LEVEL = setOf(Routes.HOME, Routes.RECIPES, Routes.FAVORITES, Routes.MORE)

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val BOTTOM_ITEMS = listOf(
    BottomItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomItem(Routes.RECIPES, "Recipes", Icons.Filled.Menu),
    BottomItem(Routes.FAVORITES, "Favorites", Icons.Filled.Favorite),
    BottomItem(Routes.MORE, "More", Icons.Filled.MoreVert),
)

private fun optionArgs() = listOf("flour", "units", "sweet", "addins").map { name ->
    navArgument(name) {
        type = NavType.StringType
        defaultValue = ""
    }
}

@Composable
fun BakeNavHost(state: MainUiState, catalog: RecipeCatalog, viewModel: MainViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in TOP_LEVEL) {
                NavigationBar(containerColor = Color.White) {
                    BOTTOM_ITEMS.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { navController.navigateTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            val openRecipe: (Recipe) -> Unit = { navController.navigate(Routes.customize(it.id)) }
            val toggleFavorite: (Recipe) -> Unit = { viewModel.toggleFavorite(it.id) }

            composable(Routes.HOME) {
                HomeScreen(
                    catalog = catalog,
                    favorites = state.favorites,
                    recents = state.recents,
                    onCategory = { navController.navigate(Routes.category(it)) },
                    onRecipe = openRecipe,
                    onToggleFavorite = toggleFavorite,
                )
            }
            composable(Routes.RECIPES) {
                AllRecipesScreen(catalog, state.favorites, openRecipe, toggleFavorite)
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(catalog, state.favorites, state.recents, openRecipe, toggleFavorite)
            }
            composable(Routes.MORE) {
                MoreScreen(
                    recipeCount = catalog.recipes.size,
                    unitSystem = state.unitSystem,
                    hasRecents = state.recents.isNotEmpty(),
                    onUnitSystem = viewModel::setUnitSystem,
                    onClearRecents = viewModel::clearRecents,
                )
            }
            composable(
                route = Routes.CATEGORY,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            ) { entry ->
                val category = RecipeCategory.entries.firstOrNull { it.name == entry.arguments?.getString("categoryId") }
                if (category == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    CategoryScreen(
                        category = category,
                        catalog = catalog,
                        favorites = state.favorites,
                        onBack = { navController.popBackStack() },
                        onRecipe = openRecipe,
                        onToggleFavorite = toggleFavorite,
                    )
                }
            }
            composable(
                route = Routes.CUSTOMIZE,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType }),
            ) { entry ->
                val recipe = catalog.recipe(entry.arguments?.getString("recipeId").orEmpty())
                if (recipe == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    CustomizeScreen(
                        recipe = recipe,
                        catalog = catalog,
                        defaultUnits = state.unitSystem,
                        onBack = { navController.popBackStack() },
                        onViewed = viewModel::onRecipeViewed,
                        onNext = { flour, units, sweetness, addIns ->
                            navController.navigate(Routes.method(recipe.id, Routes.options(flour, units, sweetness, addIns)))
                        },
                    )
                }
            }
            composable(
                route = Routes.METHOD,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType }) + optionArgs(),
            ) { entry ->
                val recipe = catalog.recipe(entry.arguments?.getString("recipeId").orEmpty())
                val choice = entry.arguments.toChoice(catalog, recipe)
                if (recipe == null || choice == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    val flourDensity = catalog.ingredient(recipe.flourIngredient).gramsPerCup ?: 120.0
                    val summary = "${FlourInput.label(choice.flourCups, choice.units, flourDensity)} flour · ${choice.sweetness.displayName}"
                    MethodScreen(
                        recipe = recipe,
                        summary = summary,
                        onBack = { navController.popBackStack() },
                        onShowRecipe = { method ->
                            navController.navigate(
                                Routes.result(
                                    recipe.id,
                                    Routes.options(choice.flourCups, choice.units, choice.sweetness, choice.addIns),
                                    method,
                                ),
                            )
                        },
                    )
                }
            }
            composable(
                route = Routes.RESULT,
                arguments = listOf(
                    navArgument("recipeId") { type = NavType.StringType },
                    navArgument("method") { type = NavType.StringType; defaultValue = "" },
                ) + optionArgs(),
            ) { entry ->
                val recipe = catalog.recipe(entry.arguments?.getString("recipeId").orEmpty())
                val choice = entry.arguments.toChoice(catalog, recipe)
                val method = CookingMethod.entries.firstOrNull { it.name == entry.arguments?.getString("method") }
                if (recipe == null || choice == null || method == null || recipe.profileFor(method) == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    ResultScreen(
                        recipe = recipe,
                        catalog = catalog,
                        flourCups = choice.flourCups,
                        sweetness = choice.sweetness,
                        method = method,
                        addIns = choice.addIns,
                        initialUnits = choice.units,
                        isFavorite = recipe.id in state.favorites,
                        onToggleFavorite = { viewModel.toggleFavorite(recipe.id) },
                        onBack = { navController.popBackStack() },
                        onViewed = viewModel::onRecipeViewed,
                    )
                }
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private data class Choice(
    val flourCups: Double,
    val units: UnitSystem,
    val sweetness: SweetnessType,
    val addIns: Set<String>,
)

/** Rebuilds the user's choices from route arguments; null when anything is missing or invalid. */
private fun android.os.Bundle?.toChoice(catalog: RecipeCatalog, recipe: Recipe?): Choice? {
    if (this == null || recipe == null) return null
    val flour = getString("flour")?.toDoubleOrNull() ?: return null
    if (flour <= 0.0 || flour > FlourInput.MAX_CUPS + 1e-9) return null
    val units = UnitSystem.entries.firstOrNull { it.name == getString("units") } ?: UnitSystem.US_CUPS
    val sweetness = SweetnessType.entries.firstOrNull { it.name == getString("sweet") } ?: return null
    val addIns = getString("addins").orEmpty().split(',')
        .filter { id -> id.isNotBlank() && recipe.addIns.any { it.id == id } && catalog.addIns.containsKey(id) }
        .toSet()
    return Choice(flour, units, sweetness, addIns)
}
