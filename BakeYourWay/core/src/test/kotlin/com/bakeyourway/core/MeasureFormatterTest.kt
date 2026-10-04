package com.bakeyourway.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MeasureFormatterTest {
    private fun cups(c: Double) = MeasureFormatter.formatVolume(c * 48)

    @Test
    fun `cup amounts use practical fractions`() {
        assertEquals("1/2 cup", cups(0.5))
        assertEquals("1 cup", cups(1.0))
        assertEquals("1 1/2 cups", cups(1.5))
        assertEquals("2 cups", cups(2.0))
        assertEquals("2 1/2 cups", cups(2.5))
        assertEquals("3 cups", cups(3.0))
        assertEquals("1/3 cup", cups(1.0 / 3))
        assertEquals("2/3 cup", cups(2.0 / 3))
        assertEquals("1 1/4 cups", cups(1.25))
        assertEquals("1/4 cup", cups(0.25))
    }

    @Test
    fun `odd eighths of a cup are written as tablespoons`() {
        assertEquals("6 tbsp", cups(0.375))
        assertEquals("2 tbsp", cups(0.125))
        assertEquals("1/2 cup + 2 tbsp", cups(0.625))
        assertEquals("3/4 cup + 2 tbsp", cups(0.875))
        assertEquals("1 cup + 2 tbsp", cups(1.125))
        assertEquals("1 cup + 6 tbsp", cups(1.375))
        assertEquals("2 1/2 cups + 2 tbsp", cups(2.625))
        assertEquals("3 cups + 2 tbsp", cups(3.125))
    }

    @Test
    fun `small amounts switch to tablespoons and teaspoons`() {
        assertEquals("3 tbsp", MeasureFormatter.formatVolume(9.6))
        assertEquals("1 tbsp", MeasureFormatter.formatVolume(3.0))
        assertEquals("1 1/2 tbsp", MeasureFormatter.formatVolume(4.5))
        assertEquals("1/4 cup", MeasureFormatter.formatVolume(11.5))
        assertEquals("1/4 tsp", MeasureFormatter.formatVolume(0.25))
        assertEquals("3/4 tsp", MeasureFormatter.formatVolume(0.75))
        assertEquals("1 1/2 tsp", MeasureFormatter.formatVolume(1.5))
        assertEquals("2 1/2 tsp", MeasureFormatter.formatVolume(2.5))
        assertEquals("a pinch", MeasureFormatter.formatVolume(0.03))
        assertEquals("3 tsp", MeasureFormatter.formatTeaspoons(3.0))
    }

    @Test
    fun `weights are rounded without false precision`() {
        assertEquals("240 g", MeasureFormatter.formatGrams(240.0))
        assertEquals("115 g", MeasureFormatter.formatGrams(113.5))
        assertEquals("5 g", MeasureFormatter.formatGrams(4.6))
        assertEquals("120 ml", MeasureFormatter.formatMl(120.0))
    }

    @Test
    fun `flour labels are tidy`() {
        assertEquals("1 1/2 cups", MeasureFormatter.formatFlourCups(1.5))
        assertEquals("1 3/4 cups", MeasureFormatter.formatFlourCups(1.75))
        assertEquals("1/2 cup", MeasureFormatter.formatFlourCups(0.5))
        assertEquals("1 cup", MeasureFormatter.formatFlourCups(1.0))
        assertEquals("1.3 cups", MeasureFormatter.formatFlourCups(1.3))
        assertEquals("2 cups", MeasureFormatter.formatFlourCups(2.0))
    }

    @Test
    fun `eggs are never shown as awkward decimals`() {
        val egg = IngredientDef("egg", "Eggs", MeasureStyle.COUNT, gramsPerPiece = 50.0,
            pieceSingular = "large egg", piecePlural = "large eggs", rounding = RoundingRule.EGG)
        assertEquals("1 large egg", MeasureFormatter.formatEggs(1.0, egg).text)
        assertEquals("2 large eggs", MeasureFormatter.formatEggs(2.0, egg).text)
        assertEquals("1 large egg + about 1 tbsp beaten egg", MeasureFormatter.formatEggs(1.33, egg).text)
        assertEquals("1 large egg + about 1 1/2 tbsp beaten egg", MeasureFormatter.formatEggs(1.5, egg).text)
        assertEquals("about 1 1/2 tbsp beaten egg (about 1/2 of an egg)", MeasureFormatter.formatEggs(0.5, egg).text)
        assertEquals("3 large eggs", MeasureFormatter.formatEggs(2.9, egg).text)
        for (n in listOf(0.25, 0.75, 1.1, 1.25, 1.75, 2.25, 3.33, 4.5)) {
            val text = MeasureFormatter.formatEggs(n, egg).text
            assert(!text.contains(Regex("\\d\\.\\d"))) { "decimal in '$text' for $n" }
        }
    }

    @Test
    fun `conversions use each ingredient's own density`() {
        val flour = IngredientDef("f", "Flour", gramsPerCup = 120.0)
        val butter = IngredientDef("b", "Butter", gramsPerCup = 227.0)
        val milk = IngredientDef("m", "Milk", MeasureStyle.LIQUID, gramsPerCup = 245.0)
        assertEquals(120.0, MeasureFormatter.toGrams(1.0, MeasureUnit.CUP, flour)!!, 1e-9)
        assertEquals(227.0, MeasureFormatter.toGrams(1.0, MeasureUnit.CUP, butter)!!, 1e-9)
        assertEquals(14.1875, MeasureFormatter.toGrams(1.0, MeasureUnit.TBSP, butter)!!, 1e-9)
        assertEquals(245.0, MeasureFormatter.toGrams(1.0, MeasureUnit.CUP, milk)!!, 1e-9)
        assertEquals(2.0, MeasureFormatter.toCups(240.0, MeasureUnit.GRAM, flour)!!, 1e-9)
    }
}
