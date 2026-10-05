package com.bakeyourway.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------------------------

@Serializable
enum class RecipeCategory(val displayName: String, val tagline: String) {
    BREAD("Bread", "Loaves & rolls"),
    COOKIES("Cookies", "Soft, chewy & crisp"),
    MUFFINS("Muffins", "Fluffy and moist"),
    CUPCAKES("Cupcakes", "Little celebration cakes"),
    PIE("Pie", "Flaky crust, sweet filling"),
    PANCAKES("Pancakes", "Weekend breakfast"),
    BROWNIES("Brownies", "Fudgy chocolate squares"),
    SCONES("Scones", "Tender and buttery"),
    CAKES("Cakes", "Simple one-bowl cakes"),
    QUICK_BREADS("Quick Breads", "No yeast, no waiting"),
    BISCUITS("Biscuits", "Flaky and golden"),
    MORE("More Recipes", "Crepes, flatbread & more"),
}

@Serializable
enum class CookingMethod(val displayName: String) {
    OVEN("Oven"),
    AIR_FRYER("Air Fryer"),
    STEAM("Steam"),
    STOVETOP("Stovetop"),
}

@Serializable
enum class SweetnessType(val displayName: String, val subtitle: String) {
    LESS_SUGAR("Less Sugar", "Less sweet"),
    NORMAL_SUGAR("Normal Sugar", "Traditional"),
}

@Serializable
enum class MeasureUnit { CUP, TBSP, TSP, GRAM, ML, PIECE }

/** Which measuring system the user wants to read the ingredient list in. */
enum class UnitSystem(val displayName: String) {
    US_CUPS("Cups (US)"),
    METRIC("Grams (Metric)"),
}

/** How an ingredient is normally measured; drives the metric/US display rules. */
@Serializable
enum class MeasureStyle {
    /** Dry/solid ingredient: cups/spoons in US mode, grams in metric mode. */
    WEIGHT,

    /** Liquid: cups/spoons in US mode, millilitres in metric mode. */
    LIQUID,

    /** Small spoon amounts (leavening, spices, extracts): spoons in both modes. */
    SPOON,

    /** Counted items (eggs, whole fruit). */
    COUNT,
}

@Serializable
enum class RoundingRule {
    /** Practical kitchen fractions for the unit (1/8 cup, 1/2 tbsp, 1/4 tsp ...). */
    MEASURE,

    /** Whole eggs plus a spoonful of beaten egg for the remainder. */
    EGG,

    /** Nearest half item. */
    HALF,

    /** Nearest whole item (at least 1). */
    WHOLE,

    /** No rounding beyond two decimals. */
    NONE,
}

@Serializable
enum class IngredientRole {
    NORMAL,

    /** Sugar/sweetener: changed by the Less Sugar profile. */
    SUGAR,

    /** The reference flour of the recipe: everything scales relative to this line. */
    FLOUR,
}

@Serializable
enum class Allergen(val label: String) {
    WHEAT("wheat"),
    EGG("egg"),
    MILK("milk"),
    TREE_NUTS("nuts"),
    PEANUTS("peanuts"),
    SOY("soy"),
}

@Serializable
enum class AddInPlacement {
    /** Mixed or folded into the batter/dough. */
    FOLD_IN,

    /** Sprinkled or spread on top just before cooking. */
    BEFORE_COOK_TOP,

    /** Added after cooking (glazes, drizzles). */
    AFTER_COOK,
}

@Serializable
enum class StepKind {
    NORMAL,

    /** Marks where the "fold in your add-ins" step should be generated. */
    ADD_INS,

    /** Step that belongs after the cooking/doneness-check steps (cooling, serving). */
    AFTER_COOK,
}

/** What an ingredient does in a recipe; the substitution engine reasons with this. */
@Serializable
enum class IngredientFunction(val label: String) {
    FAT("Fat"),
    SWEETENER("Sweetener"),
    LEAVENER("Leavener"),
    BINDER("Binder"),
    LIQUID("Liquid"),
    STRUCTURE("Structure"),
    FLAVOR("Flavor"),
    MOISTURE("Moisture"),
    TOPPING("Topping"),
    OTHER("Other"),
}

/** How much a recipe depends on an ingredient. */
@Serializable
enum class IngredientImportance(val label: String) {
    NORMAL("Normal"),
    IMPORTANT("Important"),
    STRUCTURAL("Structural"),
    OPTIONAL("Optional"),
}

@Serializable
enum class PreheatTiming {
    /** Preheat is the first step (batters, cookies ...). */
    START,

    /** Preheat just before cooking (yeast doughs that rise first). */
    BEFORE_COOK,
}

