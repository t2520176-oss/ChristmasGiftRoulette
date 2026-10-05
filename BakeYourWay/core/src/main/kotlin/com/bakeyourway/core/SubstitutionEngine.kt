package com.bakeyourway.core

// ---------------------------------------------------------------------------------------------
// Public types
// ---------------------------------------------------------------------------------------------

/** One substitution the user accepted: "this original ingredient is replaced using rule X". */
data class SubstitutionChoice(val originalIngredient: String, val substitutionId: String)

/** An ingredient the user does not have, as it appears in the CURRENT (scaled) recipe. */
data class MissingIngredient(
    val ingredientId: String,
    val name: String,
    val importance: IngredientImportance,
    val function: IngredientFunction,
    /** The calculated lines for this ingredient (main recipe and selected add-ins). */
    val lines: List<CalculatedIngredient>,
    /** Everything the recipe needs of it, e.g. "1/2 cup (115 g) butter" or "2 large eggs". */
    val originalText: String,
)

sealed interface MissingLookup {
    data object Blank : MissingLookup
    data class NotInRecipe(val text: String) : MissingLookup
    data class Found(val ingredient: MissingIngredient) : MissingLookup

    /** The text could mean several ingredients of this recipe ("sugar" with brown and white sugar). */
    data class Ambiguous(val choices: List<MissingIngredient>) : MissingLookup
}

data class SubstituteAmount(
    val ingredientId: String,
    val label: String,
    val amount: Double,
    val unit: MeasureUnit,
    val primaryText: String,
    val secondaryText: String?,
) {
    /** "6 tbsp (about 90 ml)" */
    val displayText: String get() = secondaryText?.let { "$primaryText (about $it)" } ?: primaryText
    val withName: String get() = "$displayText ${label.lowercase()}"
}

data class SubstitutionOption(
    val rule: IngredientSubstitution,
    val confidence: SubstituteConfidence,
    val rating: String,
    /** What to use, already calculated from the scaled recipe. Empty when the ingredient is simply left out. */
    val amounts: List<SubstituteAmount>,
    /** "6 tbsp (about 90 ml) neutral oil" */
    val useText: String,
    /** "Recommended for this muffin recipe." */
    val suitability: String,
    val warnings: List<String>,
    /** Add-ins (such as streusel) that cannot work with this substitute and would be removed. */
    val droppedAddIns: List<String>,
    /** "Milk is reduced by 2 tbsp because honey adds liquid." */
    val liquidAdjustment: String?,
)

data class NotRecommendedNote(val substituteName: String, val reason: String)

data class SubstitutionOptions(
    val missing: MissingIngredient,
    val options: List<SubstitutionOption>,
    /** Substitutes people often think of that we advise against for this recipe, with the reason. */
    val notRecommended: List<NotRecommendedNote>,
    /** Shown instead of options when nothing is reliable. */
    val message: String?,
    val structuralWarning: String?,
)

/** A substitution that was applied to a calculated recipe. */
data class AppliedSubstitution(
    val originalIngredient: String,
    val originalName: String,
    val rule: IngredientSubstitution,
    val originalText: String,
    val amounts: List<SubstituteAmount>,
    val useText: String,
    val droppedAddIns: List<String>,
    val liquidAdjustment: String?,
    val warnings: List<String>,
) {
    /** "Butter → Neutral oil" or "Vanilla → left out" */
    val summary: String get() = if (rule.isOmission) "$originalName → left out" else "$originalName → ${rule.substituteName}"
}

internal data class ResolvedOverride(val override: InstructionOverride, val placeholders: Map<String, String>)

internal data class ApplyResult(
    val ingredients: List<CalculatedIngredient>,
    val addInIngredients: List<CalculatedIngredient>,
    val addIns: List<AddInDef>,
    val applied: List<AppliedSubstitution>,
    val rejected: List<String>,
    val changes: Map<String, StepText.Change>,
    val overrides: List<ResolvedOverride>,
)

