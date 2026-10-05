package com.bakeyourway.core

import kotlin.math.ceil
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Input
// ---------------------------------------------------------------------------------------------

/**
 * Everything the user chose. The flour amount is always stored in US cups (the recipe's reference
 * unit); grams typed by the user are converted with the recipe's own flour density first
 * (see [FlourInput.toCups]).
 */
data class RecipeSelection(
    val flourCups: Double,
    val sweetness: SweetnessType,
    val method: CookingMethod,
    val addInIds: Set<String> = emptySet(),
    val unitSystem: UnitSystem = UnitSystem.US_CUPS,
    /** Substitutions for ingredients the user does not have (see [SubstitutionEngine]). */
    val substitutions: List<SubstitutionChoice> = emptyList(),
)

// ---------------------------------------------------------------------------------------------
// Output
// ---------------------------------------------------------------------------------------------

data class CalculatedIngredient(
    val ingredientId: String,
    /** Display name including preparation, e.g. "Butter, melted". */
    val name: String,
    val group: String?,
    val optional: Boolean,
    val isAddIn: Boolean,
    val isSugar: Boolean,
    /** Exact scaled amount in the unit the recipe is written in (no display rounding). */
    val amount: Double,
    val unit: MeasureUnit,
    val cups: Double?,
    val grams: Double?,
    val primaryText: String,
    val secondaryText: String?,
    val note: String?,
    val function: IngredientFunction = IngredientFunction.OTHER,
    val importance: IngredientImportance = IngredientImportance.NORMAL,
    /** The preparation as written in the recipe ("melted", "cold, cubed"). */
    val prep: String? = null,
    /** Set for lines that come from an add-in. */
    val addInId: String? = null,
    /** Set when this line replaces an ingredient the user does not have ("butter"). */
    val substitutedFor: String? = null,
    val substitutionId: String? = null,
    /** e.g. "reduced by 2 tbsp because honey adds liquid" */
    val adjustment: String? = null,
) {
    val isSubstitute: Boolean get() = substitutedFor != null
    /** "2 cups (240 g)" */
    val displayText: String get() = if (secondaryText.isNullOrBlank()) primaryText else "$primaryText ($secondaryText)"
}

data class YieldEstimate(
    val count: Int,
    val label: String,
    val text: String,
    val note: String?,
)

data class BatchInfo(val batches: Int, val perBatch: Int, val note: String)

/** Time/temperature facts for one cooking method. Never derived from the flour amount. */
data class CookingInfo(
    val method: CookingMethod,
    val temperatureC: Int?,
    val temperatureF: Int?,
    val timeMin: Int,
    val timeMax: Int,
    val heatLevel: String?,
    val preheat: Boolean,
    val notes: List<String>,
    val temperatureText: String,
    val timeText: String,
    val isSelected: Boolean,
)

data class InstructionStep(val number: Int, val title: String, val text: String)

data class Conclusion(
    val title: String,
    val summary: String,
    val recipeNote: String?,
    val sugarChanges: List<String>,
    val whatToExpectTitle: String,
    val whatToExpect: List<String>,
    val naturalSweetnessTitle: String?,
    val naturalSweetnessIdeas: List<String>,
    val closing: String?,
    val importantNotes: List<String>,
    val allergenNotices: List<String>,
    val allergenDisclaimer: String,
    val generalNote: String,
    /** "YOUR RECIPE SUMMARY": recipe, flour, sweetness, cooking and substitutions. */
    val recipeSummary: List<SummaryLine> = emptyList(),
    /** "Butter → Neutral oil" */
    val substitutionSummary: List<String> = emptyList(),
    /** Personalised paragraph about what the sweetness choice and the substitutions will change. */
    val substitutionExpect: String? = null,
    /** Suggested optional improvements after substituting. */
    val substitutionImprovement: String? = null,
    /** Label reminders for substitutes (plant milk, margarine ...). */
    val labelChecks: List<String> = emptyList(),
)

data class SummaryLine(val label: String, val value: String)

data class CalculatedRecipe(
    val recipe: Recipe,
    val selection: RecipeSelection,
    val flourCups: Double,
    val flourText: String,
    val scaleFactor: Double,
    /** Main recipe ingredients (including the flour line). */
    val ingredients: List<CalculatedIngredient>,
    /** Lines contributed by the add-ins the user selected. */
    val addInIngredients: List<CalculatedIngredient>,
    val yieldEstimate: YieldEstimate,
    val batchInfo: BatchInfo?,
    val panGuidance: String?,
    val cooking: CookingInfo,
    val methodComparison: List<CookingInfo>,
    val steps: List<InstructionStep>,
    val tips: List<String>,
    val methodTips: List<String>,
    val toppingSuggestions: List<ToppingDef>,
    val allergens: List<Allergen>,
    val conclusion: Conclusion,
    val warnings: List<String>,
    val cookingDisclaimer: String,
    val appliedSubstitutions: List<AppliedSubstitution> = emptyList(),
    /** Ids of requested substitutions that could not be applied to this recipe. */
    val rejectedSubstitutions: List<String> = emptyList(),
) {
    val allIngredients: List<CalculatedIngredient> get() = ingredients + addInIngredients
    val summaryUsing: String get() = "Using: $flourText flour"
}

