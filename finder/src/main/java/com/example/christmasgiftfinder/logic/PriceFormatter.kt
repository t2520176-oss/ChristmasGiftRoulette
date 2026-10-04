package com.example.christmasgiftfinder.logic

import com.example.christmasgiftfinder.model.Gift
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Formats amounts like "₱200", "₩10,000", "RM 40" (symbols made of letters get a space). */
object PriceFormatter {
    private fun decimalFormat() = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

    fun amount(symbol: String, value: BigDecimal): String = withSymbol(symbol, decimalFormat().format(value))

    fun amount(symbol: String, value: Int): String = amount(symbol, BigDecimal(value))

    fun range(gift: Gift): String =
        if (gift.minPrice == gift.maxPrice) {
            amount(gift.currencySymbol, gift.maxPrice)
        } else {
            "${amount(gift.currencySymbol, gift.minPrice)} – ${amount(gift.currencySymbol, gift.maxPrice)}"
        }

    private fun withSymbol(symbol: String, number: String): String =
        if (symbol.any { it.isLetter() }) "$symbol $number" else "$symbol$number"
}