// ---------------------------------------------------------------------------------------------
// Engine
// ---------------------------------------------------------------------------------------------

/**
 * Finds and applies substitutions for the missing ingredients of a calculated recipe.
 *
 * Everything works from the locally stored rules in [SubstitutionLibrary] and from the CURRENT scaled
 * amounts of the recipe - never from the base recipe - so a recipe scaled to 2 cups of flour produces
 * substitute amounts for the doubled butter.
 */
object SubstitutionEngine {
    const val NO_RELIABLE_SUBSTITUTE = "No reliable substitute is currently recommended for this ingredient in this recipe."
    const val NOT_IN_RECIPE = "We couldn't find this ingredient in the current recipe."
    const val MANY_KEY_INGREDIENTS = "Replacing several key ingredients may significantly change the final texture and flavor."
    const val POSSIBLE_BUT_DIFFERENT = "Possible, but expect a different result."

    // ----- Lookup ---------------------------------------------------------------------------

    /** Describes an ingredient of the (baseline, unsubstituted) recipe, or null if it is not in it. */
    fun describe(baseline: CalculatedRecipe, catalog: RecipeCatalog, ingredientId: String): MissingIngredient? {
        val lines = baseline.allIngredients.filter { it.ingredientId == ingredientId }
        if (lines.isEmpty()) return null
        val def = catalog.ingredients[ingredientId] ?: return null
        val importance = lines.map { it.importance }.maxByOrNull { rank(it) } ?: def.importance
        return MissingIngredient(
            ingredientId = ingredientId,
            name = def.name,
            importance = importance,
            function = lines.first().function,
            lines = lines,
            originalText = totalText(def, lines, baseline.selection.unitSystem),
        )
    }

    /** Matches what the user typed ("butter", "AP flour", "cooking oil") to the recipe's ingredients. */
    fun lookup(text: String, baseline: CalculatedRecipe, catalog: RecipeCatalog): MissingLookup {
        if (text.isBlank()) return MissingLookup.Blank
        val ids = catalog.library.matchIngredients(text)
        val present = ids.mapNotNull { describe(baseline, catalog, it) }
        return when (present.size) {
            0 -> MissingLookup.NotInRecipe(text.trim())
            1 -> MissingLookup.Found(present.single())
            else -> MissingLookup.Ambiguous(present)
        }
    }

    /** Warns when several key (structural or important) ingredients are being replaced. */
    fun combinationWarning(missing: List<MissingIngredient>): String? =
        combinationWarningFor(missing.map { it.importance })

    fun combinationWarningFor(importances: List<IngredientImportance>): String? {
        val major = importances.count { it == IngredientImportance.STRUCTURAL || it == IngredientImportance.IMPORTANT }
        return if (major >= 2) MANY_KEY_INGREDIENTS else null
    }

    // ----- Options --------------------------------------------------------------------------

    /**
     * The suitable substitutes for [missing] in this recipe, best first. [allMissingIds] are all the
     * ingredients the user lacks, so a substitute never relies on another missing ingredient.
     */
    fun options(
        baseline: CalculatedRecipe,
        catalog: RecipeCatalog,
        missing: MissingIngredient,
        allMissingIds: Set<String> = setOf(missing.ingredientId),
    ): SubstitutionOptions {
        val ctx = Ctx(baseline, catalog, allMissingIds + missing.ingredientId)
        val structural = structuralWarning(missing)
        val options = mutableListOf<SubstitutionOption>()
        val notRecommended = mutableListOf<NotRecommendedNote>()

        for (rule in rulesFor(catalog, missing)) {
            if (!isCompatible(rule, ctx)) continue
            if (!rule.isUsable) {
                notRecommended += NotRecommendedNote(
                    rule.substituteName,
                    rule.warning ?: "We don't recommend this swap for this recipe.",
                )
                continue
            }
            val plan = plan(rule, ctx, missing) ?: continue
            options += toOption(plan, ctx, missing, structural)
        }
        val sorted = options.sortedBy { it.confidence.ordinal }
        // When nothing is reliable, say why (the rule's own explanation) rather than inventing an answer.
        val explanation = if (sorted.isEmpty()) notRecommended.firstOrNull { it.substituteName.isBlank() }
            ?: notRecommended.firstOrNull() else null
        val message = if (sorted.isEmpty()) explanation?.reason ?: NO_RELIABLE_SUBSTITUTE else null
        return SubstitutionOptions(
            missing = missing,
            options = sorted,
            notRecommended = notRecommended.filter { it.substituteName.isNotBlank() && it !== explanation },
            message = message,
            structuralWarning = structural,
        )
    }