// ---------------------------------------------------------------------------------------------
// Flour amount input
// ---------------------------------------------------------------------------------------------

object FlourInput {
    const val MIN_CUPS = 0.25
    const val MAX_CUPS = 6.0
    const val WARN_CUPS = 4.0

    val QUICK_CUPS: List<Double> = listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0)

    sealed interface Result {
        data class Ok(val cups: Double, val warning: String?) : Result
        data class Error(val message: String) : Result
    }

    /** Parses "1.5", "1,5", "1/2", "1 1/2" (an optional unit word is ignored). Returns null if invalid. */
    fun parse(text: String): Double? {
        val cleaned = text.trim().lowercase()
            .replace(',', '.')
            .replace(Regex("(cups?|c|grams?|g)$"), "")
            .trim()
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split(Regex("\\s+"))
        var total = 0.0
        for (part in parts) {
            val v = if (part.contains('/')) {
                val frac = part.split('/')
                if (frac.size != 2) return null
                val n = frac[0].toDoubleOrNull() ?: return null
                val d = frac[1].toDoubleOrNull() ?: return null
                if (d == 0.0) return null
                n / d
            } else {
                if (!part.matches(Regex("[0-9]*\\.?[0-9]+|[0-9]+\\."))) return null
                part.toDoubleOrNull() ?: return null
            }
            total += v
        }
        return if (total.isFinite()) total else null
    }

    fun toCups(value: Double, system: UnitSystem, gramsPerCup: Double): Double =
        if (system == UnitSystem.US_CUPS) value else value / gramsPerCup

    fun fromCups(cups: Double, system: UnitSystem, gramsPerCup: Double): Double =
        if (system == UnitSystem.US_CUPS) cups else cups * gramsPerCup

    fun validate(text: String, system: UnitSystem, gramsPerCup: Double): Result {
        if (text.isBlank()) return Result.Error("Please enter how much flour you want to use.")
        val value = parse(text) ?: return Result.Error("Please enter a number, for example 1.5")
        if (value <= 0.0) return Result.Error("The amount must be greater than zero.")
        return validateCups(toCups(value, system, gramsPerCup), system, gramsPerCup)
    }

    fun validateCups(cups: Double, system: UnitSystem, gramsPerCup: Double): Result {
        if (cups <= 0.0 || !cups.isFinite()) return Result.Error("The amount must be greater than zero.")
        if (cups < MIN_CUPS - 1e-9) {
            return Result.Error("That is a very small batch. The minimum is ${limitText(MIN_CUPS, system, gramsPerCup)}.")
        }
        if (cups > MAX_CUPS + 1e-9) {
            return Result.Error("That is more than a home kitchen can handle. The maximum is ${limitText(MAX_CUPS, system, gramsPerCup)}.")
        }
        val warning = when {
            cups > WARN_CUPS + 1e-9 ->
                "That is a big batch! You will need a very large bowl and probably several pans or batches."
            cups < 0.5 - 1e-9 ->
                "Very small batch: small measures and eggs are harder to get exact, so results may vary a little."
            else -> null
        }
        return Result.Ok(cups, warning)
    }

    private fun limitText(cups: Double, system: UnitSystem, gramsPerCup: Double): String =
        if (system == UnitSystem.US_CUPS) MeasureFormatter.formatFlourCups(cups)
        else MeasureFormatter.formatGrams(cups * gramsPerCup)

    /** Label for a flour amount in the chosen system, e.g. "1 1/2 cups" or "180 g". */
    fun label(cups: Double, system: UnitSystem, gramsPerCup: Double): String =
        if (system == UnitSystem.US_CUPS) MeasureFormatter.formatFlourCups(cups)
        else MeasureFormatter.formatGrams(cups * gramsPerCup)
}

// ---------------------------------------------------------------------------------------------
// Calculator
// ---------------------------------------------------------------------------------------------

object RecipeCalculator {
    const val COOKING_DISCLAIMER =
        "Cooking times are approximate. Appliances, pan size and portion size vary. Check for doneness before serving."
    const val RESULTS_VARY_NOTE =
        "Recipe results can vary depending on ingredient brands, altitude, humidity, pan size and appliance. Cooking times are approximate."
    const val ALLERGEN_DISCLAIMER =
        "Allergen notices are based on the standard ingredients listed here. Always check the labels of the products you use."
    const val UNSUPPORTED_METHOD_MESSAGE = "This cooking method is not recommended for this recipe."

    /** Pure scaling factor: how many "base recipes" the chosen flour amount equals. */
    fun scaleFactor(recipe: Recipe, flourCups: Double): Double = flourCups / recipe.baseFlourAmount

