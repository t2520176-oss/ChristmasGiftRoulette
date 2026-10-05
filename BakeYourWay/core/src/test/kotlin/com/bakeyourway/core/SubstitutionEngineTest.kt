package com.bakeyourway.core

import com.bakeyourway.core.TestSupport.catalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubstitutionEngineTest {
    private val less = SweetnessType.LESS_SUGAR
    private val normal = SweetnessType.NORMAL_SUGAR

    // ---- helpers ---------------------------------------------------------------------------------------

    private fun baseline(
        id: String, cups: Double, sweetness: SweetnessType = normal, adds: Set<String> = emptySet(),
        system: UnitSystem = UnitSystem.US_CUPS, cat: RecipeCatalog = catalog,
    ): CalculatedRecipe {
        val r = cat.recipe(id)!!
        return RecipeCalculator.calculateBaseline(r, cat, RecipeSelection(cups, sweetness, r.recommendedMethod!!, adds, system))
    }

    private fun found(b: CalculatedRecipe, text: String, cat: RecipeCatalog = catalog): MissingIngredient =
        (SubstitutionEngine.lookup(text, b, cat) as? MissingLookup.Found)?.ingredient ?: error("'$text' not found in ${b.recipe.id}")

    private fun options(b: CalculatedRecipe, text: String, all: Set<String> = emptySet(), cat: RecipeCatalog = catalog): SubstitutionOptions {
        val m = found(b, text, cat)
        return SubstitutionEngine.options(b, cat, m, all + m.ingredientId)
    }

    private fun SubstitutionOptions.option(ruleId: String) = options.first { it.rule.id == ruleId }

    private fun applied(
        id: String, cups: Double, subs: List<SubstitutionChoice>, sweetness: SweetnessType = normal,
        adds: Set<String> = emptySet(), system: UnitSystem = UnitSystem.US_CUPS, cat: RecipeCatalog = catalog,
    ): CalculatedRecipe {
        val r = cat.recipe(id)!!
        return RecipeCalculator.calculate(r, cat, RecipeSelection(cups, sweetness, r.recommendedMethod!!, adds, system, subs))
    }

    private fun choice(original: String, rule: String) = SubstitutionChoice(original, rule)

    private fun stepsText(c: CalculatedRecipe) = c.steps.joinToString("\n") { "${it.title}: ${it.text}" }

    /** A catalog whose only recipe is a copy of [base] with extra/changed ingredient lines. */
    private fun syntheticCatalog(base: String, lines: List<IngredientLine>, flourIngredient: String? = null): RecipeCatalog {
        val r = catalog.recipe(base)!!
        val modified = r.copy(
            ingredients = if (flourIngredient != null) {
                r.ingredients.map { if (it.role == IngredientRole.FLOUR) it.copy(ingredient = flourIngredient) else it } + lines
            } else r.ingredients + lines,
            flourIngredient = flourIngredient ?: r.flourIngredient,
        )
        return RecipeCatalog(catalog.ingredients, catalog.addIns, catalog.toppings, listOf(modified), catalog.library)
    }

    // ---- ingredient names and aliases -----------------------------------------------------------------

    @Test
    fun `ingredient names match regardless of case spacing and common variations`() {
        val b = baseline("blueberry_muffins", 2.0)
        for (text in listOf("butter", "Butter", "BUTTER", "  butter  ", "unsalted butter", "Unsalted Butter", "salted butter")) {
            assertEquals(text, "butter", found(b, text).ingredientId)
        }
        for (text in listOf("all purpose flour", "all-purpose flour", "All-Purpose Flour", "AP flour", "ap flour", "plain flour", "flour")) {
            assertEquals(text, "all_purpose_flour", found(b, text).ingredientId)
        }
        for (text in listOf("egg", "eggs", "Large Eggs", "whole eggs")) assertEquals(text, "egg", found(b, text).ingredientId)
        assertEquals("milk", found(b, "whole milk").ingredientId)
        assertEquals("vanilla_extract", found(b, "pure vanilla extract").ingredientId)
        assertEquals("baking_powder", found(b, "baking-powder").ingredientId)
        assertEquals("granulated_sugar", found(b, "white sugar").ingredientId)
        assertEquals("granulated_sugar", found(b, "caster sugar").ingredientId)
    }

    @Test
    fun `oil and soda aliases work`() {
        val cake = baseline("chocolate_cake", 1.5)
        for (text in listOf("vegetable oil", "cooking oil", "canola oil", "neutral oil", "oil", "Sunflower Oil")) {
            assertEquals(text, "vegetable_oil", found(cake, text).ingredientId)
        }
        assertEquals("baking_soda", found(cake, "bicarbonate of soda").ingredientId)
        assertEquals("baking_soda", found(cake, "bicarb").ingredientId)
        assertEquals("cocoa_powder", found(cake, "cocoa").ingredientId)
    }

    @Test
    fun `an ingredient that is not in the recipe says so`() {
        val b = baseline("blueberry_muffins", 2.0)
        for (text in listOf("zebra", "cocoa", "honey", "icing sugar", "confectioners sugar", "powdered sugar", "cake flour", "sour cream")) {
            val r = SubstitutionEngine.lookup(text, b, catalog)
            assertTrue("$text -> $r", r is MissingLookup.NotInRecipe)
        }
        assertEquals("We couldn't find this ingredient in the current recipe.", SubstitutionEngine.NOT_IN_RECIPE)
        assertTrue(SubstitutionEngine.lookup("   ", b, catalog) is MissingLookup.Blank)
    }

    @Test
    fun `icing sugar and confectioners sugar find powdered sugar when the recipe has it`() {
        val cat = syntheticCatalog("blueberry_muffins", listOf(IngredientLine("powdered_sugar", 0.25, MeasureUnit.CUP, role = IngredientRole.SUGAR)))
        val b = baseline("blueberry_muffins", 1.0, cat = cat)
        for (text in listOf("icing sugar", "confectioners sugar", "Confectioners' Sugar", "powdered sugar")) {
            assertEquals(text, "powdered_sugar", found(b, text, cat).ingredientId)
        }
    }

    @Test
    fun `a broad word that fits several ingredients asks which one`() {
        val cookies = baseline("chocolate_chip_cookies", 1.0)
        val r = SubstitutionEngine.lookup("sugar", cookies, catalog) as MissingLookup.Ambiguous
        assertEquals(setOf("granulated_sugar", "brown_sugar"), r.choices.map { it.ingredientId }.toSet())
        // ... but is unambiguous when only one sugar is in the recipe.
        assertEquals("granulated_sugar", found(baseline("blueberry_muffins", 1.0), "sugar").ingredientId)
        assertEquals("brown_sugar", found(baseline("banana_muffins", 1.0), "sugar").ingredientId)
    }

    // ---- ingredient roles ---------------------------------------------------------------------------------

    @Test
    fun `ingredients know their function and importance`() {
        val m = baseline("blueberry_muffins", 2.0, adds = setOf("chocolate_chips"))
        fun line(id: String) = m.allIngredients.first { it.ingredientId == id }
        assertEquals(IngredientImportance.STRUCTURAL, line("all_purpose_flour").importance)
        assertEquals(IngredientFunction.STRUCTURE, line("all_purpose_flour").function)
        assertEquals(IngredientImportance.IMPORTANT, line("baking_powder").importance)
        assertEquals(IngredientFunction.LEAVENER, line("baking_powder").function)
        assertEquals(IngredientFunction.FAT, line("butter").function)
        assertEquals(IngredientFunction.SWEETENER, line("granulated_sugar").function)
        assertEquals(IngredientFunction.LIQUID, line("milk").function)
        assertEquals(IngredientFunction.BINDER, line("egg").function)
        assertEquals(IngredientImportance.OPTIONAL, line("chocolate_chips").importance)
        // The same ingredient can matter more in a different recipe.
        assertEquals(IngredientImportance.IMPORTANT, line("egg").importance)
        assertEquals(IngredientImportance.STRUCTURAL, baseline("vanilla_cupcakes", 1.5).allIngredients.first { it.ingredientId == "egg" }.importance)
        assertEquals(IngredientImportance.STRUCTURAL, baseline("chocolate_chip_cookies", 1.0).allIngredients.first { it.ingredientId == "butter" }.importance)
        assertEquals(IngredientImportance.IMPORTANT, baseline("blueberry_muffins", 1.0).allIngredients.first { it.ingredientId == "butter" }.importance)
    }

    // ---- butter ------------------------------------------------------------------------------------------

    @Test
    fun `butter in muffins offers oil as the best match`() {
        val b = baseline("blueberry_muffins", 2.0, less)
        val m = found(b, "Butter")
        assertEquals("1/2 cup (115 g) butter", m.originalText)
        val o = SubstitutionEngine.options(b, catalog, m)
        val best = o.options.first()
        assertEquals("butter_oil_batter", best.rule.id)
        assertEquals("BEST MATCH", best.rating)
        assertEquals("Neutral oil", best.rule.substituteName)
        assertEquals("6 tbsp (about 90 ml) neutral oil", best.useText)
        assertEquals("Recommended for this muffin recipe.", best.suitability)
        assertEquals("May become slightly softer and moister.", best.rule.effectOnTexture)
        assertEquals("Will have less buttery flavor.", best.rule.effectOnFlavor)
        assertEquals("Use a neutral-flavored oil such as canola or vegetable oil.", best.rule.specialInstructions)
        assertTrue(best.rule.tip!!.contains("For a more buttery flavor, use butter if available"))
        // Choices are NOT presented as equivalent.
        assertEquals("GOOD ALTERNATIVE", o.option("butter_coconut_oil").rating)
        assertEquals("TEXTURE WILL CHANGE", o.option("butter_applesauce").rating)
        assertEquals(SubstituteConfidence.LIMITED, o.option("butter_yogurt").confidence)
        assertEquals(o.options.map { it.confidence.ordinal }, o.options.map { it.confidence.ordinal }.sorted())
    }

    @Test
    fun `substitute amounts come from the SCALED recipe not the base recipe`() {
        // Blueberry muffins are written for 1 cup flour with 1/4 cup butter. Oil is 3/4 of the butter.
        val cases = listOf(1.0 to 0.25, 2.0 to 0.5, 3.0 to 0.75, 1.5 to 0.375, 0.5 to 0.125)
        for ((flour, butterCups) in cases) {
            val b = baseline("blueberry_muffins", flour)
            assertEquals(butterCups, b.ingredients.first { it.ingredientId == "butter" }.cups!!, 1e-9)
            val oil = options(b, "butter").option("butter_oil_batter").amounts.single()
            assertEquals("oil at $flour cups of flour", butterCups * 0.75, oil.amount, 1e-9)
        }
        assertEquals("3 tbsp (about 45 ml) neutral oil", options(baseline("blueberry_muffins", 1.0), "butter").option("butter_oil_batter").useText)
        assertEquals("6 tbsp (about 90 ml) neutral oil", options(baseline("blueberry_muffins", 2.0), "butter").option("butter_oil_batter").useText)
        assertEquals("1/2 cup + 1 tbsp (about 135 ml) neutral oil", options(baseline("blueberry_muffins", 3.0), "butter").option("butter_oil_batter").useText)
        // The ingredient list shows exactly the same number.
        val ap = applied("blueberry_muffins", 2.0, listOf(choice("butter", "butter_oil_batter")))
        assertEquals(0.375, ap.ingredients.first { it.substitutedFor == "butter" }.cups!!, 1e-9)
    }

    @Test
    fun `less sugar scaling carries into substitutions`() {
        // Banana muffins use brown sugar; Less Sugar makes it 70%. The substitute must follow that reduced amount.
        val normalB = baseline("banana_muffins", 2.0, normal)
        val lessB = baseline("banana_muffins", 2.0, less)
        val n = options(normalB, "brown sugar").option("brown_sugar_white_molasses").amounts.first { it.ingredientId == "granulated_sugar" }.amount
        val l = options(lessB, "brown sugar").option("brown_sugar_white_molasses").amounts.first { it.ingredientId == "granulated_sugar" }.amount
        assertEquals(0.5, n, 1e-9)
        assertEquals(0.5 * 0.7, l, 1e-9)
    }

    @Test
    fun `butter in cookies does not offer oil and explains why`() {
        val b = baseline("chocolate_chip_cookies", 2.0)
        val o = options(b, "butter")
        assertTrue(o.options.none { it.rule.id.startsWith("butter_oil") })
        assertEquals("butter_margarine", o.options.first().rule.id)
        assertEquals(SubstituteConfidence.RECOMMENDED, o.options.first().confidence)
        val no = o.notRecommended.first { it.substituteName == "Neutral oil" }
        assertTrue(no.reason, no.reason.contains("structure"))
        assertNotNull(o.structuralWarning)
        assertTrue(o.options.first().warnings.contains(o.structuralWarning))
        // Coconut oil and shortening are possible but flagged.
        assertEquals(SubstituteConfidence.LIMITED, o.option("butter_coconut_oil_limited").confidence)
        assertEquals(SubstituteConfidence.LIMITED, o.option("butter_shortening_cookies").confidence)
    }

    @Test
    fun `butter substitutions depend on the recipe category`() {
        fun ids(recipe: String, cups: Double) = options(baseline(recipe, cups), "butter").options.map { it.rule.id }.toSet()
        assertTrue("butter_oil_batter" in ids("blueberry_muffins", 1.0))
        assertTrue("butter_oil_batter" in ids("classic_pancakes", 1.0))
        assertTrue("butter_oil_batter" in ids("banana_bread", 1.5))
        assertTrue("butter_oil_white_bread" in ids("basic_white_bread", 3.0))
        assertTrue("butter_oil_cakes" in ids("simple_vanilla_cake", 1.5))
        assertTrue("butter_oil_brownies" in ids("classic_brownies", 0.5))
        for (r in listOf("classic_scones" to 2.0, "buttermilk_biscuits" to 2.0, "apple_pie" to 2.5, "sugar_cookies" to 2.0)) {
            assertTrue("${r.first}: ${ids(r.first, r.second)}", ids(r.first, r.second).none { it.startsWith("butter_oil") })
        }
        // Applesauce works for muffins but is excluded by quantity/category elsewhere.
        assertTrue("butter_applesauce" in ids("blueberry_muffins", 1.0))
        assertTrue("butter_applesauce" !in ids("classic_brownies", 0.5))
    }

    @Test
    fun `butter replaced by oil changes the instructions safely`() {
        val c = applied("blueberry_muffins", 2.0, listOf(choice("butter", "butter_oil_batter")), less, setOf("blueberries"))
        val text = stepsText(c)
        assertTrue(text, text.contains("whisk the eggs, milk, oil and vanilla until smooth"))
        assertFalse(text.lowercase().contains("butter"))
        assertFalse(c.tips.any { it.lowercase().contains("butter") })
        val line = c.ingredients.first { it.isSubstitute }
        assertEquals("Neutral oil", line.name)
        assertEquals("butter", line.substitutedFor)
        assertEquals("6 tbsp (90 ml)", line.displayText)
        // Everything else is untouched.
        val base = baseline("blueberry_muffins", 2.0, less, setOf("blueberries"))
        assertEquals(base.ingredients.filter { it.ingredientId != "butter" }.map { it.displayText },
            c.ingredients.filter { !it.isSubstitute }.map { it.displayText })
        assertEquals(base.cooking.timeText, c.cooking.timeText)
    }

    @Test
    fun `creaming steps are replaced when the fat cannot be creamed`() {
        val c = applied("vanilla_cupcakes", 1.5, listOf(choice("butter", "butter_oil_cakes")))
        val titles = c.steps.map { it.title }
        assertTrue(titles.toString(), "Whisk Oil and Sugar" in titles)
        assertTrue(titles.none { it.contains("Cream") })
        val whisk = c.steps.first { it.title == "Whisk Oil and Sugar" }.text
        assertFalse(whisk.lowercase().contains("butter"))
        assertFalse(whisk.contains("fluffy"))
        assertTrue(stepsText(c).contains("oil mixture"))
        // Margarine can still be creamed, so the creaming step stays (with the right name).
        val m = applied("vanilla_cupcakes", 1.5, listOf(choice("butter", "butter_margarine")))
        assertTrue(m.steps.map { it.title }.toString(), m.steps.any { it.title == "Cream Margarine and Sugar" })
        assertTrue(m.steps.first { it.title.startsWith("Cream") }.text.contains("softened margarine"))
    }

    @Test
    fun `solid fat substitutes keep the cutting in steps`() {
        val c = applied("buttermilk_biscuits", 2.0, listOf(choice("butter", "butter_shortening_pastry")))
        val cut = c.steps.first { it.title.startsWith("Cut In") }
        assertEquals("Cut In the Shortening", cut.title)
        assertTrue(cut.text, cut.text.contains("very cold shortening"))
        assertEquals("Vegetable shortening, very cold, cubed", c.ingredients.first { it.isSubstitute }.name)
    }

    @Test
    fun `an add-in that needs solid butter is removed when butter is replaced by oil`() {
        val b = baseline("blueberry_muffins", 2.0, adds = setOf("streusel"))
        val o = options(b, "butter").option("butter_oil_batter")
        assertEquals(listOf("Streusel"), o.droppedAddIns)
        assertTrue(o.warnings.any { it.contains("Streusel") })
        val c = applied("blueberry_muffins", 2.0, listOf(choice("butter", "butter_oil_batter")), adds = setOf("streusel"))
        assertTrue(c.addInIngredients.isEmpty())
        assertFalse(c.steps.any { it.title == "Add Toppings" })
        assertTrue(c.warnings.any { it.contains("Streusel") })
        // Margarine keeps the streusel.
        val m = applied("blueberry_muffins", 2.0, listOf(choice("butter", "butter_margarine")), adds = setOf("streusel"))
        assertTrue(m.addInIngredients.isNotEmpty())
    }

    // ---- milk and buttermilk ----------------------------------------------------------------------------

    @Test
    fun `milk can be replaced with plant milk or water`() {
        val b = baseline("blueberry_muffins", 2.0)
        val o = options(b, "milk")
        assertEquals("milk_plant_milk", o.options.first().rule.id)
        assertEquals("BEST MATCH", o.options.first().rating)
        assertEquals("1 cup (about 240 ml) plant-based milk", o.options.first().useText)
        assertEquals("GOOD ALTERNATIVE", o.option("milk_water_batter").rating)
        assertEquals("1 cup (about 240 ml) water", o.option("milk_water_batter").useText)
        val yogurt = o.option("milk_thinned_yogurt").amounts
        assertEquals(0.75, yogurt[0].amount, 1e-9); assertEquals(0.25, yogurt[1].amount, 1e-9)
        // Same amounts at 3 cups of flour: 1 1/2 cups of milk.
        assertEquals("1 1/2 cups (about 360 ml) plant-based milk", options(baseline("blueberry_muffins", 3.0), "milk").options.first().useText)
        val c = applied("blueberry_muffins", 2.0, listOf(choice("milk", "milk_plant_milk")))
        assertTrue(stepsText(c).contains("whisk the eggs, plant-based milk, melted butter and vanilla"))
        assertTrue(c.conclusion.labelChecks.single().contains("soy, almond, oat"))
    }

    @Test
    fun `buttermilk is soured milk with the acid measured from the scaled buttermilk`() {
        val b = baseline("buttermilk_biscuits", 2.0) // 3/4 cup buttermilk
        val m = found(b, "buttermilk")
        assertEquals("3/4 cup (180 ml) buttermilk", m.originalText)
        val o = options(b, "buttermilk")
        val lemon = o.option("buttermilk_milk_lemon")
        assertEquals("BEST MATCH", lemon.rating)
        val acid = lemon.amounts.first { it.ingredientId == "lemon_juice" }.amount
        val milk = lemon.amounts.first { it.ingredientId == "milk" }.amount
        assertEquals(0.75 / 16, acid, 1e-9)           // 1 tbsp per cup of buttermilk
        assertEquals(0.75, acid + milk, 1e-9)           // the total stays the buttermilk amount
        // Scales with the flour amount, too.
        val big = options(baseline("buttermilk_biscuits", 4.0), "buttermilk").option("buttermilk_milk_lemon").amounts
        assertEquals(1.5 / 16, big.first { it.ingredientId == "lemon_juice" }.amount, 1e-9)
        assertEquals(1.5, big.sumOf { it.amount }, 1e-9)

        val c = applied("buttermilk_biscuits", 2.0, listOf(choice("buttermilk", "buttermilk_milk_lemon")))
        val steps = c.steps.map { it.title }
        assertTrue(steps.toString(), steps.indexOf("Make the Buttermilk Substitute") < steps.indexOf("Mix Dry Ingredients"))
        assertTrue(c.steps.first { it.title == "Make the Buttermilk Substitute" }.text.contains("2 1/4 tsp lemon juice"))
        assertTrue(steps.contains("Add the Soured Milk"))
        assertTrue(c.steps.first { it.title == "Add the Soured Milk" }.text.startsWith("Pour in the soured milk"))
        assertFalse(stepsText(c).lowercase().contains("buttermilk substitute: ".repeat(2)))
        assertEquals(setOf("lemon_juice", "milk"), c.ingredients.filter { it.isSubstitute }.map { it.ingredientId }.toSet())
    }

    // ---- eggs ---------------------------------------------------------------------------------------------

    @Test
    fun `eggs can be replaced where the recipe allows it`() {
        val b = baseline("blueberry_muffins", 2.0)
        val o = options(b, "eggs")
        val flax = o.option("egg_flax_batter")
        assertEquals("2 tbsp (about 14 g) ground flaxseed + 6 tbsp (about 90 ml) water", flax.useText)
        assertEquals(2.0, flax.amounts[0].amount, 1e-9); assertEquals(MeasureUnit.TBSP, flax.amounts[0].unit)
        assertEquals("1/2 cup (about 125 g) unsweetened applesauce", o.option("egg_applesauce_batter").useText)
        assertEquals("GOOD ALTERNATIVE", flax.rating)
        // Scaled: 1.5 cups of flour needs 1.5 eggs.
        val half = options(baseline("blueberry_muffins", 1.5), "eggs").option("egg_flax_batter").useText
        assertEquals("1 1/2 tbsp (about 11 g) ground flaxseed + 1/4 cup + 1/2 tbsp (about 70 ml) water", half)
        val c = applied("blueberry_muffins", 2.0, listOf(choice("egg", "egg_flax_batter")))
        assertTrue(c.steps.first().title.startsWith("Preheat"))
        val titles = c.steps.map { it.title }
        assertTrue(titles.indexOf("Make the Flax Eggs") < titles.indexOf("Mix Wet Ingredients"))
        assertTrue(stepsText(c).contains("whisk the flax eggs, milk, melted butter and vanilla"))
        assertFalse(c.allergens.contains(Allergen.EGG))
        assertEquals(setOf("ground_flaxseed", "water"), c.ingredients.filter { it.isSubstitute }.map { it.ingredientId }.toSet())
    }

    @Test
    fun `eggs are not replaced where they are essential or too numerous`() {
        // Crepes: a specific explanation.
        val crepes = options(baseline("crepes", 1.0), "egg")
        assertTrue(crepes.options.isEmpty())
        assertEquals("Eggs are what hold crepes together. Without them the crepes tear and break in the pan.", crepes.message)
        assertNotNull(crepes.structuralWarning)
        // Brownies at 1 cup flour need 4 eggs: beyond what any replacer is trusted for.
        val big = options(baseline("classic_brownies", 1.0), "egg")
        assertTrue(big.options.isEmpty())
        assertEquals("No reliable substitute is currently recommended for this ingredient in this recipe.", big.message)
        // ... but with only 2 eggs, possible with an honest warning.
        val small = options(baseline("classic_brownies", 0.5), "egg")
        assertTrue(small.options.isNotEmpty())
        assertTrue(small.options.all { it.confidence == SubstituteConfidence.LIMITED })
        assertTrue(small.options.all { it.suitability == "Possible, but expect a different result." })
        // Muffins at 3 cups: 3 eggs - flax is still fine, applesauce (max 2) is not offered.
        val three = options(baseline("blueberry_muffins", 3.0), "eggs").options.map { it.rule.id }
        assertTrue("egg_flax_batter" in three)
        assertTrue("egg_applesauce_batter" !in three)
    }

    // ---- sugars --------------------------------------------------------------------------------------------

    @Test
    fun `brown sugar can be rebuilt from white sugar and molasses`() {
        val b = baseline("banana_muffins", 1.0) // 1/4 cup brown sugar
        val o = options(b, "brown sugar")
        val best = o.options.first()
        assertEquals("brown_sugar_white_molasses", best.rule.id)
        assertEquals("1/4 cup (about 50 g) granulated sugar + 3/4 tsp molasses", best.useText)
        val c = applied("banana_muffins", 1.0, listOf(choice("brown_sugar", "brown_sugar_white_molasses")))
        assertTrue(c.steps.any { it.title == "Make the Brown Sugar" && it.text.contains("3/4 tsp molasses") })
        assertTrue(stepsText(c).contains("homemade brown sugar"))
        // In cookies, plain white sugar is flagged as changing the cookie.
        val cookies = options(baseline("chocolate_chip_cookies", 1.0), "brown sugar")
        assertEquals(SubstituteConfidence.LIMITED, cookies.option("brown_sugar_white_cookies").confidence)
        assertEquals(SubstituteConfidence.RECOMMENDED, cookies.option("brown_sugar_white_molasses").confidence)
    }

    @Test
    fun `white sugar can be swapped for honey with a liquid reduction and the honey joins the wet ingredients`() {
        val b = baseline("blueberry_muffins", 2.0) // 2/3 cup sugar, 1 cup milk
        val o = options(b, "sugar").option("sugar_honey")
        assertEquals(SubstituteConfidence.LIMITED, o.confidence)
        assertEquals(0.5, o.amounts.single().amount, 1e-9)           // 3/4 of 2/3 cup
        assertTrue(o.liquidAdjustment!!.startsWith("Milk is reduced by"))
        val c = applied("blueberry_muffins", 2.0, listOf(choice("granulated_sugar", "sugar_honey")))
        val milk = c.ingredients.first { it.ingredientId == "milk" }
        assertEquals(1.0 - (2.0 / 3) * 0.25, milk.cups!!, 1e-9)
        assertNotNull(milk.adjustment)
        val dry = c.steps.first { it.title == "Mix Dry Ingredients" }.text
        assertFalse(dry, dry.contains("honey"))
        assertTrue(dry, dry.contains("whisk together the flour, baking powder and salt"))
        assertTrue(c.steps.first { it.title == "Mix Wet Ingredients" }.text.contains("honey"))
    }

    // ---- flour and leaveners ---------------------------------------------------------------------------------

    @Test
    fun `flour is structural so substitutions carry a warning`() {
        val b = baseline("blueberry_muffins", 2.0)
        val o = options(b, "AP flour")
        assertEquals(IngredientImportance.STRUCTURAL, o.missing.importance)
        assertTrue(o.structuralWarning!!.contains("structural"))
        val cake = o.option("ap_flour_cake_flour_batter")
        assertEquals("2 1/4 cups (about 255 g) cake flour", cake.useText)   // 1 cup + 2 tbsp per cup of flour
        assertEquals(SubstituteConfidence.LIMITED, o.option("ap_flour_bread_flour_tender").confidence)
        assertTrue(cake.warnings.contains(o.structuralWarning))
        // Flour amounts still scale from the user's choice: the flour text and yield do not change.
        val c = applied("blueberry_muffins", 2.0, listOf(choice("all_purpose_flour", "ap_flour_cake_flour_batter")))
        assertEquals(b.flourText, c.flourText)
        assertEquals(b.yieldEstimate.text, c.yieldEstimate.text)
        assertEquals("Cake flour", c.ingredients.first { it.isSubstitute }.name)
    }

    @Test
    fun `bread flour and all purpose flour swap in bread`() {
        val b = baseline("basic_white_bread", 3.0)
        val o = options(b, "bread flour")
        assertEquals("bread_flour_ap_flour", o.options.first().rule.id)
        assertEquals("3 cups (about 360 g) all-purpose flour", o.options.first().useText)
        // Cookies with all-purpose flour: bread flour makes them chewier but is acceptable.
        val cookies = options(baseline("chocolate_chip_cookies", 1.0), "flour")
        assertEquals(SubstituteConfidence.ACCEPTABLE, cookies.option("ap_flour_bread_flour_chewy").confidence)
        assertEquals(SubstituteConfidence.LIMITED, cookies.option("ap_flour_cake_flour_other").confidence)
    }

    @Test
    fun `baking powder can be rebuilt and soda replacement is limited`() {
        val b = baseline("blueberry_muffins", 2.0) // 3 tsp baking powder
        val o = options(b, "baking powder")
        assertEquals("3/4 tsp baking soda + 1 1/2 tsp cream of tartar", o.options.single().useText)
        // Replacing soda with powder is only offered for recipes that have another acid to balance it.
        assertTrue(options(baseline("chocolate_chip_cookies", 1.0), "baking soda").options.isEmpty())
        val banana = options(baseline("banana_bread", 2.0), "baking soda")
        assertEquals(SubstituteConfidence.LIMITED, banana.options.single().confidence)
        val bread = options(baseline("basic_white_bread", 3.0), "yeast")
        assertTrue(bread.options.isEmpty())
        assertEquals(SubstitutionEngine.NO_RELIABLE_SUBSTITUTE, bread.message)
    }

    // ---- optional and flavor ingredients -------------------------------------------------------------------

    @Test
    fun `optional add-ins can be swapped or left out`() {
        val b = baseline("chocolate_chip_cookies", 2.0, adds = setOf("chocolate_chips"))
        val o = options(b, "chocolate chips")
        assertEquals(IngredientImportance.OPTIONAL, o.missing.importance)
        assertEquals("chocolate_chips_chopped", o.options.first().rule.id)
        assertEquals("1 1/2 cups (about 255 g) chopped chocolate bar", o.options.first().useText)
        assertEquals("OPTIONAL - CAN SKIP", o.option("chocolate_chips_leave_out").rating)
        val c = applied("chocolate_chip_cookies", 2.0, listOf(choice("chocolate_chips", "chocolate_chips_leave_out")), adds = setOf("chocolate_chips"))
        assertTrue(c.addInIngredients.isEmpty())
        assertFalse(c.steps.any { it.title == "Add Optional Ingredients" })
        assertTrue(c.conclusion.recipeSummary.none { it.label == "Add-ins" })
    }

    @Test
    fun `leaving vanilla out keeps the instructions grammatical`() {
        val omit = listOf(choice("vanilla_extract", "vanilla_leave_out"))
        for (id in listOf("blueberry_muffins", "classic_brownies", "chocolate_brownies", "vanilla_cupcakes", "simple_vanilla_cake", "classic_pancakes")) {
            val r = catalog.recipe(id)!!
            val c = applied(id, r.baseFlourAmount, omit)
            val text = stepsText(c)
            assertFalse("$id: $text", text.lowercase().contains("vanilla"))
            assertFalse("$id: $text", Regex("\\s,|,\\s*,|\\band\\s*[.,]|\\s\\.|,\\s*\\.|\\bthen the\\s*[.,]").containsMatchIn(text))
            assertTrue(c.ingredients.none { it.ingredientId == "vanilla_extract" })
        }
        val brownies = applied("classic_brownies", 0.5, omit)
        assertTrue(brownies.steps.any { it.title == "Add Eggs" })
        assertTrue(stepsText(brownies).contains("Beat in the eggs one at a time, and stir for about a minute until glossy."))
    }

    @Test
    fun `cocoa and peanut butter have no reliable substitute`() {
        val cake = options(baseline("chocolate_cake", 1.5), "cocoa")
        assertTrue(cake.options.isEmpty())
        assertTrue(cake.message!!.contains("main chocolate flavor"))
        val pb = options(baseline("peanut_butter_cookies", 1.0), "peanut butter")
        assertTrue(pb.options.isEmpty())
        assertTrue(pb.message!!.contains("main flavor"))
        // A vanilla swap is possible but says so.
        val vanilla = options(baseline("blueberry_muffins", 1.0), "vanilla")
        assertEquals(SubstituteConfidence.LIMITED, vanilla.option("vanilla_almond_extract").confidence)
        assertEquals("FLAVOR WILL CHANGE", vanilla.option("vanilla_almond_extract").rating)
    }

    // ---- several missing ingredients ----------------------------------------------------------------------

    @Test
    fun `several missing ingredients are handled together and a warning is shown`() {
        val b = baseline("blueberry_muffins", 2.0, less, setOf("blueberries"))
        val butter = found(b, "butter"); val milk = found(b, "milk"); val vanilla = found(b, "vanilla")
        assertEquals(SubstitutionEngine.MANY_KEY_INGREDIENTS, SubstitutionEngine.combinationWarning(listOf(butter, milk)))
        assertNull(SubstitutionEngine.combinationWarning(listOf(butter)))
        assertNull(SubstitutionEngine.combinationWarning(listOf(vanilla)))
        assertNull(SubstitutionEngine.combinationWarning(listOf(butter, vanilla)))
        assertEquals("Replacing several key ingredients may significantly change the final texture and flavor.", SubstitutionEngine.MANY_KEY_INGREDIENTS)

        val c = applied("blueberry_muffins", 2.0, listOf(choice("butter", "butter_oil_batter"), choice("milk", "milk_plant_milk"), choice("vanilla_extract", "vanilla_leave_out")),
            less, setOf("blueberries"))
        assertEquals(3, c.appliedSubstitutions.size)
        assertTrue(stepsText(c).contains("whisk the eggs, plant-based milk and oil until smooth"))
        assertTrue(c.warnings.contains(SubstitutionEngine.MANY_KEY_INGREDIENTS))
        assertEquals(listOf("Butter → Neutral oil", "Milk → Plant-based milk", "Vanilla extract → left out"), c.conclusion.substitutionSummary)
    }

    @Test
    fun `a substitute never depends on another missing ingredient`() {
        val b = baseline("classic_scones", 2.0)
        val withButter = options(b, "cream").options.map { it.rule.id }
        assertTrue("cream_milk_butter" in withButter)
        // If butter is missing too, the milk-and-butter cream substitute is not offered.
        val withoutButter = options(b, "cream", setOf("butter")).options.map { it.rule.id }
        assertEquals(listOf("cream_milk"), withoutButter)
        // Soured milk needs milk: not offered when milk is also missing.
        val bb = baseline("buttermilk_biscuits", 2.0)
        val ids = options(bb, "buttermilk", setOf("milk")).options.map { it.rule.id }
        assertTrue(ids.toString(), ids.none { it == "buttermilk_milk_lemon" || it == "buttermilk_milk_vinegar" })
        assertTrue("buttermilk_yogurt_milk" !in ids)
    }

    // ---- units ----------------------------------------------------------------------------------------------

    @Test
    fun `metric mode shows grams and milliliters for substitutes`() {
        val b = baseline("blueberry_muffins", 2.0, system = UnitSystem.METRIC)
        val oil = options(b, "butter").option("butter_oil_batter")
        assertEquals("90 ml (about 6 tbsp)", oil.amounts.single().let { "${it.primaryText} (about ${it.secondaryText})" })
        val c = applied("blueberry_muffins", 2.0, listOf(choice("butter", "butter_oil_batter"), choice("egg", "egg_flax_batter")), system = UnitSystem.METRIC)
        assertEquals("90 ml (6 tbsp)", c.ingredients.first { it.ingredientId == "vegetable_oil" }.displayText)
        assertEquals("14 g (2 tbsp)", c.ingredients.first { it.ingredientId == "ground_flaxseed" }.displayText)
        assertTrue(c.steps.first { it.title == "Make the Flax Eggs" }.text.contains("14 g ground flaxseed into 90 ml water"))
        // Grams for a dry substitute use that ingredient's own density.
        val flour = options(baseline("blueberry_muffins", 2.0, system = UnitSystem.METRIC), "flour").option("ap_flour_cake_flour_batter")
        assertEquals("255 g", flour.amounts.single().primaryText)
    }

    @Test
    fun `practical measurements are used for substitutes`() {
        val allowed = Regex("^(a pinch|(\\d+ )?\\d+/\\d+ (cup|cups|tsp|tbsp)|\\d+(/\\d+)? ?(cup|cups|tsp|tbsp)|\\d+ \\d+/\\d+ (cups|tsp|tbsp)|\\d+ (cups?|tbsp|tsp)|\\d+ g|\\d+ ml|(\\d+ )?(\\d+/\\d+ )?cups? \\+ (\\d+ )?(\\d+/\\d+ )?tbsp|\\d+/\\d+ cup \\+ \\d+ tbsp)$")
        for (flour in listOf(0.5, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0)) {
            val b = baseline("blueberry_muffins", flour)
            for (text in listOf("butter", "milk", "eggs", "baking powder", "sugar")) {
                for (o in options(b, text).options) for (a in o.amounts) {
                    assertFalse("${o.rule.id}@$flour: ${a.primaryText}", Regex("\\d\\.\\d").containsMatchIn(a.primaryText))
                    assertTrue("${o.rule.id}@$flour: '${a.primaryText}'", allowed.matches(a.primaryText))
                }
            }
        }
    }

    // ---- unsupported substitutions ---------------------------------------------------------------------------

    @Test
    fun `an unsuitable substitution is rejected and changes nothing`() {
        val plain = applied("chocolate_chip_cookies", 1.0, emptyList())
        for (bad in listOf(
            choice("butter", "butter_oil_batter"),          // not allowed in cookies
            choice("butter", "butter_oil_not_cookies"),     // a "not recommended" rule
            choice("butter", "does_not_exist"),
            choice("milk", "butter_margarine"),             // rule belongs to another ingredient
            choice("egg", "egg_flax_batter"),               // not allowed in cookies
        )) {
            val c = applied("chocolate_chip_cookies", 1.0, listOf(bad))
            assertEquals(listOf(bad.substitutionId), c.rejectedSubstitutions)
            assertTrue(c.appliedSubstitutions.isEmpty())
            assertEquals(plain.allIngredients.map { it.displayText }, c.allIngredients.map { it.displayText })
            assertEquals(stepsText(plain), stepsText(c))
            assertNull(c.conclusion.substitutionExpect)
        }
    }

    @Test
    fun `quantity limits are respected`() {
        val limited = IngredientSubstitution(
            id = "test_limited", originalIngredient = "butter", substituteName = "Applesauce",
            conversionRule = ConversionRule(listOf(SubstituteComponent("applesauce", ratio = 0.5))),
            maxPerFlourCup = 0.2, confidenceLevel = SubstituteConfidence.LIMITED,
            effectOnTexture = "x", effectOnFlavor = "x", effectOnBrowning = "x", warning = "w",
        )
        val lib = SubstitutionLibrary.build(listOf(limited), AliasFile(), catalog.ingredients)
        val cat = RecipeCatalog(catalog.ingredients, catalog.addIns, catalog.toppings, catalog.recipes, lib)
        // Muffin butter is 0.25 cup per cup of flour, over the 0.2 limit.
        assertTrue(options(baseline("blueberry_muffins", 2.0, cat = cat), "butter", cat = cat).options.isEmpty())
        // Banana bread butter is about 0.22 per cup; pretend the limit is 0.3 and it passes.
        val ok = limited.copy(maxPerFlourCup = 0.3)
        val cat2 = RecipeCatalog(catalog.ingredients, catalog.addIns, catalog.toppings, catalog.recipes,
            SubstitutionLibrary.build(listOf(ok), AliasFile(), catalog.ingredients))
        assertEquals(1, options(baseline("banana_bread", 1.5, cat = cat2), "butter", cat = cat2).options.size)
        // An absolute limit.
        val abs = limited.copy(maxPerFlourCup = null, maxOriginalAmount = 0.3)
        val cat3 = RecipeCatalog(catalog.ingredients, catalog.addIns, catalog.toppings, catalog.recipes,
            SubstitutionLibrary.build(listOf(abs), AliasFile(), catalog.ingredients))
        assertTrue(options(baseline("blueberry_muffins", 2.0, cat = cat3), "butter", cat = cat3).options.isEmpty())
        assertEquals(1, options(baseline("blueberry_muffins", 1.0, cat = cat3), "butter", cat = cat3).options.size)
    }

    @Test
    fun `incompatible recipes and cooking methods exclude a rule`() {
        val rule = IngredientSubstitution(
            id = "t", originalIngredient = "butter", substituteName = "Oil",
            conversionRule = ConversionRule(listOf(SubstituteComponent("vegetable_oil", ratio = 0.75))),
            incompatibleRecipes = listOf("blueberry_muffins"), incompatibleMethods = listOf(CookingMethod.STEAM),
            confidenceLevel = SubstituteConfidence.ACCEPTABLE, effectOnTexture = "x", effectOnFlavor = "x", effectOnBrowning = "x",
        )
        val cat = RecipeCatalog(catalog.ingredients, catalog.addIns, catalog.toppings, catalog.recipes,
            SubstitutionLibrary.build(listOf(rule), AliasFile(), catalog.ingredients))
        assertTrue(options(baseline("blueberry_muffins", 1.0, cat = cat), "butter", cat = cat).options.isEmpty())
        assertEquals(1, options(baseline("banana_muffins", 1.0, cat = cat), "butter", cat = cat).options.size)
        val r = cat.recipe("banana_muffins")!!
        val steam = RecipeCalculator.calculateBaseline(r, cat, RecipeSelection(1.0, normal, CookingMethod.STEAM))
        assertTrue(options(steam, "butter", cat = cat).options.isEmpty())
    }

    // ---- ingredients that the real recipes do not use (synthetic recipes) -------------------------------------

    @Test
    fun `other library ingredients work in a recipe that uses them`() {
        fun check(lines: List<IngredientLine>, ask: String, rule: String, expectedUse: String, flour: String? = null) {
            val cat = syntheticCatalog("blueberry_muffins", lines, flour)
            val b = baseline("blueberry_muffins", 1.0, cat = cat)
            val o = options(b, ask, cat = cat)
            assertEquals("$ask -> ${o.options.map { it.rule.id }}", rule, o.options.first { it.rule.id == rule }.rule.id)
            assertEquals(expectedUse, o.options.first { it.rule.id == rule }.useText)
            // And it can be applied.
            val c = RecipeCalculator.calculate(cat.recipe("blueberry_muffins")!!, cat,
                RecipeSelection(1.0, normal, CookingMethod.OVEN, emptySet(), UnitSystem.US_CUPS, listOf(choice(o.missing.ingredientId, rule))))
            assertTrue(rule, c.appliedSubstitutions.size == 1 && c.rejectedSubstitutions.isEmpty())
        }
        check(listOf(IngredientLine("sour_cream", 0.5, MeasureUnit.CUP)), "soured cream", "sour_cream_yogurt", "1/2 cup (about 125 g) plain yogurt")
        check(listOf(IngredientLine("honey", 0.5, MeasureUnit.CUP)), "honey", "honey_maple_syrup", "1/2 cup (about 160 g) maple syrup")
        check(listOf(IngredientLine("maple_syrup", 0.5, MeasureUnit.CUP)), "maple syrup", "maple_syrup_honey", "1/2 cup (about 170 g) honey")
        check(listOf(IngredientLine("white_vinegar", 1.0, MeasureUnit.TSP)), "vinegar", "white_vinegar_lemon_juice", "1 tsp lemon juice")
        check(listOf(IngredientLine("coconut_oil", 0.25, MeasureUnit.CUP)), "coconut oil", "coconut_oil_vegetable_oil", "1/4 cup (about 60 ml) neutral oil")
        check(listOf(IngredientLine("powdered_sugar", 0.25, MeasureUnit.CUP, role = IngredientRole.SUGAR)), "icing sugar", "powdered_sugar_blended",
            "1/4 cup (about 50 g) granulated sugar + 3/4 tsp cornstarch")
        check(emptyList(), "cake flour", "cake_flour_ap_flour_cornstarch", "3/4 cup + 2 tbsp (about 105 g) all-purpose flour + 2 tbsp (about 16 g) cornstarch", flour = "cake_flour")
        check(listOf(IngredientLine("cream_cheese", 0.25, MeasureUnit.CUP, prep = "softened", optional = true)), "cream cheese", "cream_cheese_mascarpone", "1/4 cup (about 55 g) mascarpone")
    }

    // ---- conclusion ----------------------------------------------------------------------------------------------

    @Test
    fun `the conclusion includes the substitutions`() {
        val c = applied("blueberry_muffins", 2.0, listOf(choice("butter", "butter_oil_batter")), less, setOf("blueberries")).conclusion
        assertEquals(listOf(
            "Recipe" to "Blueberry Muffins", "Flour" to "2 cups (240 g)", "Sweetness" to "Less Sugar", "Cooking" to "Oven",
            "Add-ins" to "Blueberries", "Substitution" to "Butter → Neutral oil",
        ), c.recipeSummary.map { it.label to it.value })
        assertEquals(
            "You selected Less Sugar and replaced butter with neutral oil. These blueberry muffins will be less sweet than the traditional " +
                "version and may be slightly softer and moister. They will also have less buttery flavor.",
            c.substitutionExpect)
        assertTrue(c.substitutionImprovement!!.startsWith("For additional flavor without greatly increasing sweetness, try "))
        assertTrue(c.substitutionImprovement!!.contains("vanilla"))
        assertTrue(c.substitutionImprovement!!.contains("cinnamon"))
        assertFalse("already added", c.substitutionImprovement!!.contains("blueberries"))
        assertTrue(c.importantNotes.any { it.contains("Substitutions can change how quickly") })
        // The sweetness part of the conclusion is still there.
        assertEquals("About Your Less Sugar Version", c.title)
        assertTrue(c.summary.startsWith("You selected Less Sugar."))
    }

    @Test
    fun `the conclusion changes with the sweetness choice and the substitution`() {
        val n = applied("blueberry_muffins", 1.0, listOf(choice("butter", "butter_oil_batter")), normal).conclusion
        assertTrue(n.substitutionExpect!!.contains("will have the traditional level of sweetness"))
        assertEquals("About Your Normal Sugar Version", n.title)
        val bread = applied("banana_bread", 1.5, listOf(choice("butter", "butter_margarine")), less).conclusion
        assertTrue(bread.substitutionExpect!!.startsWith("This banana bread will")  .not() && bread.substitutionExpect!!.contains("This banana bread"))
        val omit = applied("blueberry_muffins", 1.0, listOf(choice("vanilla_extract", "vanilla_leave_out"))).conclusion
        assertTrue(omit.substitutionExpect!!.contains("left out vanilla extract"))
        assertFalse(omit.substitutionImprovement.orEmpty().contains("extra vanilla"))
    }

    @Test
    fun `no substitutions means no substitution text`() {
        val c = applied("blueberry_muffins", 2.0, emptyList()).conclusion
        assertNull(c.substitutionExpect)
        assertTrue(c.substitutionSummary.isEmpty())
        assertTrue(c.recipeSummary.none { it.label == "Substitution" })
        assertEquals(4, c.recipeSummary.size)
    }

    // ---- allergens -------------------------------------------------------------------------------------------------

    @Test
    fun `allergen notices follow the substitutions`() {
        val egg = applied("classic_pancakes", 1.0, listOf(choice("egg", "egg_flax_batter")))
        assertFalse(Allergen.EGG in egg.allergens)
        val almond = applied("blueberry_muffins", 1.0, listOf(choice("vanilla_extract", "vanilla_almond_extract")))
        assertTrue(Allergen.TREE_NUTS in almond.allergens)
        val noButter = applied("banana_muffins", 1.0, listOf(choice("butter", "butter_oil_batter")))
        assertTrue(Allergen.MILK in noButter.allergens) // milk is still in the recipe
        val margarine = applied("blueberry_muffins", 1.0, listOf(choice("butter", "butter_margarine")))
        assertTrue(margarine.conclusion.labelChecks.any { it.contains("milk or soy") })
    }

    // ---- properties over the whole library ---------------------------------------------------------------------------

    @Test
    fun `every applicable substitution in every recipe produces clean results`() {
        val forbidden = mapOf(
            "butter" to Regex("(?<!peanut )\\bbutter\\b", RegexOption.IGNORE_CASE),
            "buttermilk" to Regex("\\bbuttermilk\\b", RegexOption.IGNORE_CASE),
            "milk" to Regex("(?<!butter)\\bmilk\\b", RegexOption.IGNORE_CASE),
            "vanilla_extract" to Regex("\\bvanilla\\b", RegexOption.IGNORE_CASE),
            "baking_powder" to Regex("\\bbaking powder\\b", RegexOption.IGNORE_CASE),
            "baking_soda" to Regex("\\bbaking soda\\b", RegexOption.IGNORE_CASE),
            "plain_yogurt" to Regex("\\byogurt\\b", RegexOption.IGNORE_CASE),
            "lemon_juice" to Regex("\\blemon juice\\b", RegexOption.IGNORE_CASE),
            "cinnamon" to Regex("\\bcinnamon\\b(?! sugar)", RegexOption.IGNORE_CASE),
            "garlic_powder" to Regex("\\bgarlic powder\\b", RegexOption.IGNORE_CASE),
            "cornstarch" to Regex("\\bcornstarch\\b", RegexOption.IGNORE_CASE),
        )
        val messy = Regex("\\{|\\}|  |\\s,|,,|\\s\\.|\\band\\s*[.,]|\\band and\\b|\\bthe\\s+(and|,|\\.)|,\\s*\\.|\\(\\s*\\)")
        var applications = 0
        for (recipe in catalog.recipes) {
            val all = recipe.addIns.map { it.id }.toSet()
            for (cups in listOf(recipe.baseFlourAmount, 2.0)) {
                for (adds in listOf(emptySet(), all)) {
                    val b = baseline(recipe.id, cups, adds = adds)
                    val ids = b.allIngredients.map { it.ingredientId }.distinct()
                    val baseText = stepsText(b)
                    for (id in ids) {
                        val m = SubstitutionEngine.describe(b, catalog, id)!!
                        for (opt in SubstitutionEngine.options(b, catalog, m).options) {
                            val c = RecipeCalculator.calculate(recipe, catalog,
                                RecipeSelection(cups, normal, recipe.recommendedMethod!!, adds, UnitSystem.US_CUPS, listOf(choice(id, opt.rule.id))))
                            applications++
                            val where = "${recipe.id}@$cups ${opt.rule.id} adds=${adds.size}"
                            assertTrue("$where rejected", c.rejectedSubstitutions.isEmpty())
                            assertEquals(where, 1, c.appliedSubstitutions.size)
                            // The explanatory sentence of the yogurt-thinning step has to name the milk it replaces.
                            val text = (stepsText(c) + "\n" + c.tips.joinToString("\n") + "\n" + c.methodTips.joinToString("\n"))
                                .replace("Use it wherever the recipe calls for milk.", "")
                                // A general serving suggestion for other add-ins, not an instruction about this recipe's ingredients.
                                .replace("try blueberries, chocolate chips or cinnamon.", "")
                                .replace("Make the Buttermilk Substitute", "")
                                // Greasing the pan is a separate use of butter, not the recipe's butter.
                                .replace(Regex("(?i)(grease|greasing|lightly butter|butter the|butter a)[^.]*\\."), "")
                            assertFalse("$where messy text:\n$text", messy.containsMatchIn(stepsText(c)))
                            assertTrue(where, c.steps.all { it.title.isNotBlank() && it.text.isNotBlank() })
                            assertEquals(where, (1..c.steps.size).toList(), c.steps.map { it.number })
                            // The replaced ingredient no longer appears in the instructions (unless the substitute still contains it).
                            val allowed = (opt.rule.stepNouns + opt.rule.conversionRule.components.map { catalog.ingredient(it.ingredient).noun } +
                                opt.rule.conversionRule.components.map { it.label.orEmpty() } + opt.rule.substituteName).joinToString(" ").lowercase()
                            forbidden[id]?.let { rx ->
                                if (!rx.containsMatchIn(allowed)) {
                                    assertFalse("$where still mentions $id:\n$text", rx.containsMatchIn(text))
                                }
                            }
                            // Ingredient lines are clean.
                            assertTrue(where, c.allIngredients.all { it.primaryText.isNotBlank() && it.amount.isFinite() && it.amount > 0 })
                            if (!opt.rule.isOmission) assertTrue(where, c.allIngredients.any { it.isSubstitute })
                            else assertTrue(where, c.allIngredients.none { it.ingredientId == id })
                            assertNotNull(where, c.conclusion.substitutionExpect)
                            assertEquals(where, c.appliedSubstitutions.size, c.conclusion.substitutionSummary.size)
                            // Cooking time and flour never change because of a substitution.
                            assertEquals(where, b.cooking.timeText, c.cooking.timeText)
                            assertEquals(where, b.flourText, c.flourText)
                            assertEquals(where, b.yieldEstimate.text, c.yieldEstimate.text)
                            if (opt.droppedAddIns.isEmpty() && opt.rule.isOmission.not()) {
                                assertEquals(where, b.steps.size + opt.rule.instructionOverrides.count { it.action == OverrideAction.PREPEND_STEP && (it.categories.isEmpty() || recipe.category in it.categories) },
                                    c.steps.size)
                            }
                        }
                    }
                    assertEquals(baseText, stepsText(baseline(recipe.id, cups, adds = adds))) // baseline is stable
                }
            }
        }
        assertTrue("only $applications substitutions exercised", applications > 1500)
    }

    // ---- offline -------------------------------------------------------------------------------------------------------

    @Test
    fun `the substitution feature uses only local data and no network or AI services`() {
        val root = java.io.File(System.getProperty("bakeyourway.assets")).parentFile.parentFile.parentFile // BakeYourWay/
        val sources = listOf("core/src/main", "app/src/main/java").flatMap { dir ->
            java.io.File(root, dir).walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "xml" || it.extension == "kts") }.toList()
        } + java.io.File(root, "app/build.gradle.kts") + java.io.File(root, "core/build.gradle.kts")
        assertTrue(sources.size > 20)
        val banned = Regex("java\\.net\\.|HttpURLConnection|okhttp|retrofit|ktor|openai|anthropic|gemini|api\\.openai|WebView|android\\.permission\\.INTERNET|volley",
            RegexOption.IGNORE_CASE)
        for (f in sources) {
            val hit = banned.find(f.readText())
            assertNull("${f.path} mentions '${hit?.value}'", hit)
        }
        assertFalse(TestSupport.manifest.readText().contains("uses-permission"))
        assertTrue(catalog.library.rules.isNotEmpty())
    }
}