// ---------------------------------------------------------------------------------------------
// Catalog data (ingredients, add-ins, toppings)
// ---------------------------------------------------------------------------------------------

@Serializable
data class IngredientDef(
    val id: String,
    val name: String,
    val style: MeasureStyle = MeasureStyle.WEIGHT,
    /** Ingredient-specific density. Never assume every ingredient weighs the same per cup. */
    val gramsPerCup: Double? = null,
    val gramsPerPiece: Double? = null,
    val pieceSingular: String? = null,
    val piecePlural: String? = null,
    val rounding: RoundingRule = RoundingRule.MEASURE,
    val allergens: List<Allergen> = emptyList(),
    /** Default role of this ingredient; a recipe line can override the importance. */
    val function: IngredientFunction = IngredientFunction.OTHER,
    val importance: IngredientImportance = IngredientImportance.NORMAL,
    /** How instructions refer to it ("flour" for all-purpose flour). Defaults to the lower-cased name. */
    val stepName: String? = null,
    /** Shown when this ingredient is used as a substitute, e.g. "check the label for soy, almond or oat". */
    val allergenHint: String? = null,
) {
    val noun: String get() = stepName ?: name.lowercase()
}

@Serializable
data class AddInLine(
    val ingredient: String,
    /** Amount per ONE cup of the user's flour amount. */
    val perFlourCup: Double,
    val unit: MeasureUnit,
    val prep: String? = null,
)

@Serializable
data class AddInDef(
    val id: String,
    val name: String,
    val emoji: String? = null,
    val placement: AddInPlacement = AddInPlacement.FOLD_IN,
    val lines: List<AddInLine>,
    /** Method-neutral sentence used for BEFORE_COOK_TOP / AFTER_COOK placements. */
    val instruction: String? = null,
    /** True for add-ins that add natural sweetness (suggested for the Less Sugar version). */
    val naturalSweet: Boolean = false,
    /** Short hint shown in "If you want more natural sweetness". */
    val sweetHint: String? = null,
    /** How the conclusion names this add-in when suggesting improvements ("extra blueberries"). */
    val improveHint: String? = null,
)

@Serializable
data class ToppingDef(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String? = null,
)

// ---------------------------------------------------------------------------------------------
// Recipe data
// ---------------------------------------------------------------------------------------------

@Serializable
data class IngredientLine(
    val ingredient: String,
    val amount: Double,
    val unit: MeasureUnit,
    val prep: String? = null,
    val role: IngredientRole = IngredientRole.NORMAL,
    /** False for amounts that should not follow the flour (rare). */
    val scalable: Boolean = true,
    val optional: Boolean = false,
    /** Optional heading in the ingredient list, e.g. "Crust" or "Filling". */
    val group: String? = null,
    val rounding: RoundingRule? = null,
    val note: String? = null,
    /** Overrides the ingredient's default importance for this recipe (e.g. eggs are structural in cakes). */
    val importance: IngredientImportance? = null,
    val function: IngredientFunction? = null,
)

@Serializable
data class YieldSpec(
    /** How many portions the BASE recipe (at the reference flour amount) makes. */
    val base: Double,
    val singular: String,
    val plural: String,
    val note: String? = null,
)

@Serializable
data class SweetnessProfile(
    val type: SweetnessType = SweetnessType.LESS_SUGAR,
    /** Multiplier applied to every SUGAR ingredient. 1.0 = traditional amount. */
    val multiplier: Double = 1.0,
    val note: String? = null,
    val expectedChanges: List<String> = emptyList(),
    /** Add-in ids that suit this recipe if the user wants more natural sweetness. */
    val suggestedAlternatives: List<String> = emptyList(),
)

@Serializable
data class SweetnessSet(
    val less: SweetnessProfile,
    val normal: SweetnessProfile? = null,
)

@Serializable
data class CookingProfile(
    val method: CookingMethod,
    val temperatureC: Int? = null,
    val temperatureF: Int? = null,
    val timeMin: Int,
    val timeMax: Int,
    val heatLevel: String? = null,
    val preheat: Boolean = false,
    val preheatMinutes: Int? = null,
    /** Vessel/pan preparation sentence (method specific). */
    val prep: String? = null,
    /** Cooking instruction. May contain {time}, {temp} and {heat} placeholders. */
    val cook: String,
    val check: String? = null,
    val notes: List<String> = emptyList(),
    /** How many portions fit in one batch with this method. Null = single pan/dish. */
    val batchCapacity: Int? = null,
)

@Serializable
data class RecipeStep(
    val title: String = "",
    val text: String = "",
    val kind: StepKind = StepKind.NORMAL,
    /** Empty = applies to every cooking method. */
    val methods: List<CookingMethod> = emptyList(),
    /**
     * Names the technique of this step (e.g. CREAM_FAT) so substitution rules can replace it safely.
     * Ingredient mentions inside [title] and [text] are tokens such as `{@butter|melted butter}`
     * which render the ingredient currently in use (see [StepText]).
     */
    val tag: String? = null,
)