    private fun structuralWarning(missing: MissingIngredient): String? =
        if (missing.importance == IngredientImportance.STRUCTURAL) {
            "Replacing ${missing.name.lowercase()} can noticeably change the texture and how this recipe rises or holds " +
                "together - this ingredient plays a structural role."
        } else null

    /** The catalog rules for this ingredient plus the automatic "leave it out" for optional ingredients. */
    private fun rulesFor(catalog: RecipeCatalog, missing: MissingIngredient): List<IngredientSubstitution> {
        val rules = catalog.library.rulesFor(missing.ingredientId)
        val hasOmit = rules.any { it.isOmission && it.isUsable }
        val allOptional = missing.lines.all { it.importance == IngredientImportance.OPTIONAL }
        if (!hasOmit && allOptional) {
            return rules + IngredientSubstitution(
                id = "omit_${missing.ingredientId}",
                originalIngredient = missing.ingredientId,
                substituteName = "Leave it out",
                conversionRule = ConversionRule(omit = true),
                effectOnTexture = "Barely changes the texture.",
                effectOnFlavor = "The recipe simply won't have this optional ingredient.",
                expectFlavor = "a plainer flavor",
                tip = "This ingredient is optional, so the recipe works well without it.",
                confidenceLevel = SubstituteConfidence.ACCEPTABLE,
                ratingLabel = "OPTIONAL - CAN SKIP",
            )
        }
        return rules
    }

    // ----- Applying -------------------------------------------------------------------------

