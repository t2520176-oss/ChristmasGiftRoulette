package com.example.christmasgiftroulette

import com.example.christmasgiftroulette.model.AmountParseResult
import com.example.christmasgiftroulette.model.AmountParser
import com.example.christmasgiftroulette.model.CurrencyFormatter
import com.example.christmasgiftroulette.model.CurrencyType
import com.example.christmasgiftroulette.model.GiftItem
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    private fun fmt(value: String, currency: CurrencyType) = CurrencyFormatter.format(BigDecimal(value), currency)

    @Test
    fun formatsEachCurrencyWithItsOwnConventions() {
        assertEquals("₩50,000", fmt("50000", CurrencyType.KRW))
        assertEquals("$25.00", fmt("25", CurrencyType.USD))
        assertEquals("¥3,000", fmt("3000", CurrencyType.JPY))
        assertEquals("€20.00", fmt("20", CurrencyType.EUR))
        assertEquals("₱500.00", fmt("500", CurrencyType.PHP))
        assertEquals("£15.00", fmt("15", CurrencyType.GBP))
        assertEquals("C$1,234.50", fmt("1234.5", CurrencyType.CAD))
        assertEquals("A$9.99", fmt("9.99", CurrencyType.AUD))
        assertEquals("¥88.00", fmt("88", CurrencyType.CNY))
        assertEquals("S$1,000,000.00", fmt("1000000", CurrencyType.SGD))
    }

    @Test
    fun krwAndJpyHaveNoDecimals() {
        assertEquals("₩15,000", fmt("15000", CurrencyType.KRW))
        assertEquals("¥1,800", fmt("1800.00", CurrencyType.JPY))
    }

    @Test
    fun decimalCurrenciesKeepTwoPlacesWithoutFloatingPointDrift() {
        assertEquals("$0.30", fmt("0.30", CurrencyType.USD))
        assertEquals("$0.10", fmt("0.1", CurrencyType.USD))
        assertEquals("$1.01", fmt("1.005", CurrencyType.USD)) // HALF_UP, exact decimal arithmetic
    }

    @Test
    fun giftItemFormatsOnlyWhenAmountPresent() {
        assertNull(GiftItem("a", "Mystery", null, CurrencyType.USD).formattedAmount)
        assertEquals("€25.00", GiftItem("b", "Perfume", BigDecimal("25"), CurrencyType.EUR).formattedAmount)
    }

    @Test
    fun parserAcceptsBlankAsNoAmount() {
        assertEquals(AmountParseResult.Valid(null), AmountParser.parse("   ", CurrencyType.USD))
        assertEquals(AmountParseResult.Valid(null), AmountParser.parse("", CurrencyType.KRW))
    }

    @Test
    fun parserAcceptsGroupingAndDecimals() {
        val krw = AmountParser.parse("50,000", CurrencyType.KRW) as AmountParseResult.Valid
        assertEquals(0, BigDecimal("50000").compareTo(krw.amount))
        val usd = AmountParser.parse("12.5", CurrencyType.USD) as AmountParseResult.Valid
        assertEquals("12.50", usd.amount.toString())
        val krwTrailing = AmountParser.parse("1500.0", CurrencyType.KRW) as AmountParseResult.Valid
        assertEquals("1500", krwTrailing.amount.toString())
    }

    @Test
    fun parserRejectsInvalidInput() {
        listOf("abc", "-5", "1e5", "12.3.4", "$5", "1,2,x").forEach {
            assertTrue("'$it' should be invalid", AmountParser.parse(it, CurrencyType.USD) is AmountParseResult.Invalid)
        }
        assertTrue(AmountParser.parse("10.5", CurrencyType.KRW) is AmountParseResult.Invalid)
        assertTrue(AmountParser.parse("10.505", CurrencyType.USD) is AmountParseResult.Invalid)
        assertTrue(AmountParser.parse("1234567890123", CurrencyType.USD) is AmountParseResult.Invalid)
    }

    @Test
    fun currencyLookupFallsBackToDefault() {
        assertEquals(CurrencyType.EUR, CurrencyType.fromCode("EUR"))
        assertEquals(CurrencyType.DEFAULT, CurrencyType.fromCode("XXX"))
        assertEquals(CurrencyType.DEFAULT, CurrencyType.fromCode(null))
    }
}