@Serializable
data class RecipeAddIn(
    val id: String,
    val default: Boolean = false,
    /** Recipe-specific scale of the catalog amount (e.g. brownies take more nuts). */
    val amountMultiplier: Double = 1.0,
)

@Serializable
data class PanOption(
    /** Applies when the chosen flour amount (in cups) is at most this value. */
    val maxFlourCups: Double,
    val text: String,
)

@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val category: RecipeCategory,
    val description: String,
    val imageKey: String? = null,
    val baseFlourAmount: Double,
    val baseFlourUnit: MeasureUnit = MeasureUnit.CUP,
    val flourIngredient: String,
    val ingredients: List<IngredientLine>,
    @SerialName("yield") val yieldSpec: YieldSpec,
    val sweetness: SweetnessSet,
    /** Name of a shared cooking-profile set defined in the same recipe file. */
    val profileSet: String? = null,
    val profileOverrides: List<CookingProfile> = emptyList(),
    val excludeMethods: List<CookingMethod> = emptyList(),
    /** Filled in by [CatalogLoader] from [profileSet] + [profileOverrides]. */
    val cookingProfiles: List<CookingProfile> = emptyList(),
    val steps: List<RecipeStep>,
    val addIns: List<RecipeAddIn> = emptyList(),
    val toppingSuggestions: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val conclusionNotes: List<String> = emptyList(),
    val panOptions: List<PanOption> = emptyList(),
    val preheatTiming: PreheatTiming = PreheatTiming.START,
    val allergenTags: List<Allergen> = emptyList(),
) {
    val supportedCookingMethods: List<CookingMethod>
        get() = CookingMethod.entries.filter { m -> cookingProfiles.any { it.method == m } }

    fun profileFor(method: CookingMethod): CookingProfile? = cookingProfiles.firstOrNull { it.method == method }

    val normalSugarMultiplier: Double get() = sweetness.normal?.multiplier ?: 1.0
    val lessSugarMultiplier: Double get() = sweetness.less.multiplier
    val lessSugarNote: String? get() = sweetness.less.note

    /** The method suggested first (Oven when supported). */
    val recommendedMethod: CookingMethod?
        get() = supportedCookingMethods.firstOrNull()

    fun profileForSweetness(type: SweetnessType): SweetnessProfile = when (type) {
        SweetnessType.LESS_SUGAR -> sweetness.less
        SweetnessType.NORMAL_SUGAR -> sweetness.normal ?: SweetnessProfile(
            type = SweetnessType.NORMAL_SUGAR,
            multiplier = 1.0,
        )
    }
}

/** One JSON file in `assets/data/recipes/`. */
@Serializable
data class RecipeFile(
    val profileSets: Map<String, List<CookingProfile>> = emptyMap(),
    val recipes: List<Recipe>,
)

@Serializable
data class IngredientFile(val ingredients: List<IngredientDef>)

@Serializable
data class AddInFile(val addIns: List<AddInDef>)

@Serializable
data class ToppingFile(val toppings: List<ToppingDef>)

@Serializable
data class CatalogIndex(
    val ingredients: String,
    val addIns: String,
    val toppings: String,
    val recipeFiles: List<String>,
    /** Substitution rules (`ingredient_substitutions.json`). */
    val substitutions: String? = null,
    /** Ingredient names/aliases (`ingredient_aliases.json`). */
    val aliases: String? = null,
)

// ---------------------------------------------------------------------------------------------
// Ingredient substitutions
// ---------------------------------------------------------------------------------------------

@Serializable
enum class SubstituteConfidence(val label: String, val defaultRating: String) {
    RECOMMENDED("Recommended", "BEST MATCH"),
    ACCEPTABLE("Acceptable", "GOOD ALTERNATIVE"),
    LIMITED("Limited", "TEXTURE WILL CHANGE"),
    NOT_RECOMMENDED("Not recommended", "NOT RECOMMENDED"),
}

/** How a substitute amount is derived from the (already scaled) amount of the missing ingredient. */
@Serializable
enum class QuantityMode {
    /** ratio x the original volume in cups. */
    VOLUME_RATIO,

    /** ratio x the original weight in grams. */
    WEIGHT_RATIO,

    /** perPieceAmount (in perPieceUnit) for every counted piece of the original (eggs). */
    PER_PIECE,

    /** Whatever is left of the original volume after the other components (e.g. milk + lemon juice). */
    FILL_TO_TOTAL,
}