    fun calculate(recipe: Recipe, catalog: RecipeCatalog, selection: RecipeSelection): CalculatedRecipe {
        val baseline = calculateBaseline(recipe, catalog, selection)
        if (selection.substitutions.isEmpty()) return baseline
        return applySubstitutions(baseline, catalog, selection)
    }

    /** The recipe exactly as written (no substitutions): the amounts every substitution is calculated from. */
    fun calculateBaseline(recipe: Recipe, catalog: RecipeCatalog, selection: RecipeSelection): CalculatedRecipe {
        require(selection.flourCups > 0 && selection.flourCups.isFinite()) { "Flour amount must be positive" }
        val profile = recipe.profileFor(selection.method)
            ?: throw IllegalArgumentException("${recipe.name} does not support ${selection.method.displayName}")

        val factor = scaleFactor(recipe, selection.flourCups)
        val sweetness = recipe.profileForSweetness(selection.sweetness)
        val sugarMultiplier = sweetness.multiplier
        val flourDef = catalog.ingredient(recipe.flourIngredient)
        val flourPerCup = flourDef.gramsPerCup ?: error("Flour '${flourDef.id}' has no density")
        val system = selection.unitSystem

        // ----- Ingredients (scaling only; cooking time is handled further below) ------------
        val ingredients = recipe.ingredients.map { line ->
            val def = catalog.ingredient(line.ingredient)
            var amount = line.amount
            if (line.scalable) amount *= factor
            if (line.role == IngredientRole.SUGAR) amount *= sugarMultiplier
            buildIngredient(def, line, amount, system)
        }

        // ----- Add-ins -----------------------------------------------------------------------
        val selectedAddIns = recipe.addIns
            .filter { it.id in selection.addInIds }
            .map { ref -> ref to (catalog.addIns[ref.id] ?: error("Unknown add-in '${ref.id}'")) }
        val addInIngredients = selectedAddIns.flatMap { (ref, def) ->
            def.lines.map { l ->
                val ing = catalog.ingredient(l.ingredient)
                val amount = l.perFlourCup * selection.flourCups * ref.amountMultiplier
                val synthetic = IngredientLine(
                    ingredient = l.ingredient,
                    amount = amount,
                    unit = l.unit,
                    prep = l.prep,
                    optional = true,
                    group = "Your optional add-ins",
                    note = def.name,
                )
                buildIngredient(ing, synthetic, amount, system, addIn = true, addInId = def.id)
            }
        }

        // ----- Yield and batches (portions scale with flour; cooking time does not) -----------
        val yieldEstimate = estimateYield(recipe, factor)
        val batch = profile.batchCapacity?.let { cap ->
            if (yieldEstimate.count > cap) {
                val n = ceil(yieldEstimate.count / cap.toDouble()).toInt()
                BatchInfo(
                    batches = n,
                    perBatch = cap,
                    note = "This makes about ${yieldEstimate.count} ${yieldEstimate.label.substringAfter(' ')}, but your " +
                        "${selection.method.displayName.lowercase()} fits about $cap at a time. Cook in $n batches. " +
                        "Each batch needs the same cooking time as a single batch - the time does not increase with the flour amount.",
                )
            } else null
        }
        val pan = recipe.panOptions.firstOrNull { selection.flourCups <= it.maxFlourCups + 1e-9 }
            ?: recipe.panOptions.lastOrNull()

        val flourText = flourLabel(selection.flourCups, system, flourPerCup)
        return assemble(
            recipe = recipe, catalog = catalog, selection = selection, profile = profile, factor = factor,
            flourText = flourText, flourPerCup = flourPerCup,
            ingredients = ingredients, addInIngredients = addInIngredients,
            addIns = selectedAddIns.map { it.second },
            yieldEstimate = yieldEstimate, batch = batch, panText = pan?.text,
            applied = emptyList(), rejected = emptyList(),
            stepText = StepText.NONE, overrides = emptyList(),
        )
    }

    private fun applySubstitutions(
        baseline: CalculatedRecipe,
        catalog: RecipeCatalog,
        selection: RecipeSelection,
    ): CalculatedRecipe {
        val recipe = baseline.recipe
        val selectedDefs = recipe.addIns.filter { it.id in selection.addInIds }.mapNotNull { catalog.addIns[it.id] }
        val result = SubstitutionEngine.apply(baseline, catalog, selectedDefs, selection.substitutions)
        val profile = recipe.profileFor(selection.method)!!
        val flourPerCup = catalog.ingredient(recipe.flourIngredient).gramsPerCup ?: error("Flour has no density")
        return assemble(
            recipe = recipe, catalog = catalog, selection = selection, profile = profile, factor = baseline.scaleFactor,
            flourText = baseline.flourText, flourPerCup = flourPerCup,
            ingredients = result.ingredients, addInIngredients = result.addInIngredients,
            addIns = result.addIns,
            yieldEstimate = baseline.yieldEstimate, batch = baseline.batchInfo, panText = baseline.panGuidance,
            applied = result.applied, rejected = result.rejected,
            stepText = StepText(result.changes), overrides = result.overrides,
        )
    }

