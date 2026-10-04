package com.example.christmasgiftfinder.logic

import com.example.christmasgiftfinder.R
import java.math.BigDecimal

enum class BudgetError(val messageRes: Int) {
    EMPTY(R.string.error_budget_empty),
    INVALID(R.string.error_budget_invalid),
    NEGATIVE(R.string.error_budget_negative),
    ZERO(R.string.error_budget_zero),
    TOO_LARGE(R.string.error_budget_too_large),
}

sealed interface BudgetResult {
    data class Valid(val amount: BigDecimal) : BudgetResult
    data class Invalid(val error: BudgetError) : BudgetResult
}

/** Validates the budget text typed by the user (in the selected country's currency). */
object BudgetParser {
    /** Anything above this is rejected so very large inputs can never overflow or look silly. */
    val MAX_BUDGET: BigDecimal = BigDecimal("1000000000000")

    private val NUMBER = Regex("^-?(\\d+(\\.\\d*)?|\\.\\d+)$")

    fun parse(text: String): BudgetResult {
        // Allow thousands separators and spaces ("10,000", "10 000").
        val cleaned = text.trim().replace(",", "").replace(" ", "")
        if (cleaned.isEmpty()) return BudgetResult.Invalid(BudgetError.EMPTY)
        if (!NUMBER.matches(cleaned)) return BudgetResult.Invalid(BudgetError.INVALID)
        // Very long digit strings are still fine for BigDecimal; no Long/Double overflow is possible.
        val value = BigDecimal(cleaned)
        return when {
            value.signum() < 0 -> BudgetResult.Invalid(BudgetError.NEGATIVE)
            value.signum() == 0 -> BudgetResult.Invalid(BudgetError.ZERO)
            value > MAX_BUDGET -> BudgetResult.Invalid(BudgetError.TOO_LARGE)
            else -> BudgetResult.Valid(value)
        }
    }
}
