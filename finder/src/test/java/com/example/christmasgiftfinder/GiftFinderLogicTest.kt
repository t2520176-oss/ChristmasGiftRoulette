package com.example.christmasgiftfinder

import com.example.christmasgiftfinder.data.Countries
import com.example.christmasgiftfinder.data.GiftRepository
import com.example.christmasgiftfinder.logic.BudgetError
import com.example.christmasgiftfinder.logic.BudgetParser
import com.example.christmasgiftfinder.logic.BudgetResult
import com.example.christmasgiftfinder.logic.GiftFilter
import com.example.christmasgiftfinder.logic.PriceFormatter
import com.example.christmasgiftfinder.logic.SurprisePicker
import com.example.christmasgiftfinder.model.Gift
import com.example.christmasgiftfinder.model.GiftQuery
import com.example.christmasgiftfinder.model.GiftSearchResult
import com.example.christmasgiftfinder.model.GiftStyle
import com.example.christmasgiftfinder.model.Recipient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import kotlin.random.Random

class GiftFinderLogicTest {

    // Gradle runs unit tests with the module directory as working directory.
    private val gifts: List<Gift> = GiftRepository.parse(File("src/main/assets/gifts.json").readText())

    private fun query(country: String, budget: String, recipient: Recipient, style: GiftStyle) =
        GiftQuery(Countries.byName(country)!!, BigDecimal(budget), recipient, style)

    private fun search(country: String, budget: String, recipient: Recipient, style: GiftStyle) =
        GiftFilter.search(gifts, query(country, budget, recipient, style))

    @Test fun dataSetIsLoadedForEveryCountry() {
        assertTrue(gifts.isNotEmpty())
        for (country in Countries.all) {
            val forCountry = gifts.filter { it.country == country.name }
            assertTrue("${country.name} has ${forCountry.size} gifts", forCountry.size >= 15)
            // Currency in the data matches the country definition.
            assertTrue(forCountry.all { it.currencyCode == country.currencyCode && it.currencySymbol == country.currencySymbol })
        }
        assertTrue(gifts.all { it.minPrice in 0..it.maxPrice })
    }

    @Test fun pricesDifferBetweenCurrencies() {
        val ph = gifts.first { it.country == "Philippines" && it.name == "Mug" }
        val kr = gifts.first { it.country == "South Korea" && it.name == "Mug" }
        val us = gifts.first { it.country == "United States" && it.name == "Mug" }
        assertTrue(ph.maxPrice in 100..300)
        assertTrue(kr.maxPrice in 5000..15000)
        assertTrue(us.maxPrice in 5..15)
    }

    // TEST 1
    @Test fun philippines200AnyoneAny() {
        val r = search("Philippines", "200", Recipient.ANYONE, GiftStyle.ANY)
        assertTrue(r.affordable.size >= 5)
        assertTrue(r.affordable.all { it.maxPrice <= 200 })
        assertTrue(r.stretch.isEmpty())
    }

    // TEST 2
    @Test fun southKorea10000CoworkerUseful() {
        val r = search("South Korea", "10000", Recipient.COWORKER, GiftStyle.USEFUL)
        assertTrue(r.affordable.size >= 3)
        assertTrue(r.affordable.all { it.maxPrice <= 10000 && it.categories.contains("Useful") })
        assertTrue(r.affordable.all { it.recipients.contains("Coworker") || it.recipients.contains("Anyone") })
    }

    // TEST 3
    @Test fun unitedStates20FriendCute() {
        val r = search("United States", "20", Recipient.FRIEND, GiftStyle.CUTE)
        assertTrue(r.affordable.size >= 3)
        assertTrue(r.affordable.all { it.categories.contains("Cute") && it.maxPrice <= 20 })
    }

    // TEST 4
    @Test fun japan2000AnyoneChristmas() {
        val r = search("Japan", "2000", Recipient.ANYONE, GiftStyle.CHRISTMAS)
        assertTrue(r.affordable.size >= 3)
        assertTrue(r.affordable.all { it.categories.contains("Christmas") && it.maxPrice <= 2000 })
    }

    // TEST 5
    @Test fun verySmallBudgetGivesNoResultsAndDoesNotCrash() {
        for (country in Countries.all) {
            val r = GiftFilter.search(gifts, GiftQuery(country, BigDecimal("0.01"), Recipient.ANYONE, GiftStyle.ANY))
            assertTrue(country.name, r.isEmpty)
            assertTrue(r.surprisePool.isEmpty())
            assertNull(SurprisePicker.pick(r.surprisePool))
        }
    }

    @Test fun everyDemoScenarioWorksForEveryCountryAtTypicalBudget() {
        for (country in Countries.all) {
            val r = GiftFilter.search(gifts, GiftQuery(country, BigDecimal(country.typicalBudget), Recipient.ANYONE, GiftStyle.ANY))
            assertTrue("${country.name} typical budget ${country.typicalBudget}", r.affordable.size >= 5)
        }
    }

    @Test fun giftAboveBudgetIsNeverAffordable() {
        // ₱250–₱350 must not show for ₱200; ₱100–₱180 must.
        val r = search("Philippines", "200", Recipient.ANYONE, GiftStyle.ANY)
        assertTrue(r.affordable.none { it.maxPrice > 200 })
        assertTrue(gifts.any { it.country == "Philippines" && it.maxPrice > 200 })
    }

    @Test fun stretchItemsAreOnlyAddedWhenFewAffordableAndFitMinPrice() {
        // Budget 100 PHP: few affordable gifts, some others have minPrice <= 100 but maxPrice > 100.
        val r = search("Philippines", "100", Recipient.ANYONE, GiftStyle.ANY)
        assertTrue(r.stretch.all { it.maxPrice > 100 && it.minPrice <= 100 })
        assertTrue(r.stretch.size <= GiftFilter.MAX_STRETCH)
        assertTrue(r.affordable.size < GiftFilter.MIN_AFFORDABLE_BEFORE_STRETCH || r.stretch.isEmpty())
        val plenty = search("Philippines", "5000", Recipient.ANYONE, GiftStyle.ANY)
        assertTrue(plenty.stretch.isEmpty())
    }