    /** Builds everything that depends on the final ingredient lists (steps, allergens, conclusion ...). */
    private fun assemble(
        recipe: Recipe,
        catalog: RecipeCatalog,
        selection: RecipeSelection,
        profile: CookingProfile,
        factor: Double,
        flourText: String,
        flourPerCup: Double,
        ingredients: List<CalculatedIngredient>,
        addInIngredients: List<CalculatedIngredient>,
        addIns: List<AddInDef>,
        yieldEstimate: YieldEstimate,
        batch: BatchInfo?,
        panText: String?,
        applied: List<AppliedSubstitution>,
        rejected: List<String>,
        stepText: StepText,
        overrides: List<ResolvedOverride>,
    ): CalculatedRecipe {
        val system = selection.unitSystem

        // ----- Cooking (independent of the scale factor) ---------------------------------------
        val cooking = cookingInfo(profile, selected = true, stepText = stepText)
        val comparison = recipe.cookingProfiles.map { cookingInfo(it, it.method == selection.method, stepText) }

        // ----- Allergens (from the final ingredient lists, so substitutions are respected) ------
        val allergens = buildSet {
            addAll(recipe.allergenTags)
            (ingredients + addInIngredients).forEach { addAll(catalog.ingredient(it.ingredientId).allergens) }
        }.sortedBy { it.ordinal }
        val labelChecks = (ingredients + addInIngredients).filter { it.isSubstitute }
            .mapNotNull { catalog.ingredient(it.ingredientId).allergenHint }.distinct()

        // ----- Steps ---------------------------------------------------------------------------
        val steps = buildSteps(recipe, profile, addIns, addInIngredients, batch, panText, stepText, overrides)

        // ----- Warnings ------------------------------------------------------------------------
        val warnings = mutableListOf<String>()
        (FlourInput.validateCups(selection.flourCups, system, flourPerCup) as? FlourInput.Result.Ok)?.warning?.let { warnings += it }
        if (ingredients.any { it.primaryText.contains("beaten egg") }) {
            warnings += "The egg amount is not a whole number at this flour amount, so a little beaten egg makes up the difference."
        }
        batch?.let { warnings += "Multiple batches needed: ${it.batches} batches of up to ${it.perBatch}." }
        applied.forEach { a -> a.warnings.forEach { if (it !in warnings) warnings += it } }
        val combo = SubstitutionEngine.combinationWarningFor(applied.map { importanceOf(recipe, catalog, it.originalIngredient) })
        if (combo != null && combo !in warnings) warnings += combo

        val toppings = recipe.toppingSuggestions.mapNotNull { catalog.toppings[it] }
        val conclusion = buildConclusion(
            recipe, catalog, selection, ingredients, addIns, allergens, batch, applied, flourText, labelChecks,
        )

        return CalculatedRecipe(
            recipe = recipe,
            selection = selection,
            flourCups = selection.flourCups,
            flourText = flourText,
            scaleFactor = factor,
            ingredients = ingredients,
            addInIngredients = addInIngredients,
            yieldEstimate = yieldEstimate,
            batchInfo = batch,
            panGuidance = panText,
            cooking = cooking,
            methodComparison = comparison,
            steps = steps,
            tips = recipe.tips.mapNotNull { stepText.filterNote(it) },
            methodTips = profile.notes.mapNotNull { stepText.filterNote(it) },
            toppingSuggestions = toppings,
            allergens = allergens,
            conclusion = conclusion,
            warnings = warnings,
            cookingDisclaimer = COOKING_DISCLAIMER,
            appliedSubstitutions = applied,
            rejectedSubstitutions = rejected,
        )
    }

    private fun importanceOf(recipe: Recipe, catalog: RecipeCatalog, ingredientId: String): IngredientImportance {
        val line = recipe.ingredients.firstOrNull { it.ingredient == ingredientId }
        return line?.importance ?: catalog.ingredients[ingredientId]?.importance ?: IngredientImportance.NORMAL
    }

    /** "2 cups (240 g)" - the flour is always shown in both systems. */
    private fun flourLabel(cups: Double, system: UnitSystem, gramsPerCup: Double): String {
        val cupText = MeasureFormatter.formatFlourCups(cups)
        val gramText = MeasureFormatter.formatGrams(cups * gramsPerCup)
        return if (system == UnitSystem.US_CUPS) "$cupText ($gramText)" else "$gramText ($cupText)"
    }

    // ----- Ingredient formatting ------------------------------------------------------------

