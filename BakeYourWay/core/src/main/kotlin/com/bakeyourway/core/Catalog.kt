package com.bakeyourway.core

import kotlinx.serialization.json.Json

/** Everything the app knows, loaded once from local JSON. No network is ever involved. */
class RecipeCatalog(
    val ingredients: Map<String, IngredientDef>,
    val addIns: Map<String, AddInDef>,
    val toppings: Map<String, ToppingDef>,
    val recipes: List<Recipe>,
) {
    private val byId: Map<String, Recipe> = recipes.associateBy { it.id }

    fun recipe(id: String): Recipe? = byId[id]

    fun recipesIn(category: RecipeCategory): List<Recipe> = recipes.filter { it.category == category }

    /** Categories that actually contain recipes, in display order. */
    fun categories(): List<RecipeCategory> = RecipeCategory.entries.filter { c -> recipes.any { it.category == c } }

    fun search(query: String): List<Recipe> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return recipes
        val words = q.split(Regex("\\s+"))
        return recipes.filter { r ->
            val haystack = "${r.name} ${r.category.displayName} ${r.description}".lowercase()
            words.all { haystack.contains(it) }
        }
    }

    fun ingredient(id: String): IngredientDef =
        ingredients[id] ?: error("Unknown ingredient '$id'")
}

/**
 * Reads the catalog through a caller-supplied text reader so the same code runs on Android
 * (AssetManager) and in plain JVM unit tests (files).
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object CatalogLoader {
    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
        allowComments = true
        allowTrailingComma = true
    }

    const val INDEX_PATH = "data/index.json"

    fun load(readText: (path: String) -> String): RecipeCatalog {
        val index = json.decodeFromString(CatalogIndex.serializer(), readText(INDEX_PATH))
        val ingredients = json.decodeFromString(IngredientFile.serializer(), readText("data/${index.ingredients}"))
            .ingredients.associateBy { it.id }
        val addIns = json.decodeFromString(AddInFile.serializer(), readText("data/${index.addIns}"))
            .addIns.associateBy { it.id }
        val toppings = json.decodeFromString(ToppingFile.serializer(), readText("data/${index.toppings}"))
            .toppings.associateBy { it.id }

        val recipes = index.recipeFiles.flatMap { path ->
            val file = json.decodeFromString(RecipeFile.serializer(), readText("data/$path"))
            file.recipes.map { resolveProfiles(it, file.profileSets, path) }
        }
        return RecipeCatalog(ingredients, addIns, toppings, recipes)
    }

    /** Expands `profileSet` + `profileOverrides` + `excludeMethods` into the final profile list. */
    private fun resolveProfiles(
        recipe: Recipe,
        sets: Map<String, List<CookingProfile>>,
        file: String,
    ): Recipe {
        val base = recipe.profileSet?.let { name ->
            sets[name] ?: error("Recipe '${recipe.id}' in $file uses unknown profileSet '$name'")
        } ?: emptyList()
        val merged = LinkedHashMap<CookingMethod, CookingProfile>()
        base.forEach { merged[it.method] = it }
        recipe.profileOverrides.forEach { merged[it.method] = it }
        recipe.excludeMethods.forEach { merged.remove(it) }
        recipe.cookingProfiles.forEach { merged[it.method] = it }
        return recipe.copy(cookingProfiles = CookingMethod.entries.mapNotNull { merged[it] })
    }
}