    @Test fun recipientAndStyleFilters() {
        val g = Gift("X", "XXX", "x", "Candle", 1, 2, listOf("Friend"), listOf("Cute"), "", "🕯️")
        assertTrue(GiftFilter.matchesRecipient(g, Recipient.ANYONE))
        assertTrue(GiftFilter.matchesRecipient(g, Recipient.FRIEND))
        assertFalse(GiftFilter.matchesRecipient(g, Recipient.MAN))
        val anyone = g.copy(recipients = listOf("Anyone"))
        assertTrue(GiftFilter.matchesRecipient(anyone, Recipient.MAN))
        assertTrue(GiftFilter.matchesStyle(g, GiftStyle.ANY))
        assertTrue(GiftFilter.matchesStyle(g, GiftStyle.CUTE))
        assertFalse(GiftFilter.matchesStyle(g, GiftStyle.TECH))
    }

    @Test fun budgetParserAcceptsValidNumbers() {
        assertEquals(BigDecimal("200"), (BudgetParser.parse("200") as BudgetResult.Valid).amount)
        assertEquals(BigDecimal("10000"), (BudgetParser.parse(" 10,000 ") as BudgetResult.Valid).amount)
        assertEquals(BigDecimal("20.5"), (BudgetParser.parse("20.5") as BudgetResult.Valid).amount)
        assertEquals(BigDecimal(".5"), (BudgetParser.parse(".5") as BudgetResult.Valid).amount)
    }

    @Test fun budgetParserRejectsInvalidNumbers() {
        fun error(text: String) = (BudgetParser.parse(text) as BudgetResult.Invalid).error
        assertEquals(BudgetError.EMPTY, error(""))
        assertEquals(BudgetError.EMPTY, error("   "))
        assertEquals(BudgetError.INVALID, error("abc"))
        assertEquals(BudgetError.INVALID, error("12abc"))
        assertEquals(BudgetError.INVALID, error("1.2.3"))
        assertEquals(BudgetError.INVALID, error("--5"))
        assertEquals(BudgetError.INVALID, error("."))
        assertEquals(BudgetError.NEGATIVE, error("-5"))
        assertEquals(BudgetError.NEGATIVE, error("-0.5"))
        assertEquals(BudgetError.ZERO, error("0"))
        assertEquals(BudgetError.ZERO, error("0.00"))
        assertEquals(BudgetError.TOO_LARGE, error("1000000000001"))
        assertEquals(BudgetError.TOO_LARGE, error("9".repeat(400)))
    }

    @Test fun veryLargeButAllowedBudgetDoesNotCrashFiltering() {
        val r = search("Indonesia", "1000000000000", Recipient.ANYONE, GiftStyle.ANY)
        assertTrue(r.affordable.size > 5)
    }

    @Test fun priceFormatting() {
        assertEquals("₱200", PriceFormatter.amount("₱", 200))
        assertEquals("₩10,000", PriceFormatter.amount("₩", 10000))
        assertEquals("$20", PriceFormatter.amount("$", BigDecimal("20")))
        assertEquals("¥2,000", PriceFormatter.amount("¥", 2000))
        assertEquals("RM 40", PriceFormatter.amount("RM", 40))
        assertEquals("Rp 150,000", PriceFormatter.amount("Rp", 150000))
        assertEquals("R$ 80", PriceFormatter.amount("R$", 80))
        assertEquals("$20.5", PriceFormatter.amount("$", BigDecimal("20.50")))
        val g = Gift("Philippines", "PHP", "₱", "Mug", 100, 180, listOf("Anyone"), listOf("Cute"), "", "☕")
        assertEquals("₱100 – ₱180", PriceFormatter.range(g))
    }

    @Test fun surprisePickerAvoidsRepeatingWhenPossible() {
        val pool = search("Philippines", "200", Recipient.ANYONE, GiftStyle.ANY).surprisePool
        assertTrue(pool.size > 1)
        val random = Random(42)
        var current = SurprisePicker.pick(pool, null, random)
        assertNotNull(current)
        repeat(200) {
            val next = SurprisePicker.pick(pool, current!!.id, random)!!
            assertNotEquals(current.id, next.id)
            assertTrue(next in pool)
            current = next
        }
        // A single-item pool still returns that item.
        val single = listOf(pool.first())
        assertEquals(pool.first(), SurprisePicker.pick(single, pool.first().id))
    }

    @Test fun surprisePoolFallsBackToStretchItems() {
        val g = Gift("X", "XXX", "x", "Candle", 1, 2, listOf("Anyone"), listOf("Cute"), "", "🕯️")
        val r = GiftSearchResult(emptyList(), listOf(g))
        assertEquals(listOf(g), r.surprisePool)
    }

    @Test fun malformedJsonDoesNotCrash() {
        assertTrue(GiftRepository.parse("not json").isEmpty())
        assertTrue(GiftRepository.parse("[{\"country\":\"X\"}, 5]").isEmpty())
        assertEquals(0, GiftRepository.parse("[]").size)
    }

    @Test fun countrySearch() {
        assertEquals(Countries.all.size, Countries.search("").size)
        assertEquals(listOf("Philippines"), Countries.search("phil").map { it.name })
        assertTrue(Countries.search("EUR").size == 5)
        assertTrue(Countries.search("zzz").isEmpty())
        assertTrue(Countries.all.size >= 21)
    }
}