    internal fun buildIngredient(
        def: IngredientDef,
        line: IngredientLine,
        amount: Double,
        system: UnitSystem,
        addIn: Boolean = false,
        addInId: String? = null,
        label: String? = null,
        fine: Boolean = false,
    ): CalculatedIngredient {
        val grams = MeasureFormatter.toGrams(amount, line.unit, def)
        val cups = MeasureFormatter.toCups(amount, line.unit, def)
        val rule = line.rounding ?: def.rounding
        val (primary, secondary) = displayText(def, line.unit, amount, rule, system, fine)
        val baseName = label ?: def.name
        val name = if (line.prep.isNullOrBlank()) baseName else "$baseName, ${line.prep}"
        return CalculatedIngredient(
            ingredientId = def.id,
            name = name,
            group = line.group,
            optional = line.optional,
            isAddIn = addIn,
            isSugar = line.role == IngredientRole.SUGAR,
            amount = amount,
            unit = line.unit,
            cups = cups,
            grams = grams,
            primaryText = primary,
            secondaryText = secondary,
            note = line.note,
            function = line.function ?: def.function,
            importance = line.importance ?: if (line.optional) IngredientImportance.OPTIONAL else def.importance,
            prep = line.prep,
            addInId = addInId,
        )
    }

    /** Returns (primary, secondary) display strings for one ingredient. */
    fun displayText(
        def: IngredientDef,
        unit: MeasureUnit,
        amount: Double,
        rule: RoundingRule,
        system: UnitSystem,
        fine: Boolean = false,
    ): Pair<String, String?> {
        // Counted items
        if (def.style == MeasureStyle.COUNT || unit == MeasureUnit.PIECE) {
            return if (rule == RoundingRule.EGG) {
                MeasureFormatter.formatEggs(amount, def).text to null
            } else {
                MeasureFormatter.formatPieces(amount, def, rule) to null
            }
        }
        val tsp = MeasureFormatter.toTeaspoons(amount, unit)
            ?: MeasureFormatter.toCups(amount, unit, def)?.times(MeasureFormatter.TSP_PER_CUP)
            ?: return "${MeasureFormatter.trimNumber(amount)} ${unit.name.lowercase()}" to null

        val volume = if (rule == RoundingRule.NONE) {
            "${MeasureFormatter.trimNumber(tsp / MeasureFormatter.TSP_PER_CUP)} cup"
        } else if (def.style == MeasureStyle.SPOON && tsp < 6.0) {
            // "3 tsp baking powder", not "1 tbsp" - leaveners and spices stay in teaspoons.
            MeasureFormatter.formatTeaspoons(tsp)
        } else {
            MeasureFormatter.formatVolume(tsp, fine)
        }
        val grams = MeasureFormatter.toGrams(amount, unit, def)
        val metricText: String? = grams?.let {
            if (def.style == MeasureStyle.LIQUID) {
                MeasureFormatter.formatMl(tsp / MeasureFormatter.TSP_PER_CUP * MeasureFormatter.ML_PER_CUP)
            } else MeasureFormatter.formatGrams(it)
        }
        // Spoon-sized amounts stay in spoons (nobody weighs 1/4 tsp of baking powder).
        if (def.style == MeasureStyle.SPOON || metricText == null) return volume to null
        val spoonSized = tsp < MeasureFormatter.TSP_PER_TBSP - 1e-9
        return when (system) {
            UnitSystem.US_CUPS -> volume to (if (spoonSized) null else metricText)
            UnitSystem.METRIC -> if (spoonSized) volume to null else metricText to volume
        }
    }

    // ----- Yield ----------------------------------------------------------------------------

    private fun estimateYield(recipe: Recipe, factor: Double): YieldEstimate {
        val y = recipe.yieldSpec
        val count = maxOf((y.base * factor).roundToInt(), 1)
        val noun = if (count == 1) y.singular else y.plural
        return YieldEstimate(count, "$count $noun", "about $count $noun", y.note)
    }

    // ----- Cooking ----------------------------------------------------------------------------

    private fun cookingInfo(p: CookingProfile, selected: Boolean, stepText: StepText) = CookingInfo(
        method = p.method,
        temperatureC = p.temperatureC,
        temperatureF = p.temperatureF,
        timeMin = p.timeMin,
        timeMax = p.timeMax,
        heatLevel = p.heatLevel,
        preheat = p.preheat,
        notes = p.notes.mapNotNull { stepText.filterNote(it) },
        temperatureText = temperatureText(p),
        timeText = timeText(p),
        isSelected = selected,
    )

    fun temperatureText(p: CookingProfile): String = when {
        p.temperatureC != null && p.temperatureF != null -> "${p.temperatureC}°C / ${p.temperatureF}°F"
        !p.heatLevel.isNullOrBlank() -> p.heatLevel
        else -> ""
    }

    fun timeText(p: CookingProfile): String =
        if (p.timeMin == p.timeMax) "${p.timeMin} minutes" else "${p.timeMin}–${p.timeMax} minutes"

    // ----- Instructions -----------------------------------------------------------------------