@Serializable
data class SubstituteComponent(
    val ingredient: String,
    val mode: QuantityMode = QuantityMode.VOLUME_RATIO,
    val ratio: Double = 1.0,
    val perPieceAmount: Double? = null,
    val perPieceUnit: MeasureUnit? = null,
    val prep: String? = null,
    /** Use the same preparation as the original line ("melted", "softened", "cold, cubed"). */
    val inheritPrep: Boolean = false,
    /** Name shown in the ingredient list instead of the catalog name (e.g. "Neutral oil"). */
    val label: String? = null,
)

@Serializable
data class ConversionRule(
    val components: List<SubstituteComponent> = emptyList(),
    /** Reduce the recipe's main liquid by this fraction of the original volume (honey and syrups add liquid). */
    val liquidReduction: Double? = null,
    /** Leave the ingredient out instead of replacing it. */
    val omit: Boolean = false,
)

@Serializable
enum class OverrideAction { REPLACE_STEP, PREPEND_STEP, APPEND_TO_STEP, REMOVE_STEP }

/** A structured change to the instructions that a substitution needs. Never a global text replace. */
@Serializable
data class InstructionOverride(
    val action: OverrideAction,
    /** The [RecipeStep.tag] this applies to (not needed for PREPEND_STEP). */
    val stepTag: String? = null,
    val title: String? = null,
    val text: String? = null,
    /** Restricts the override to these categories (empty = all). */
    val categories: List<RecipeCategory> = emptyList(),
)

@Serializable
data class IngredientSubstitution(
    val id: String,
    val originalIngredient: String,
    /** Display name of the substitute, e.g. "Neutral oil". */
    val substituteName: String,
    val conversionRule: ConversionRule = ConversionRule(),
    // Compatibility. A rule applies to a recipe whose category or id is listed as compatible; when both
    // compatible lists are empty it applies everywhere except what is listed as incompatible.
    val compatibleCategories: List<RecipeCategory> = emptyList(),
    val incompatibleCategories: List<RecipeCategory> = emptyList(),
    val compatibleRecipes: List<String> = emptyList(),
    val incompatibleRecipes: List<String> = emptyList(),
    val compatibleMethods: List<CookingMethod> = emptyList(),
    val incompatibleMethods: List<CookingMethod> = emptyList(),
    /** Largest amount of the original this rule is trusted for (pieces for eggs, otherwise cups). */
    val maxOriginalAmount: Double? = null,
    /** The same limit, per cup of flour the user chose. */
    val maxPerFlourCup: Double? = null,
    /** True for liquid substitutes that cannot replace a solid fat that must stay cold (cut-in, streusel). */
    val liquidSubstitute: Boolean = false,
    /** True when the swap only makes sense in the main recipe; add-ins that use the ingredient are removed instead. */
    val mainRecipeOnly: Boolean = false,
    /** True when the substitute is liquid and belongs with the wet ingredients, never in the dry mix (honey, syrup). */
    val wetSubstitute: Boolean = false,
    val effectOnTexture: String? = null,
    val effectOnFlavor: String? = null,
    val effectOnBrowning: String? = null,
    /** Short fragment for the conclusion: "slightly softer and moister" (fits "may be ..."). */
    val expectTexture: String? = null,
    /** Short fragment for the conclusion: "less buttery flavor" (fits "will have ..."). */
    val expectFlavor: String? = null,
    val specialInstructions: String? = null,
    val warning: String? = null,
    val tip: String? = null,
    val confidenceLevel: SubstituteConfidence,
    /** Overrides the rating label derived from the confidence level. */
    val ratingLabel: String? = null,
    /** How instructions refer to the substitute ("oil", or "baking soda" + "cream of tartar"). */
    val stepNouns: List<String> = emptyList(),
    val instructionOverrides: List<InstructionOverride> = emptyList(),
    /** Add-in ids that add flavor back, suggested in the conclusion. */
    val flavorBoosters: List<String> = emptyList(),
) {
    val substituteIngredient: String? get() = conversionRule.components.firstOrNull()?.ingredient
    val isOmission: Boolean get() = conversionRule.omit
    val isUsable: Boolean get() = confidenceLevel != SubstituteConfidence.NOT_RECOMMENDED
    val rating: String get() = ratingLabel ?: confidenceLevel.defaultRating
}

@Serializable
data class SubstitutionFile(val substitutions: List<IngredientSubstitution>)

@Serializable
data class AliasFile(
    /** Canonical ingredient id -> other names people use for it. */
    val aliases: Map<String, List<String>> = emptyMap(),
    /** Broad words ("sugar", "oil") -> every ingredient id they may mean. */
    val groups: Map<String, List<String>> = emptyMap(),
)