    internal fun apply(
        baseline: CalculatedRecipe,
        catalog: RecipeCatalog,
        selectedAddIns: List<AddInDef>,
        choices: List<SubstitutionChoice>,
    ): ApplyResult {
        val latest = LinkedHashMap<String, SubstitutionChoice>()
        choices.forEach { latest[it.originalIngredient] = it }
        val ctx = Ctx(baseline, catalog, latest.keys)

        val main: MutableList<List<CalculatedIngredient>> = baseline.ingredients.map { listOf(it) }.toMutableList()
        val addIn: MutableList<List<CalculatedIngredient>> = baseline.addInIngredients.map { listOf(it) }.toMutableList()
        val applied = mutableListOf<AppliedSubstitution>()
        val rejected = mutableListOf<String>()
        val changes = LinkedHashMap<String, StepText.Change>()
        val overrides = mutableListOf<ResolvedOverride>()
        val droppedAddInIds = mutableSetOf<String>()
        val reductions = mutableListOf<Pair<Reduction, String>>()

        for (choice in latest.values) {
            val missing = describe(baseline, catalog, choice.originalIngredient)
            val rule = catalog.library.rule(choice.substitutionId)
                ?: missing?.let { m -> rulesFor(catalog, m).firstOrNull { it.id == choice.substitutionId } }
            if (missing == null || rule == null || rule.originalIngredient != missing.ingredientId ||
                !rule.isUsable || !isCompatible(rule, ctx)
            ) {
                rejected += choice.substitutionId
                continue
            }
            val plan = plan(rule, ctx, missing)
            if (plan == null) {
                rejected += choice.substitutionId
                continue
            }
            plan.mainReplacements.forEach { (i, lines) -> main[i] = lines }
            plan.addInReplacements.forEach { (i, lines) -> addIn[i] = lines }
            droppedAddInIds += plan.droppedAddInIds
            plan.reduction?.let { reductions += it to rule.substituteName }

            val option = toOption(plan, ctx, missing, structuralWarning(missing))
            applied += AppliedSubstitution(
                originalIngredient = missing.ingredientId,
                originalName = missing.name,
                rule = rule,
                originalText = missing.originalText,
                amounts = option.amounts,
                useText = option.useText,
                droppedAddIns = option.droppedAddIns,
                liquidAdjustment = option.liquidAdjustment,
                warnings = option.warnings,
            )

            val nouns = when {
                rule.isOmission -> emptyList()
                rule.stepNouns.isNotEmpty() -> rule.stepNouns
                else -> rule.conversionRule.components.mapNotNull { catalog.ingredients[it.ingredient]?.noun }
            }
            val originalPrep = missing.lines.firstNotNullOfOrNull { it.prep }
            changes[missing.ingredientId] = StepText.Change(
                nouns = nouns,
                inheritAdjective = rule.conversionRule.components.firstOrNull()?.inheritPrep ?: false,
                omitted = rule.isOmission,
                originalAdjective = StepText.adjectiveOf(originalPrep),
                dropFromDry = rule.wetSubstitute,
            )
            val placeholders = placeholders(rule, option, missing, catalog)
            rule.instructionOverrides.forEach { overrides += ResolvedOverride(it, placeholders) }
        }

        // Liquid reductions (honey and syrups add liquid) are applied to the recipe's main liquid.
        for ((reduction, substituteName) in reductions) {
            val current = main[reduction.index].singleOrNull() ?: continue
            main[reduction.index] = listOf(reduce(current, reduction, substituteName, ctx))
        }

        val outMain = main.flatten()
        val outAddIn = addIn.flatten().filter { it.addInId !in droppedAddInIds }
        val remaining = outAddIn.mapNotNull { it.addInId }.toSet()
        val effective = selectedAddIns.filter { it.id !in droppedAddInIds && it.id in remaining }
        return ApplyResult(outMain, outAddIn, effective, applied, rejected, changes, overrides)
    }

