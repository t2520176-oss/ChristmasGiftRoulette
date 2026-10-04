package com.bakeyourway.core

import com.bakeyourway.core.TestSupport.catalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test
    fun `catalog has at least 30 recipes`() {
        assertTrue("only ${catalog.recipes.size} recipes", catalog.recipes.size >= 30)
    }

    @Test
    fun `catalog data is internally consistent`() {
        val problems = CatalogValidator.validate(catalog)
        assertTrue("Catalog problems:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `every category has recipes and the required names exist`() {
        for (c in RecipeCategory.entries) {
            assertTrue("no recipes in $c", catalog.recipesIn(c).isNotEmpty())
        }
        val names = catalog.recipes.map { it.name }.toSet()
        listOf(
            "Blueberry Muffins", "Chocolate Chip Muffins", "Banana Muffins", "Apple Cinnamon Muffins", "Vanilla Muffins",
            "Chocolate Chip Cookies", "Sugar Cookies", "Oatmeal Cookies", "Peanut Butter Cookies", "Double Chocolate Cookies",
            "Vanilla Cupcakes", "Chocolate Cupcakes", "Strawberry Cupcakes", "Banana Cupcakes",
            "Basic White Bread", "Milk Bread", "Banana Bread", "Cinnamon Bread", "Simple Dinner Rolls",
            "Classic Pancakes", "Banana Pancakes", "Blueberry Pancakes", "Chocolate Pancakes",
            "Classic Brownies", "Chocolate Brownies", "Walnut Brownies",
            "Classic Scones", "Raisin Scones", "Blueberry Scones", "Chocolate Chip Scones",
        ).forEach { assertTrue("missing recipe $it", it in names) }
    }

    @Test
    fun `less sugar reduction is recipe specific and never one universal percentage`() {
        val multipliers = catalog.recipes.map { it.lessSugarMultiplier }.toSet()
        assertTrue("expected several different reductions, got $multipliers", multipliers.size >= 4)
        assertTrue(catalog.recipes.all { it.lessSugarMultiplier < it.normalSugarMultiplier })
        assertTrue(catalog.recipes.all { !it.lessSugarNote.isNullOrBlank() })
    }

    @Test
    fun `cooking methods are limited to what suits each recipe`() {
        fun methods(id: String) = TestSupport.recipe(id).supportedCookingMethods.toSet()

        assertEquals(setOf(CookingMethod.OVEN, CookingMethod.AIR_FRYER, CookingMethod.STEAM), methods("blueberry_muffins"))
        assertEquals(setOf(CookingMethod.OVEN, CookingMethod.AIR_FRYER), methods("chocolate_chip_cookies"))
        assertEquals(setOf(CookingMethod.STOVETOP), methods("classic_pancakes"))
        assertEquals(setOf(CookingMethod.OVEN), methods("banana_bread"))
        assertEquals(setOf(CookingMethod.OVEN), methods("apple_pie"))

        for (m in CookingMethod.entries) {
            assertTrue("no recipe supports $m", catalog.recipes.any { m in it.supportedCookingMethods })
        }
        catalog.recipesIn(RecipeCategory.PANCAKES).forEach { assertEquals(setOf(CookingMethod.STOVETOP), it.supportedCookingMethods.toSet()) }
        catalog.recipesIn(RecipeCategory.COOKIES).forEach {
            assertTrue(CookingMethod.STEAM !in it.supportedCookingMethods && CookingMethod.STOVETOP !in it.supportedCookingMethods)
        }
    }

    @Test
    fun `blueberry muffins use the documented cooking profiles`() {
        val r = TestSupport.recipe("blueberry_muffins")
        val oven = r.profileFor(CookingMethod.OVEN)!!
        assertEquals(180, oven.temperatureC); assertEquals(350, oven.temperatureF)
        assertEquals(18, oven.timeMin); assertEquals(22, oven.timeMax); assertTrue(oven.preheat)
        val air = r.profileFor(CookingMethod.AIR_FRYER)!!
        assertEquals(160, air.temperatureC); assertEquals(320, air.temperatureF)
        assertEquals(12, air.timeMin); assertEquals(16, air.timeMax)
        val steam = r.profileFor(CookingMethod.STEAM)!!
        assertEquals(15, steam.timeMin); assertEquals(20, steam.timeMax)
        assertEquals("Medium steam", steam.heatLevel)
    }

    @Test
    fun `search finds recipes by name and category`() {
        assertTrue(catalog.search("blueberry").any { it.id == "blueberry_muffins" })
        assertTrue(catalog.search("muffins").size >= 5)
        assertEquals(catalog.recipes.size, catalog.search("  ").size)
        assertTrue(catalog.search("zzzzqq").isEmpty())
    }

    @Test
    fun `the app declares no internet permission`() {
        val manifest = TestSupport.manifest
        assertTrue("manifest not found at $manifest", manifest.exists())
        val text = manifest.readText()
        assertTrue("INTERNET permission must not be declared", !text.contains("android.permission.INTERNET"))
        assertTrue(!text.contains("ACCESS_NETWORK_STATE"))
    }
}