/** Consistency checks for the recipe data. An empty list means the data is internally sound. */
object CatalogValidator {
    fun validate(catalog: RecipeCatalog): List<String> {
        val problems = mutableListOf<String>()
        fun bad(msg: String) { problems += msg }

        val seenIds = HashSet<String>()
        for (r in catalog.recipes) {
            val p = "Recipe '${r.id}'"
            if (!seenIds.add(r.id)) bad("$p: duplicate id")
            if (r.name.isBlank()) bad("$p: blank name")
            if (r.description.isBlank()) bad("$p: blank description")
            if (r.baseFlourAmount <= 0) bad("$p: baseFlourAmount must be positive")
            if (r.baseFlourUnit != MeasureUnit.CUP) bad("$p: baseFlourUnit must be CUP in v1")

            // Ingredients
            val flourLines = r.ingredients.filter { it.role == IngredientRole.FLOUR }
            if (flourLines.size != 1) {
                bad("$p: needs exactly one FLOUR ingredient line (has ${flourLines.size})")
            } else {
                val f = flourLines.single()
                if (f.ingredient != r.flourIngredient) bad("$p: FLOUR line is '${f.ingredient}' but flourIngredient is '${r.flourIngredient}'")
                if (f.unit != r.baseFlourUnit || f.amount != r.baseFlourAmount) bad("$p: FLOUR line amount/unit differs from baseFlourAmount/Unit")
            }
            val flourDef = catalog.ingredients[r.flourIngredient]
            if (flourDef == null) bad("$p: unknown flourIngredient '${r.flourIngredient}'")
            else if (flourDef.gramsPerCup == null) bad("$p: flour '${flourDef.id}' has no gramsPerCup")

            if (r.ingredients.none { it.role == IngredientRole.SUGAR }) bad("$p: no SUGAR ingredient (Less Sugar would do nothing)")
            for (l in r.ingredients) {
                val def = catalog.ingredients[l.ingredient]
                if (def == null) { bad("$p: unknown ingredient '${l.ingredient}'"); continue }
                if (l.amount <= 0) bad("$p: ingredient '${l.ingredient}' has non-positive amount")
                if (l.unit == MeasureUnit.PIECE && def.gramsPerPiece == null && def.style != MeasureStyle.COUNT) {
                    bad("$p: ingredient '${l.ingredient}' is in pieces but has no piece data")
                }
                if (l.unit in listOf(MeasureUnit.CUP, MeasureUnit.TBSP, MeasureUnit.TSP, MeasureUnit.GRAM, MeasureUnit.ML) &&
                    def.style != MeasureStyle.COUNT && def.gramsPerCup == null
                ) bad("$p: ingredient '${l.ingredient}' measured by volume/weight but has no gramsPerCup")
            }

            // Yield
            if (r.yieldSpec.base <= 0) bad("$p: yield.base must be positive")

            // Sweetness
            val less = r.sweetness.less.multiplier
            if (less < 0.4 || less >= 1.0) bad("$p: lessSugar multiplier $less must be in [0.4, 1.0)")
            if (r.sweetness.less.note.isNullOrBlank()) bad("$p: missing lessSugar note")
            if (r.sweetness.less.expectedChanges.isEmpty()) bad("$p: missing lessSugar expectedChanges")
            for (id in r.sweetness.less.suggestedAlternatives) {
                if (r.addIns.none { it.id == id }) bad("$p: suggested alternative '$id' is not one of the recipe's add-ins")
            }

            // Cooking
            if (r.cookingProfiles.isEmpty()) bad("$p: no cooking methods")
            for (c in r.cookingProfiles) {
                val cp = "$p/${c.method}"
                if (c.timeMin <= 0 || c.timeMax < c.timeMin) bad("$cp: invalid time range ${c.timeMin}-${c.timeMax}")
                if (c.cook.isBlank()) bad("$cp: blank cook instruction")
                when (c.method) {
                    CookingMethod.OVEN, CookingMethod.AIR_FRYER -> {
                        if (c.temperatureC == null || c.temperatureF == null) bad("$cp: needs temperatures")
                        else if (kotlin.math.abs(c.temperatureC * 9.0 / 5 + 32 - c.temperatureF) > 10) {
                            bad("$cp: ${c.temperatureC}C and ${c.temperatureF}F do not match")
                        }
                    }
                    CookingMethod.STOVETOP -> if (c.heatLevel.isNullOrBlank()) bad("$cp: stovetop needs heatLevel")
                    CookingMethod.STEAM -> if (c.heatLevel.isNullOrBlank()) bad("$cp: steam needs heatLevel")
                }
                if (c.batchCapacity != null && c.batchCapacity <= 0) bad("$cp: batchCapacity must be positive")
            }
            for (m in r.excludeMethods) if (r.profileFor(m) != null) bad("$p: excluded method $m still has a profile")

            // Steps
            if (r.steps.size < 3) bad("$p: too few steps")
            if (r.steps.any { it.kind == StepKind.NORMAL && (it.title.isBlank() || it.text.isBlank()) }) bad("$p: a step has no title/text")
            if (r.steps.count { it.kind == StepKind.ADD_INS } > 1) bad("$p: more than one ADD_INS marker")

            // Add-ins and toppings
            for (a in r.addIns) {
                if (catalog.addIns[a.id] == null) bad("$p: unknown add-in '${a.id}'")
                if (a.amountMultiplier <= 0) bad("$p: add-in '${a.id}' multiplier must be positive")
            }
            if (r.addIns.map { it.id }.toSet().size != r.addIns.size) bad("$p: duplicate add-ins")
            for (t in r.toppingSuggestions) if (catalog.toppings[t] == null) bad("$p: unknown topping '$t'")
            if (r.toppingSuggestions.isEmpty()) bad("$p: no topping suggestions")

            // Pan guidance must be ordered.
            val maxes = r.panOptions.map { it.maxFlourCups }
            if (maxes != maxes.sorted()) bad("$p: panOptions must be sorted by maxFlourCups")
        }

        for (a in catalog.addIns.values) {
            for (l in a.lines) {
                val def = catalog.ingredients[l.ingredient]
                if (def == null) bad("Add-in '${a.id}': unknown ingredient '${l.ingredient}'")
                else if (def.gramsPerCup == null && def.style != MeasureStyle.COUNT) bad("Add-in '${a.id}': '${l.ingredient}' has no gramsPerCup")
            }
            if (a.placement != AddInPlacement.FOLD_IN && a.instruction.isNullOrBlank()) bad("Add-in '${a.id}': needs an instruction")
        }
        for (i in catalog.ingredients.values) {
            if (i.style == MeasureStyle.COUNT && i.gramsPerPiece == null) bad("Ingredient '${i.id}': COUNT needs gramsPerPiece")
            if (i.style == MeasureStyle.COUNT && (i.pieceSingular == null || i.piecePlural == null)) bad("Ingredient '${i.id}': COUNT needs piece names")
        }
        return problems
    }
}