    private fun fill(text: String, p: CookingProfile, pan: String? = null): String = text
        .replace("{time}", timeText(p))
        .replace("{temp}", temperatureText(p))
        .replace("{heat}", p.heatLevel ?: "")
        .replace("{pan}", pan ?: "a suitable pan")

    private fun preheatStep(p: CookingProfile): Pair<String, String>? = when (p.method) {
        CookingMethod.OVEN -> if (p.preheat) {
            "Preheat the Oven" to "Preheat the oven to ${temperatureText(p)}" +
                (p.preheatMinutes?.let { " (this takes about $it minutes)." } ?: ".")
        } else null
        CookingMethod.AIR_FRYER -> if (p.preheat) {
            "Preheat the Air Fryer" to "Preheat the air fryer to ${temperatureText(p)}" +
                (p.preheatMinutes?.let { " for about $it minutes." } ?: ".")
        } else null
        CookingMethod.STEAM -> "Prepare the Steamer" to
            "Add water to the steamer pot and bring it to a boil, then lower the heat to a steady simmer for medium steam. " +
            "Keep the water below the steamer rack so it never touches the food."
        CookingMethod.STOVETOP -> "Heat the Pan" to
            "Heat a non-stick pan or griddle over ${p.heatLevel?.lowercase() ?: "medium"} heat."
    }

    private fun cookTitle(m: CookingMethod) = when (m) {
        CookingMethod.OVEN -> "Bake"
        CookingMethod.AIR_FRYER -> "Air Fry"
        CookingMethod.STEAM -> "Steam"
        CookingMethod.STOVETOP -> "Cook"
    }

    private fun lowerFirst(s: String) = s.replaceFirstChar { it.lowercase() }

    /** One recipe step while it is being assembled (before numbering). */
    private class RawStep(val title: String, val text: String, val tag: String? = null, val isAddInMarker: Boolean = false)

    private fun buildSteps(
        recipe: Recipe,
        p: CookingProfile,
        addIns: List<AddInDef>,
        addInIngredients: List<CalculatedIngredient>,
        batch: BatchInfo?,
        pan: String?,
        stepText: StepText,
        overrides: List<ResolvedOverride>,
    ): List<InstructionStep> {
        fun render(text: String) = fill(stepText.render(text), p, pan)
        fun renderStep(s: RecipeStep) = fill(stepText.render(s.text, dry = s.tag == "MIX_DRY"), p, pan)

        val setup = mutableListOf<Pair<String, String>>()
        preheatStep(p)?.let { setup += it }
        p.prep?.takeIf { it.isNotBlank() }?.let { setup += "Prepare" to render(it) }

        val foldIns = addIns.filter { it.placement == AddInPlacement.FOLD_IN }
        val tops = addIns.filter { it.placement == AddInPlacement.BEFORE_COOK_TOP }
        val afters = addIns.filter { it.placement == AddInPlacement.AFTER_COOK }

        val foldStep: Pair<String, String>? = if (foldIns.isEmpty()) null else {
            val parts = foldIns.map { def ->
                val first = addInIngredients.firstOrNull { it.addInId == def.id }
                val noun = if (first != null && first.isSubstitute) first.name.substringBefore(',').lowercase() else def.name.lowercase()
                if (first != null) "$noun (${first.primaryText})" else noun
            }
            "Add Optional Ingredients" to "Gently fold in ${joinNatural(parts)} until just combined. Don't overmix."
        }
        val topStep: Pair<String, String>? = if (tops.isEmpty()) null else
            "Add Toppings" to tops.joinToString(" ") { render(it.instruction.orEmpty()) }
        val afterAddInStep: Pair<String, String>? = if (afters.isEmpty()) null else
            "Finish" to afters.joinToString(" ") { render(it.instruction.orEmpty()) }

        fun applicable(s: RecipeStep) = s.methods.isEmpty() || p.method in s.methods
        val beforeSteps = recipe.steps.filter { it.kind != StepKind.AFTER_COOK && applicable(it) }.map { s ->
            if (s.kind == StepKind.ADD_INS) RawStep("", "", isAddInMarker = true)
            else RawStep(stepText.render(s.title), renderStep(s), s.tag)
        }.toMutableList()
        val afterSteps = recipe.steps.filter { it.kind == StepKind.AFTER_COOK && applicable(it) }
            .map { RawStep(stepText.render(it.title), renderStep(it), it.tag) }.toMutableList()
        val hasMarker = beforeSteps.any { it.isAddInMarker }

        // Substitution-specific instruction changes, driven by step tags (never by text search).
        val category = recipe.category
        var prepended = 0
        for (resolved in overrides) {
            val o = resolved.override
            if (o.categories.isNotEmpty() && category !in o.categories) continue
            fun fillText(t: String?): String? = t?.let { raw ->
                var out = raw
                resolved.placeholders.forEach { (k, v) -> out = out.replace("{$k}", v) }
                render(out)
            }
            val title = fillText(o.title)
            val text = fillText(o.text)
            when (o.action) {
                OverrideAction.PREPEND_STEP -> {
                    beforeSteps.add(prepended, RawStep(title.orEmpty(), text.orEmpty()))
                    prepended++
                }
                OverrideAction.REPLACE_STEP -> for (list in listOf(beforeSteps, afterSteps)) {
                    for (i in list.indices) if (list[i].tag == o.stepTag) {
                        list[i] = RawStep(title ?: list[i].title, text ?: list[i].text, list[i].tag)
                    }
                }
                OverrideAction.APPEND_TO_STEP -> for (list in listOf(beforeSteps, afterSteps)) {
                    for (i in list.indices) if (list[i].tag == o.stepTag && text != null) {
                        list[i] = RawStep(list[i].title, list[i].text + " " + text, list[i].tag)
                    }
                }
                OverrideAction.REMOVE_STEP -> for (list in listOf(beforeSteps, afterSteps)) {
                    list.removeAll { it.tag == o.stepTag }
                }
            }
        }

        val raw = mutableListOf<Pair<String, String>>()
        if (recipe.preheatTiming == PreheatTiming.START) raw += setup
        for (s in beforeSteps) {
            if (s.isAddInMarker) foldStep?.let { raw += it } else raw += s.title to s.text
        }
        if (!hasMarker) foldStep?.let { raw += it }
        if (recipe.preheatTiming == PreheatTiming.BEFORE_COOK) raw += setup
        topStep?.let { raw += it }

        val batchText = if (batch != null) " Cook in ${batch.batches} batches; each batch uses this same time." else ""
        raw += cookTitle(p.method) to render(p.cook) + batchText
        raw += "Check for Doneness" to (p.check?.let { render(it) } ?: "Check for doneness before removing.")
        for (s in afterSteps) raw += s.title to s.text
        afterAddInStep?.let { raw += it }

        return raw.mapIndexed { i, (title, text) -> InstructionStep(i + 1, title, text) }
    }

