package com.example.christmasgiftroulette.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Formats money as symbol + grouped digits, e.g. ₩50,000 / $25.00. No conversion is ever done. */
object CurrencyFormatter {
    private val symbols = DecimalFormatSymbols(Locale.US)

    fun format(amount: BigDecimal, currency: CurrencyType): String {
        val pattern = buildString {
            append("#,##0")
            if (currency.fractionDigits > 0) {
                append('.')
                repeat(currency.fractionDigits) { append('0') }
            }
        }
        val formatter = DecimalFormat(pattern, symbols).apply { roundingMode = RoundingMode.HALF_UP }
        return currency.symbol + formatter.format(amount)
    }
}

sealed interface AmountParseResult {
    /** [amount] is null when the field was left blank (which is valid). */
    data class Valid(val amount: BigDecimal?) : AmountParseResult
    data class Invalid(val message: String) : AmountParseResult
}

object AmountParser {
    private const val MAX_INTEGER_DIGITS = 12
    private val pattern = Regex("^\\d{1,$MAX_INTEGER_DIGITS}(\\.\\d+)?$")

    fun parse(text: String, currency: CurrencyType): AmountParseResult {
        val cleaned = text.trim().replace(",", "").replace(" ", "")
        if (cleaned.isEmpty()) return AmountParseResult.Valid(null)
        if (!pattern.matches(cleaned)) {
            return AmountParseResult.Invalid("Enter a valid amount using digits only")
        }
        val value = BigDecimal(cleaned)
        val usedScale = maxOf(value.stripTrailingZeros().scale(), 0)
        if (usedScale > currency.fractionDigits) {
            val message = if (currency.fractionDigits == 0) {
                "${currency.code} has no decimal places"
            } else {
                "${currency.code} allows up to ${currency.fractionDigits} decimal places"
            }
            return AmountParseResult.Invalid(message)
        }
        return AmountParseResult.Valid(value.setScale(currency.fractionDigits, RoundingMode.UNNECESSARY))
    }
}