    private fun placeholders(
        rule: IngredientSubstitution,
        option: SubstitutionOption,
        missing: MissingIngredient,
        catalog: RecipeCatalog,
    ): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        val nouns = if (rule.stepNouns.isNotEmpty()) rule.stepNouns
        else rule.conversionRule.components.mapNotNull { catalog.ingredients[it.ingredient]?.noun }
        map["sub"] = StepText.joinNatural(nouns).ifEmpty { rule.substituteName.lowercase() }
        map["Sub"] = StepText.titleCase(map["sub"] ?: "")
        map["subName"] = rule.substituteName
        map["orig"] = catalog.ingredients[missing.ingredientId]?.noun ?: missing.name.lowercase()
        map["origName"] = missing.name
        map["origAmt"] = missing.originalText
        option.amounts.firstOrNull()?.let { map["amt"] = it.primaryText }
        option.amounts.forEach { map["amt:${it.ingredientId}"] = it.primaryText }
        return map
    }

    // ----- Compatibility --------------------------------------------------------------------

    private class Ctx(val baseline: CalculatedRecipe, val catalog: RecipeCatalog, val missingIds: Set<String>) {
        val recipe: Recipe get() = baseline.recipe
        val flourCups: Double get() = baseline.flourCups
        val system: UnitSystem get() = baseline.selection.unitSystem
        val method: CookingMethod get() = baseline.selection.method
    }

    /** Category / recipe / cooking-method compatibility of a rule. */
    private fun isCompatible(rule: IngredientSubstitution, ctx: Ctx): Boolean {
        val recipe = ctx.recipe
        if (recipe.id in rule.incompatibleRecipes) return false
        if (recipe.category in rule.incompatibleCategories) return false
        if (ctx.method in rule.incompatibleMethods) return false
        if (rule.compatibleMethods.isNotEmpty() && ctx.method !in rule.compatibleMethods) return false
        if (rule.compatibleCategories.isNotEmpty() || rule.compatibleRecipes.isNotEmpty()) {
            if (recipe.category !in rule.compatibleCategories && recipe.id !in rule.compatibleRecipes) return false
        }
        return true
    }

    // ----- Planning (amounts come from the scaled lines) ------------------------------------

    private class Reduction(val index: Int, val cups: Double)

    private class Plan(
        val rule: IngredientSubstitution,
        val mainReplacements: Map<Int, List<CalculatedIngredient>>,
        val addInReplacements: Map<Int, List<CalculatedIngredient>>,
        val droppedAddInIds: Set<String>,
        val droppedAddInNames: List<String>,
        val amounts: List<SubstituteAmount>,
        val reduction: Reduction?,
    )

    private fun plan(rule: IngredientSubstitution, ctx: Ctx, missing: MissingIngredient): Plan? {
        val catalog = ctx.catalog
        val conv = rule.conversionRule
        if (!conv.omit && conv.components.isEmpty()) return null
        // A substitute must not depend on another ingredient the user is also missing.
        if (conv.components.any { it.ingredient in ctx.missingIds || catalog.ingredients[it.ingredient] == null }) return null

        val mainLines = ctx.baseline.ingredients
        val addInLines = ctx.baseline.addInIngredients
        val mainIdx = mainLines.indices.filter { mainLines[it].ingredientId == missing.ingredientId }
        val addInIdx = addInLines.indices.filter { addInLines[it].ingredientId == missing.ingredientId }

        // Liquid substitutes cannot replace a solid fat that must stay cold (cutting in, streusel).
        val dropped = mutableSetOf<String>()
        val droppedNames = mutableListOf<String>()
        val keptMain = mutableListOf<Int>()
        val keptAddIn = mutableListOf<Int>()
        for (i in mainIdx) {
            if (needsSolid(mainLines[i]) && rule.liquidSubstitute) return null
            keptMain += i
        }
        for (i in addInIdx) {
            val line = addInLines[i]
            if ((needsSolid(line) && rule.liquidSubstitute) || rule.mainRecipeOnly) {
                line.addInId?.let { dropped += it }
                line.note?.let { if (it !in droppedNames) droppedNames += it }
            } else keptAddIn += i
        }
        if (keptMain.isEmpty() && keptAddIn.isEmpty()) return null
        val kept = keptMain.map { mainLines[it] } + keptAddIn.map { addInLines[it] }

        if (conv.omit && kept.any { it.importance == IngredientImportance.STRUCTURAL }) return null

        // Quantity limits: the same rule is only trusted up to a certain amount.
        val total = originalAmount(kept)
        rule.maxOriginalAmount?.let { if (total > it + 1e-9) return null }
        rule.maxPerFlourCup?.let { if (total / ctx.flourCups > it + 1e-9) return null }

        // Substitute lines for every original line.
        val mainRepl = LinkedHashMap<Int, List<CalculatedIngredient>>()
        val addInRepl = LinkedHashMap<Int, List<CalculatedIngredient>>()
        val totals = LinkedHashMap<Int, Double>()
        for ((list, repl, indices) in listOf(
            Triple(mainLines, mainRepl, keptMain),
            Triple(addInLines, addInRepl, keptAddIn),
        )) {
            for (i in indices) {
                val subs = if (conv.omit) emptyList() else makeLines(rule, ctx, list[i]) ?: return null
                repl[i] = subs
                subs.forEachIndexed { c, line -> totals[c] = (totals[c] ?: 0.0) + line.amount }
            }
        }

        val amounts = if (conv.omit) emptyList() else conv.components.mapIndexed { c, comp ->
            val def = catalog.ingredient(comp.ingredient)
            val sample = (mainRepl.values + addInRepl.values).first()[c]
            val amount = totals[c] ?: 0.0
            val (primary, secondary) = RecipeCalculator.displayText(def, sample.unit, amount, def.rounding, ctx.system, fine = true)
            SubstituteAmount(comp.ingredient, comp.label ?: (if (conv.components.size == 1) rule.substituteName else def.name), amount, sample.unit, primary, secondary)
        }

        // Honey and syrups add liquid: take it out of the recipe's main liquid.
        var reduction: Reduction? = null
        conv.liquidReduction?.let { fraction ->
            val cups = fraction * kept.sumOf { it.cups ?: 0.0 }
            if (cups >= 1.0 / MeasureFormatter.TSP_PER_CUP - 1e-9) {
                val idx = mainLines.indices.firstOrNull { i ->
                    val l = mainLines[i]
                    l.function == IngredientFunction.LIQUID && l.ingredientId !in ctx.missingIds && l.unit != MeasureUnit.PIECE && l.cups != null
                } ?: return null
                if ((mainLines[idx].cups ?: 0.0) < cups * 2) return null
                reduction = Reduction(idx, cups)
            }
        }
        return Plan(rule, mainRepl, addInRepl, dropped, droppedNames, amounts, reduction)
    }

    private fun needsSolid(line: CalculatedIngredient): Boolean {
        val adj = StepText.adjectiveOf(line.prep)?.lowercase() ?: return false
        return adj == "cold" || adj == "very cold"
    }

    private fun originalAmount(lines: List<CalculatedIngredient>): Double =
        lines.sumOf { if (it.unit == MeasureUnit.PIECE) it.amount else (it.cups ?: 0.0) }

    private fun makeLines(rule: IngredientSubstitution, ctx: Ctx, line: CalculatedIngredient): List<CalculatedIngredient>? {
        val comps = rule.conversionRule.components
        val originalCups = line.cups
        val fixedCups = comps.filter { it.mode == QuantityMode.VOLUME_RATIO }
            .sumOf { (originalCups ?: return null) * it.ratio }
        val originalName = ctx.catalog.ingredients[line.ingredientId]?.name?.lowercase() ?: line.name.lowercase()
        return comps.map { c ->
            val def = ctx.catalog.ingredient(c.ingredient)
            val (amount, unit) = when (c.mode) {
                QuantityMode.VOLUME_RATIO -> ((originalCups ?: return null) * c.ratio) to MeasureUnit.CUP
                QuantityMode.WEIGHT_RATIO -> ((line.grams ?: return null) * c.ratio) to MeasureUnit.GRAM
                QuantityMode.PER_PIECE -> {
                    if (line.unit != MeasureUnit.PIECE) return null
                    (line.amount * (c.perPieceAmount ?: return null)) to (c.perPieceUnit ?: return null)
                }
                QuantityMode.FILL_TO_TOTAL -> {
                    val rest = (originalCups ?: return null) - fixedCups
                    if (rest <= 0.0) return null
                    rest to MeasureUnit.CUP
                }
            }
            val prep = c.prep ?: if (c.inheritPrep) line.prep else null
            val synthetic = IngredientLine(
                ingredient = def.id,
                amount = amount,
                unit = unit,
                prep = prep,
                role = if (line.isSugar) IngredientRole.SUGAR else IngredientRole.NORMAL,
                optional = line.optional,
                group = line.group,
                note = line.note,
                importance = line.importance,
                function = line.function,
            )
            val label = c.label ?: if (comps.size == 1) rule.substituteName else null
            RecipeCalculator.buildIngredient(
                def, synthetic, amount, ctx.system,
                addIn = line.isAddIn, addInId = line.addInId, label = label, fine = true,
            ).copy(substitutedFor = originalName, substitutionId = rule.id)
        }
    }

    private fun reduce(line: CalculatedIngredient, reduction: Reduction, substituteName: String, ctx: Ctx): CalculatedIngredient {
        val def = ctx.catalog.ingredient(line.ingredientId)
        val newCups = (line.cups ?: return line) - reduction.cups
        val (primary, secondary) = RecipeCalculator.displayText(def, MeasureUnit.CUP, newCups, def.rounding, ctx.system, fine = true)
        val cutText = MeasureFormatter.formatVolume(reduction.cups * MeasureFormatter.TSP_PER_CUP, fine = true)
        return line.copy(
            amount = newCups,
            unit = MeasureUnit.CUP,
            cups = newCups,
            grams = MeasureFormatter.toGrams(newCups, MeasureUnit.CUP, def),
            primaryText = primary,
            secondaryText = secondary,
            adjustment = "reduced by $cutText because ${substituteName.lowercase()} adds liquid",
        )
    }

    // ----- Presentation helpers -------------------------------------------------------------

    private fun toOption(plan: Plan, ctx: Ctx, missing: MissingIngredient, structural: String?): SubstitutionOption {
        val rule = plan.rule
        val noun = categoryNoun(ctx.recipe.category)
        val suitability = when {
            rule.isOmission -> "This ingredient can be left out of this $noun recipe."
            rule.confidenceLevel == SubstituteConfidence.RECOMMENDED -> "Recommended for this $noun recipe."
            rule.confidenceLevel == SubstituteConfidence.ACCEPTABLE -> "A good alternative for this $noun recipe."
            else -> POSSIBLE_BUT_DIFFERENT
        }
        val adjustment = plan.reduction?.let { r ->
            val line = ctx.baseline.ingredients[r.index]
            val cut = MeasureFormatter.formatVolume(r.cups * MeasureFormatter.TSP_PER_CUP, fine = true)
            "${line.name.substringBefore(',')} is reduced by $cut because ${rule.substituteName.lowercase()} adds liquid."
        }
        val warnings = listOfNotNull(
            structural,
            rule.warning,
            if (plan.droppedAddInNames.isNotEmpty())
                "${StepText.joinNatural(plan.droppedAddInNames)} would be removed: this swap does not suit it."
            else null,
        )
        val useText = if (rule.isOmission) "Leave it out" else plan.amounts.joinToString(" + ") { it.withName }
        return SubstitutionOption(
            rule = rule,
            confidence = rule.confidenceLevel,
            rating = rule.rating,
            amounts = plan.amounts,
            useText = useText,
            suitability = suitability,
            warnings = warnings,
            droppedAddIns = plan.droppedAddInNames,
            liquidAdjustment = adjustment,
        )
    }

    private fun totalText(def: IngredientDef, lines: List<CalculatedIngredient>, system: UnitSystem): String {
        if (lines.all { it.unit == MeasureUnit.PIECE }) {
            val pieces = lines.sumOf { it.amount }
            return RecipeCalculator.displayText(def, MeasureUnit.PIECE, pieces, def.rounding, system).first
        }
        val cups = lines.sumOf { it.cups ?: 0.0 }
        val (primary, secondary) = RecipeCalculator.displayText(def, MeasureUnit.CUP, cups, def.rounding, system, fine = true)
        val amount = if (secondary == null) primary else "$primary ($secondary)"
        return "$amount ${def.noun}"
    }

    private fun categoryNoun(category: RecipeCategory): String = when (category) {
        RecipeCategory.BREAD -> "bread"
        RecipeCategory.COOKIES -> "cookie"
        RecipeCategory.MUFFINS -> "muffin"
        RecipeCategory.CUPCAKES -> "cupcake"
        RecipeCategory.PIE -> "pie"
        RecipeCategory.PANCAKES -> "pancake"
        RecipeCategory.BROWNIES -> "brownie"
        RecipeCategory.SCONES -> "scone"
        RecipeCategory.CAKES -> "cake"
        RecipeCategory.QUICK_BREADS -> "quick bread"
        RecipeCategory.BISCUITS -> "biscuit"
        RecipeCategory.MORE -> "recipe"
    }

    private fun rank(i: IngredientImportance): Int = when (i) {
        IngredientImportance.STRUCTURAL -> 3
        IngredientImportance.IMPORTANT -> 2
        IngredientImportance.NORMAL -> 1
        IngredientImportance.OPTIONAL -> 0
    }
}