    private fun joinNatural(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        2 -> "${items[0]} and ${items[1]}"
        else -> items.dropLast(1).joinToString(", ") + " and " + items.last()
    }

    // ----- Conclusion ---------------------------------------------------------------------------

    private fun buildConclusion(
        recipe: Recipe,
        catalog: RecipeCatalog,
        selection: RecipeSelection,
        ingredients: List<CalculatedIngredient>,
        addIns: List<AddInDef>,
        allergens: List<Allergen>,
        batch: BatchInfo?,
        applied: List<AppliedSubstitution>,
        flourText: String,
        labelChecks: List<String>,
    ): Conclusion {
        val base = sweetnessConclusion(recipe, catalog, selection, ingredients, addIns, allergens, batch)
        val summary = buildList {
            add(SummaryLine("Recipe", recipe.name))
            add(SummaryLine("Flour", flourText))
            add(SummaryLine("Sweetness", selection.sweetness.displayName))
            add(SummaryLine("Cooking", selection.method.displayName))
            if (addIns.isNotEmpty()) add(SummaryLine("Add-ins", addIns.joinToString(", ") { it.name }))
            applied.forEach { add(SummaryLine("Substitution", it.summary)) }
        }
        if (applied.isEmpty()) return base.copy(recipeSummary = summary)

        val lowerName = recipe.name.lowercase()
        val plural = recipe.name.endsWith("s")
        val subject = if (plural) "These $lowerName" else "This $lowerName"
        val pronoun = if (plural) "They" else "It"
        val verbs = applied.map {
            if (it.rule.isOmission) "left out ${it.originalName.lowercase()}"
            else "replaced ${it.originalName.lowercase()} with ${it.rule.substituteName.lowercase()}"
        }
        val sweetClause = if (selection.sweetness == SweetnessType.LESS_SUGAR) "will be less sweet than the traditional version"
        else "will have the traditional level of sweetness"
        val texture = applied.mapNotNull { it.rule.expectTexture }.distinct()
        val flavor = applied.mapNotNull { it.rule.expectFlavor }.distinct()
        val expect = buildString {
            append("You selected ${selection.sweetness.displayName} and ${joinNatural(verbs)}. ")
            append("$subject $sweetClause")
            if (texture.isNotEmpty()) append(" and may be ${joinNatural(texture)}")
            append(".")
            if (flavor.isNotEmpty()) append(" $pronoun will also have ${joinNatural(flavor)}.")
        }

        val replacedIds = applied.map { it.originalIngredient }.toSet()
        val boosters = (applied.flatMap { it.rule.flavorBoosters } + recipe.sweetness.less.suggestedAlternatives).distinct()
            .filter { id -> recipe.addIns.any { it.id == id } && id !in selection.addInIds }
            .mapNotNull { catalog.addIns[it] }
            .filter { def -> def.lines.none { it.ingredient in replacedIds } }
            .take(4)
            .map { lowerFirst(it.improveHint ?: it.sweetHint ?: it.name) }
        val improvement = if (boosters.isEmpty()) null
        else "For additional flavor without greatly increasing sweetness, try ${joinOr(boosters)}."

        val notes = base.importantNotes.toMutableList()
        notes.add(
            maxOf(0, notes.size - 1),
            "Substitutions can change how quickly a recipe browns and sets, so start checking for doneness a little early.",
        )
        return base.copy(
            recipeSummary = summary,
            substitutionSummary = applied.map { it.summary },
            substitutionExpect = expect,
            substitutionImprovement = improvement,
            importantNotes = notes,
            labelChecks = labelChecks,
        )
    }

