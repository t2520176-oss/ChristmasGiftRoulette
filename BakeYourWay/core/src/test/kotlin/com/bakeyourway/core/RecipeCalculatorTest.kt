package com.bakeyourway.core

import com.bakeyourway.core.TestSupport.calc
import com.bakeyourway.core.TestSupport.catalog
import com.bakeyourway.core.TestSupport.line
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RecipeCalculatorTest {
    private val muffins = "blueberry_muffins"
    private val less = SweetnessType.LESS_SUGAR
    private val normal = SweetnessType.NORMAL_SUGAR
    private val flourAmounts = listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0)

    // ---- Flour scaling (0.5 / 1 / 1.5 / 2 / 2.5 / 3 cups) ------------------------------------------

    @Test
    fun `every ingredient scales with the flour amount`() {
        for (cups in flourAmounts) {
            val r = calc(muffins, cups)
            assertEquals("scale factor at $cups", cups, r.scaleFactor, 1e-9)
            assertEquals("flour at $cups", cups, r.line("all_purpose_flour").cups!!, 1e-9)
            assertEquals("sugar at $cups", cups / 3, r.line("granulated_sugar").cups!!, 1e-6)
            assertEquals("milk at $cups", cups * 0.5, r.line("milk").cups!!, 1e-9)
            assertEquals("butter at $cups", cups * 0.25, r.line("butter").cups!!, 1e-9)
            assertEquals("baking powder tsp at $cups", cups * 1.5, r.line("baking_powder").amount, 1e-9)
            assertEquals("egg at $cups", cups, r.line("egg").amount, 1e-9)
        }
    }

    @Test
    fun `half cup flour`() {
        val r = calc(muffins, 0.5)
        assertEquals("1/2 cup (60 g)", r.line("all_purpose_flour").displayText)
        assertEquals("1/4 cup", r.line("milk").primaryText)
        assertEquals("2 tbsp", r.line("butter").primaryText)
        assertEquals("3/4 tsp", r.line("baking_powder").primaryText)
        assertEquals("about 3 muffins", r.yieldEstimate.text)
    }

    @Test
    fun `one cup flour is the base recipe`() {
        val r = calc(muffins, 1.0)
        assertEquals("1 cup (120 g)", r.line("all_purpose_flour").displayText)
        assertEquals("1/3 cup", r.line("granulated_sugar").primaryText)
        assertEquals("1/2 cup (120 ml)", r.line("milk").displayText)
        assertEquals("1/4 cup (55 g)", r.line("butter").displayText)
        assertEquals("1 large egg", r.line("egg").primaryText)
        assertEquals("1 1/2 tsp", r.line("baking_powder").primaryText)
        assertEquals("about 6 muffins", r.yieldEstimate.text)
    }

    @Test
    fun `one and a half cups flour`() {
        val r = calc(muffins, 1.5)
        assertEquals("1 1/2 cups (180 g)", r.line("all_purpose_flour").displayText)
        assertEquals("1/2 cup", r.line("granulated_sugar").primaryText)
        assertEquals("3/4 cup (180 ml)", r.line("milk").displayText)
        assertEquals("2 1/4 tsp", r.line("baking_powder").primaryText)
        assertEquals("1 large egg + about 1 1/2 tbsp beaten egg", r.line("egg").primaryText)
        assertTrue(r.warnings.any { it.contains("beaten egg") })
        assertEquals("about 9 muffins", r.yieldEstimate.text)
    }

    @Test
    fun `two cups flour matches the documented example`() {
        val r = calc(muffins, 2.0)
        assertEquals("2 cups (240 g)", r.line("all_purpose_flour").displayText)
        assertEquals("2/3 cup", r.line("granulated_sugar").primaryText)
        assertEquals("3 tsp", r.line("baking_powder").primaryText)
        assertEquals("1/2 tsp", r.line("salt").primaryText)
        assertEquals("2 large eggs", r.line("egg").primaryText)
        assertEquals("1 cup (240 ml)", r.line("milk").displayText)
        assertEquals("1/2 cup (115 g)", r.line("butter").displayText)
        assertEquals("2 tsp", r.line("vanilla_extract").primaryText)
        assertEquals("about 12 muffins", r.yieldEstimate.text)
    }

    @Test
    fun `two and a half cups flour`() {
        val r = calc(muffins, 2.5)
        assertEquals("2 1/2 cups (300 g)", r.line("all_purpose_flour").displayText)
        assertEquals("3 3/4 tsp", r.line("baking_powder").primaryText)
        assertEquals("2 large eggs + about 1 1/2 tbsp beaten egg", r.line("egg").primaryText)
        assertEquals("about 15 muffins", r.yieldEstimate.text)
    }

    @Test
    fun `three cups flour`() {
        val r = calc(muffins, 3.0)
        assertEquals("3 cups (360 g)", r.line("all_purpose_flour").displayText)
        assertEquals("1 cup", r.line("granulated_sugar").primaryText)
        assertEquals("3 large eggs", r.line("egg").primaryText)
        assertEquals("1 1/2 cups (360 ml)", r.line("milk").displayText)
        assertEquals("about 18 muffins", r.yieldEstimate.text)
    }

    @Test
    fun `custom decimal flour amounts scale correctly`() {
        for (cups in listOf(1.25, 1.75, 2.25, 0.75)) {
            val r = calc(muffins, cups)
            assertEquals(cups, r.line("all_purpose_flour").cups!!, 1e-9)
            assertEquals(cups * 0.5, r.line("milk").cups!!, 1e-9)
            assertFalse(r.allIngredients.any { it.primaryText.contains(Regex("\\d\\.\\d")) })
        }
        assertEquals("1 1/4 cups (150 g)", calc(muffins, 1.25).line("all_purpose_flour").displayText)
        assertEquals("1 3/4 cups (210 g)", calc(muffins, 1.75).line("all_purpose_flour").displayText)
    }

    @Test
    fun `scaling uses each recipe's own reference flour`() {
        // Brownies are written for 1/2 cup flour, bread for 3 cups of bread flour.
        val brownies = calc("classic_brownies", 1.0)
        assertEquals(2.0, brownies.scaleFactor, 1e-9)
        assertEquals(2.0 / 3, brownies.line("cocoa_powder").cups!!, 1e-9)
        assertEquals("4 large eggs", brownies.line("egg").primaryText)
        val bread = calc("basic_white_bread", 3.0)
        assertEquals(1.0, bread.scaleFactor, 1e-9)
        assertEquals("3 cups (380 g)", bread.line("bread_flour").displayText)
        val breadHalf = calc("basic_white_bread", 1.5)
        assertEquals(0.5, breadHalf.scaleFactor, 1e-9)
        assertEquals("1/2 cup (120 ml)", breadHalf.line("water").displayText)
    }

    // ---- Sweetness -----------------------------------------------------------------------------------

    @Test
    fun `normal sugar uses the traditional amount`() {
        val r = calc(muffins, 2.0, normal)
        assertEquals(2.0 / 3, r.line("granulated_sugar").cups!!, 1e-6)
        assertEquals("2/3 cup", r.line("granulated_sugar").primaryText)
    }

    @Test
    fun `less sugar uses the recipe's own reduction`() {
        val r = calc(muffins, 2.0, less)
        assertEquals(2.0 / 3 * 0.75, r.line("granulated_sugar").cups!!, 1e-6)
        assertEquals("1/2 cup", r.line("granulated_sugar").primaryText)
        // Only sugar changes - nothing else moves.
        val n = calc(muffins, 2.0, normal)
        for (ing in r.ingredients.filter { !it.isSugar }) {
            assertEquals(ing.ingredientId, n.ingredients.first { it.ingredientId == ing.ingredientId }.amount, ing.amount, 1e-12)
        }
    }

    @Test
    fun `different recipes reduce sugar by different amounts`() {
        fun ratio(id: String): Double {
            val r = catalog.recipe(id)!!
            val l = calc(id, r.baseFlourAmount, less, r.recommendedMethod!!).ingredients.first { it.isSugar }.amount
            val n = calc(id, r.baseFlourAmount, normal, r.recommendedMethod!!).ingredients.first { it.isSugar }.amount
            return l / n
        }
        assertEquals(0.75, ratio("blueberry_muffins"), 1e-9)
        assertEquals(0.70, ratio("banana_bread"), 1e-9)
        assertEquals(0.80, ratio("chocolate_chip_cookies"), 1e-9)
        assertEquals(0.85, ratio("sugar_cookies"), 1e-9)
        assertEquals(0.50, ratio("classic_pancakes"), 1e-9)
    }

    @Test
    fun `less sugar reduces every sugar line including swirls`() {
        val l = calc("cinnamon_bread", 2.0, less)
        val n = calc("cinnamon_bread", 2.0, normal)
        val sugars = l.ingredients.filter { it.isSugar }
        assertEquals(2, sugars.size)
        for (s in sugars) {
            val base = n.ingredients.first { it.ingredientId == s.ingredientId && it.name == s.name }
            assertEquals(base.amount * 0.75, s.amount, 1e-9)
        }
    }

    // ---- Units ----------------------------------------------------------------------------------------

    @Test
    fun `metric mode shows grams and ml and keeps spoons for small amounts`() {
        val r = calc(muffins, 2.0, system = UnitSystem.METRIC)
        assertEquals("240 g (2 cups)", r.line("all_purpose_flour").displayText)
        assertEquals("240 ml (1 cup)", r.line("milk").displayText)
        assertEquals("115 g (1/2 cup)", r.line("butter").displayText)
        assertEquals("135 g (2/3 cup)", r.line("granulated_sugar").displayText)
        assertEquals("3 tsp", r.line("baking_powder").displayText)
        assertEquals("2 large eggs", r.line("egg").displayText)
        assertEquals("240 g (2 cups)", r.flourText)
    }

    @Test
    fun `us mode lists grams in brackets`() {
        val r = calc(muffins, 2.0, system = UnitSystem.US_CUPS)
        assertEquals("2 cups (240 g)", r.flourText)
        assertEquals("2 cups (240 g)", r.line("all_purpose_flour").displayText)
    }

    @Test
    fun `grams are not the same for every ingredient`() {
        val r = calc(muffins, 1.0)
        val flour = r.line("all_purpose_flour").grams!!
        val sugarGrams = calc(muffins, 3.0).line("granulated_sugar").grams!! // 1 cup of sugar
        val butter = calc(muffins, 4.0).line("butter").grams!! // 1 cup of butter
        assertEquals(120.0, flour, 1e-9)
        assertEquals(200.0, sugarGrams, 1e-6)
        assertEquals(227.0, butter, 1e-9)
        assertNotEquals(flour, sugarGrams, 1.0)
        // A different flour has a different density, too.
        assertEquals(127.0, calc("basic_white_bread", 1.0).line("bread_flour").grams!!, 1e-9)
    }

    @Test
    fun `flour in grams converts with the recipe flour density`() {
        val bread = TestSupport.recipe("basic_white_bread")
        val density = catalog.ingredient(bread.flourIngredient).gramsPerCup!!
        val cups = FlourInput.toCups(254.0, UnitSystem.METRIC, density)
        assertEquals(2.0, cups, 1e-9)
        val r = RecipeCalculator.calculate(bread, catalog, TestSupport.select(cups, system = UnitSystem.METRIC))
        assertEquals("255 g (2 cups)", r.flourText)
    }

    // ---- Cooking is independent of flour ----------------------------------------------------------------

    @Test
    fun `cooking time and temperature never scale with flour`() {
        for (recipe in catalog.recipes) {
            for (method in recipe.supportedCookingMethods) {
                val profile = recipe.profileFor(method)!!
                for (cups in flourAmounts) {
                    val r = calc(recipe.id, cups, method = method)
                    assertEquals("${recipe.id}/$method time min @ $cups", profile.timeMin, r.cooking.timeMin)
                    assertEquals("${recipe.id}/$method time max @ $cups", profile.timeMax, r.cooking.timeMax)
                    assertEquals("${recipe.id}/$method temp @ $cups", profile.temperatureC, r.cooking.temperatureC)
                }
            }
        }
    }

    @Test
    fun `larger amounts make more portions and need more batches not longer cooking`() {
        val one = calc(muffins, 1.0)
        val three = calc(muffins, 3.0)
        assertEquals("18 muffins", three.yieldEstimate.label)
        assertEquals(one.cooking.timeText, three.cooking.timeText)
        assertEquals("18–22 minutes", three.cooking.timeText)
        assertEquals(null, one.batchInfo)
        assertEquals(2, three.batchInfo!!.batches)
        assertEquals(12, three.batchInfo!!.perBatch)
        assertTrue(three.batchInfo!!.note.contains("does not increase"))
        // Air fryers hold fewer, so even 1 cup of flour needs a second round.
        val air = calc(muffins, 1.0, method = CookingMethod.AIR_FRYER)
        assertEquals(2, air.batchInfo!!.batches)
        assertEquals("12–16 minutes", air.cooking.timeText)
    }

    @Test
    fun `cooking info for each method matches the recipe data`() {
        val oven = calc(muffins, 2.0, method = CookingMethod.OVEN)
        assertEquals("180°C / 350°F", oven.cooking.temperatureText)
        assertEquals("18–22 minutes", oven.cooking.timeText)
        val steam = calc(muffins, 2.0, method = CookingMethod.STEAM)
        assertEquals("Medium steam", steam.cooking.temperatureText)
        assertEquals("15–20 minutes", steam.cooking.timeText)
        val stove = calc("classic_pancakes", 2.0, method = CookingMethod.STOVETOP)
        assertEquals("Medium", stove.cooking.temperatureText)
        assertTrue(stove.cooking.heatLevel!!.isNotBlank())
    }

    @Test
    fun `comparison highlights only the selected method`() {
        val r = calc(muffins, 2.0, method = CookingMethod.AIR_FRYER)
        assertEquals(setOf(CookingMethod.OVEN, CookingMethod.AIR_FRYER, CookingMethod.STEAM), r.methodComparison.map { it.method }.toSet())
        assertEquals(listOf(CookingMethod.AIR_FRYER), r.methodComparison.filter { it.isSelected }.map { it.method })
    }

    @Test
    fun `unsupported cooking methods are refused`() {
        for ((id, method) in listOf("classic_pancakes" to CookingMethod.OVEN, "chocolate_chip_cookies" to CookingMethod.STEAM,
            "banana_bread" to CookingMethod.AIR_FRYER, "apple_pie" to CookingMethod.STOVETOP)) {
            try {
                calc(id, 1.0, method = method)
                fail("$id should not support $method")
            } catch (expected: IllegalArgumentException) {
                // ok
            }
        }
        assertEquals("This cooking method is not recommended for this recipe.", RecipeCalculator.UNSUPPORTED_METHOD_MESSAGE)
    }

    @Test
    fun `cooking disclaimer is always present`() {
        assertEquals("Cooking times are approximate. Appliances, pan size and portion size vary. Check for doneness before serving.",
            calc(muffins, 1.0).cookingDisclaimer)
    }

    // ---- Instructions change with the method --------------------------------------------------------------------

    @Test
    fun `instructions follow the selected cooking method`() {
        val oven = calc(muffins, 2.0, method = CookingMethod.OVEN).steps.joinToString("\n") { "${it.title}: ${it.text}" }
        val air = calc(muffins, 2.0, method = CookingMethod.AIR_FRYER).steps.joinToString("\n") { "${it.title}: ${it.text}" }
        val steam = calc(muffins, 2.0, method = CookingMethod.STEAM).steps.joinToString("\n") { "${it.title}: ${it.text}" }

        assertTrue(oven.contains("Preheat the oven to 180°C / 350°F"))
        assertTrue(oven.contains("muffin tin"))
        assertTrue(oven.contains("Bake at 180°C / 350°F for 18–22 minutes"))

        assertTrue(air.contains("Preheat the air fryer to 160°C / 320°F"))
        assertTrue(air.contains("Air fry at 160°C / 320°F for 12–16 minutes"))
        assertFalse("air fryer instructions must not mention the oven: $air", air.lowercase().contains("oven"))
        assertFalse(air.contains("muffin tin"))

        assertTrue(steam.contains("steamer"))
        assertTrue(steam.contains("15–20 minutes"))
        assertFalse(steam.lowercase().contains("oven"))
        assertFalse(steam.contains("Preheat"))
    }

    @Test
    fun `steps are numbered in order and end with a doneness check`() {
        val r = calc(muffins, 2.0)
        assertEquals((1..r.steps.size).toList(), r.steps.map { it.number })
        assertEquals("Preheat the Oven", r.steps.first().title)
        assertTrue(r.steps.any { it.title == "Check for Doneness" })
        assertTrue(r.steps.indexOfFirst { it.title == "Bake" } < r.steps.indexOfFirst { it.title == "Check for Doneness" })
    }

    @Test
    fun `yeast breads preheat just before baking and name the right pan`() {
        val r = calc("basic_white_bread", 3.0)
        val titles = r.steps.map { it.title }
        assertTrue(titles.indexOf("Second Rise") < titles.indexOf("Preheat the Oven"))
        assertTrue(titles.indexOf("Preheat the Oven") < titles.indexOf("Bake"))
        assertTrue(r.steps.first { it.title == "Shape" }.text.contains("9×5 in"))
        assertTrue(calc("basic_white_bread", 1.0).panGuidance!!.contains("mini loaf"))
        assertTrue(calc("basic_white_bread", 6.0).panGuidance!!.contains("two"))
    }

    @Test
    fun `stovetop pancakes heat the pan and cook in batches`() {
        val r = calc("classic_pancakes", 2.0, method = CookingMethod.STOVETOP)
        assertEquals("Heat the Pan", r.steps.first { it.title.startsWith("Heat") }.title)
        assertTrue(r.steps.last { it.title == "Cook" }.text.contains("medium heat"))
        assertEquals(4, r.batchInfo!!.batches) // 12 pancakes, 3 per pan
    }

    // ---- Add-ins ---------------------------------------------------------------------------------------------------

    @Test
    fun `no add-ins still gives a complete recipe`() {
        val r = calc(muffins, 2.0)
        assertTrue(r.addInIngredients.isEmpty())
        assertFalse(r.steps.any { it.title == "Add Optional Ingredients" })
        assertTrue(r.ingredients.isNotEmpty() && r.steps.size >= 5)
    }

    @Test
    fun `selected add-ins scale with flour and appear as optional lines and a step`() {
        val r = calc(muffins, 2.0, addIns = setOf("blueberries", "chocolate_chips"))
        val blue = r.addInIngredients.first { it.ingredientId == "blueberries" }
        assertTrue(blue.optional && blue.isAddIn)
        assertEquals("1 cup (150 g)", blue.displayText)
        assertEquals("1 cup (170 g)", r.addInIngredients.first { it.ingredientId == "chocolate_chips" }.displayText)
        val step = r.steps.first { it.title == "Add Optional Ingredients" }
        assertTrue(step.text, step.text.contains("blueberries (1 cup)") && step.text.contains("chocolate chips (1 cup)"))
        val big = calc(muffins, 3.0, addIns = setOf("blueberries"))
        assertEquals("1 1/2 cups (225 g)", big.addInIngredients.single().displayText)
        // Add-ins never change the flour-based scaling of the main recipe.
        assertEquals(calc(muffins, 3.0).ingredients.map { it.displayText }, big.ingredients.map { it.displayText })
    }

    @Test
    fun `unselected or unknown add-ins are ignored`() {
        val r = calc(muffins, 2.0, addIns = setOf("cheese", "nonexistent"))
        assertTrue(r.addInIngredients.isEmpty())
    }

    @Test
    fun `topping style add-ins are placed before cooking or after`() {
        val r = calc(muffins, 2.0, addIns = setOf("streusel", "chocolate_drizzle"))
        val titles = r.steps.map { it.title }
        assertTrue(titles.indexOf("Add Toppings") < titles.indexOf("Bake"))
        assertTrue(titles.indexOf("Finish") > titles.indexOf("Check for Doneness"))
        assertTrue(r.addInIngredients.any { it.ingredientId == "butter" && it.optional })
    }

    @Test
    fun `default add-ins are real add-ins of the recipe`() {
        val r = TestSupport.recipe(muffins)
        assertEquals(listOf("blueberries"), r.addIns.filter { it.default }.map { it.id })
    }

    // ---- Conclusion -------------------------------------------------------------------------------------------------

    @Test
    fun `less sugar conclusion is personalised`() {
        val c = calc(muffins, 2.0, less, addIns = setOf("blueberries")).conclusion
        assertEquals("About Your Less Sugar Version", c.title)
        assertTrue(c.summary.startsWith("You selected Less Sugar."))
        assertTrue(c.summary.contains("less sweet than the traditional version"))
        assertTrue(c.summary.contains("browning, moisture, softness or texture"))
        assertTrue(c.whatToExpect.first().contains("Less sweet"))
        assertTrue(c.whatToExpect.last().contains("good result"))
        assertEquals("If You Want More Natural Sweetness", c.naturalSweetnessTitle)
        assertTrue(c.naturalSweetnessIdeas.isNotEmpty())
        assertTrue(c.closing!!.contains("ripe") || c.closing!!.contains("banana"))
        assertTrue(c.sugarChanges.any { it.contains("2/3 cup") && it.contains("1/2 cup") })
        assertTrue(c.sugarChanges.first().contains("25%"))
        assertTrue(c.importantNotes.any { it.contains("ingredient brands, altitude, humidity, pan size and appliance") })
    }

    @Test
    fun `normal sugar conclusion is different`() {
        val c = calc(muffins, 2.0, normal).conclusion
        assertEquals("About Your Normal Sugar Version", c.title)
        assertEquals("You selected Normal Sugar. This version uses the traditional sweetness level designed for this recipe.", c.summary)
        assertTrue(c.naturalSweetnessIdeas.isEmpty())
        assertEquals(null, c.naturalSweetnessTitle)
        assertTrue(c.sugarChanges.isEmpty())
        assertEquals(RecipeCalculator.RESULTS_VARY_NOTE, c.generalNote)
    }

    @Test
    fun `natural sweetness ideas skip add-ins the user already chose`() {
        val without = calc("banana_muffins", 1.0, less).conclusion
        val with = calc("banana_muffins", 1.0, less, addIns = setOf("raisins")).conclusion
        assertTrue(without.closing!!.contains("raisins"))
        assertFalse(with.closing!!.contains("raisins"))
        assertTrue(with.summary.contains("raisins"))
    }

    @Test
    fun `natural sweetness suggestions only use add-ins suited to the recipe`() {
        for (r in catalog.recipes) {
            val ideas = r.sweetness.less.suggestedAlternatives
            assertTrue(r.id, ideas.all { id -> r.addIns.any { it.id == id } })
        }
        val savory = calc("cheddar_biscuits", 2.0, less).conclusion
        assertTrue(savory.naturalSweetnessIdeas.isEmpty())
    }

    // ---- Allergens ----------------------------------------------------------------------------------------------------

    @Test
    fun `allergen notices come from the ingredients`() {
        val r = calc(muffins, 1.0)
        assertEquals(listOf(Allergen.WHEAT, Allergen.EGG, Allergen.MILK), r.allergens)
        assertEquals(listOf("Contains wheat", "Contains egg", "Contains milk"), r.conclusion.allergenNotices)
        val nuts = calc(muffins, 1.0, addIns = setOf("nuts"))
        assertTrue(Allergen.TREE_NUTS in nuts.allergens)
        assertTrue(nuts.conclusion.allergenNotices.contains("Contains nuts"))
        assertTrue(Allergen.PEANUTS in calc("peanut_butter_cookies", 1.0).allergens)
        assertTrue(r.conclusion.allergenDisclaimer.contains("check the labels"))
    }

    // ---- Robustness over the whole catalog -----------------------------------------------------------------------------

    @Test
    fun `every recipe works for every method flour amount sweetness and unit system`() {
        var combos = 0
        for (recipe in catalog.recipes) {
            val allAddIns = recipe.addIns.map { it.id }.toSet()
            for (method in recipe.supportedCookingMethods) for (cups in flourAmounts + listOf(0.25, 1.25, 4.0, 6.0)) {
                for (sw in SweetnessType.entries) for (system in UnitSystem.entries) {
                    val r = calc(recipe.id, cups, sw, method, allAddIns, system)
                    combos++
                    val all = r.allIngredients.joinToString(" | ") { "${it.name} ${it.displayText}" } +
                        r.steps.joinToString(" | ") { it.text } + r.yieldEstimate.text + r.flourText
                    assertFalse("${recipe.id} $cups: NaN/Infinity in $all", all.contains("NaN") || all.contains("Infinity"))
                    assertFalse("${recipe.id}: unresolved placeholder in steps", r.steps.any { it.text.contains("{") })
                    assertFalse("${recipe.id}: unresolved placeholder in notes", r.methodTips.any { it.contains("{") })
                    assertTrue("${recipe.id}: blank ingredient text", r.allIngredients.none { it.primaryText.isBlank() })
                    assertTrue(r.yieldEstimate.count >= 1)
                    assertTrue(r.steps.size >= 5)
                    assertEquals(cups, r.line(recipe.flourIngredient).cups!!, 1e-9)
                    assertTrue("${recipe.id}: ${r.flourText}", r.flourText.isNotBlank())
                    assertFalse("${recipe.id}: decimal fraction in '$all'", Regex("\\b\\d+\\.\\d+ (cups?|tbsp|tsp|g|ml)\\b").containsMatchIn(all))
                }
            }
        }
        assertTrue("only $combos combinations tested", combos > 2000)
    }
}
