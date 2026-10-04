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
)

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
)
