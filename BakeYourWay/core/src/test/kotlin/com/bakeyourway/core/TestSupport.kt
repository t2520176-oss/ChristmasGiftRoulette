package com.bakeyourway.core

import java.io.File

/** Loads the real catalog from the app's assets folder - the same JSON that ships in the APK. */
object TestSupport {
    private val assetsRoot: File by lazy {
        val prop = System.getProperty("bakeyourway.assets") ?: error("bakeyourway.assets system property is not set")
        File(prop)
    }

    val catalog: RecipeCatalog by lazy {
        CatalogLoader.load { path -> File(assetsRoot, "assets/$path").readText(Charsets.UTF_8) }
    }

    val manifest: File get() = File(assetsRoot, "AndroidManifest.xml")

    fun recipe(id: String): Recipe = catalog.recipe(id) ?: error("No recipe '$id'")

    fun select(
        flourCups: Double,
        sweetness: SweetnessType = SweetnessType.NORMAL_SUGAR,
        method: CookingMethod = CookingMethod.OVEN,
        addIns: Set<String> = emptySet(),
        system: UnitSystem = UnitSystem.US_CUPS,
    ) = RecipeSelection(flourCups, sweetness, method, addIns, system)

    fun calc(
        id: String,
        flourCups: Double,
        sweetness: SweetnessType = SweetnessType.NORMAL_SUGAR,
        method: CookingMethod = CookingMethod.OVEN,
        addIns: Set<String> = emptySet(),
        system: UnitSystem = UnitSystem.US_CUPS,
    ): CalculatedRecipe = RecipeCalculator.calculate(recipe(id), catalog, select(flourCups, sweetness, method, addIns, system))

    fun CalculatedRecipe.line(ingredientId: String): CalculatedIngredient =
        ingredients.first { it.ingredientId == ingredientId }
}