    private fun joinOr(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        2 -> "${items[0]} or ${items[1]}"
        else -> items.dropLast(1).joinToString(", ") + ", or " + items.last()
    }

    private fun sweetnessConclusion(
        recipe: Recipe,
        catalog: RecipeCatalog,
        selection: RecipeSelection,
        ingredients: List<CalculatedIngredient>,
        addIns: List<AddInDef>,
        allergens: List<Allergen>,
        batch: BatchInfo?,
    ): Conclusion {
        val less = selection.sweetness == SweetnessType.LESS_SUGAR
        val profile = recipe.profileForSweetness(selection.sweetness)
        val allergenNotices = allergens.map { "Contains ${it.label}" }

        val notes = mutableListOf<String>()
        if (less) notes += "Reducing sugar can affect browning, texture and moisture, so your result may look or feel a little different."
        notes += "Baking times may vary depending on your appliance and portion size."
        notes += "Start checking a few minutes early."
        batch?.let { notes += "This amount needs ${it.batches} batches - the time per batch stays the same." }
        notes += recipe.conclusionNotes
        notes += RESULTS_VARY_NOTE

        if (!less) {
            return Conclusion(
                title = "About Your Normal Sugar Version",
                summary = "You selected Normal Sugar. This version uses the traditional sweetness level designed for this recipe.",
                recipeNote = profile.note,
                sugarChanges = emptyList(),
                whatToExpectTitle = "What to Expect",
                whatToExpect = listOf(
                    "The traditional sweetness for this recipe",
                    "Browning, moisture and texture as the recipe was designed",
                ),
                naturalSweetnessTitle = null,
                naturalSweetnessIdeas = emptyList(),
                closing = null,
                importantNotes = notes,
                allergenNotices = allergenNotices,
                allergenDisclaimer = ALLERGEN_DISCLAIMER,
                generalNote = RESULTS_VARY_NOTE,
            )
        }

        // Less Sugar: show exactly what changed for this recipe.
        val sugarChanges = recipe.ingredients.filter { it.role == IngredientRole.SUGAR }.map { line ->
            val def = catalog.ingredient(line.ingredient)
            val factor = scaleFactor(recipe, selection.flourCups)
            val less1 = displayText(def, line.unit, line.amount * factor * recipe.lessSugarMultiplier, line.rounding ?: def.rounding, selection.unitSystem).first
            val normal = displayText(def, line.unit, line.amount * factor * recipe.normalSugarMultiplier, line.rounding ?: def.rounding, selection.unitSystem).first
            "${def.name}: $less1 instead of $normal"
        }
        val percent = ((1 - recipe.lessSugarMultiplier / recipe.normalSugarMultiplier) * 100).roundToInt()
        val chosenSweet = addIns.filter { it.naturalSweet }.map { it.name.lowercase() }
        val summary = buildString {
            append("You selected Less Sugar. This recipe will taste less sweet than the traditional version. ")
            append("Depending on the recipe, reducing sugar may also slightly affect browning, moisture, softness or texture.")
            if (chosenSweet.isNotEmpty()) {
                append(" Your add-ins (${joinNatural(chosenSweet)}) will bring a little natural sweetness back.")
            }
        }
        val alternatives = profile.suggestedAlternatives
            .filter { id -> addIns.none { it.id == id } }
            .mapNotNull { catalog.addIns[it] }
        val ideas = alternatives.map { it.sweetHint ?: it.name }
        val closing = if (alternatives.isEmpty()) null else
            "If you prefer more natural sweetness, try adding ${joinNatural(alternatives.map { lowerFirst(it.sweetHint ?: it.name) })}."

        return Conclusion(
            title = "About Your Less Sugar Version",
            summary = summary,
            recipeNote = profile.note,
            sugarChanges = listOf("Sugar is reduced by about $percent% for this recipe.") + sugarChanges,
            whatToExpectTitle = "What to Expect",
            whatToExpect = listOf("Less sweet than the traditional version") + profile.expectedChanges +
                "Still designed to produce a good result",
            naturalSweetnessTitle = if (ideas.isEmpty()) null else "If You Want More Natural Sweetness",
            naturalSweetnessIdeas = ideas,
            closing = closing,
            importantNotes = notes,
            allergenNotices = allergenNotices,
            allergenDisclaimer = ALLERGEN_DISCLAIMER,
            generalNote = RESULTS_VARY_NOTE,
        )
    }
}
