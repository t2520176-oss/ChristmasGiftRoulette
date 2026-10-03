package com.example.christmasgiftroulette.game

import com.example.christmasgiftroulette.model.AmountParseResult
import com.example.christmasgiftroulette.model.AmountParser
import com.example.christmasgiftroulette.model.GiftDraft
import com.example.christmasgiftroulette.model.GiftItem

data class RowValidation(val nameError: String? = null, val amountError: String? = null) {
    val isValid: Boolean get() = nameError == null && amountError == null
}

data class ValidationResult(
    val gifts: List<GiftItem>,
    /** Only rows that have a problem are present. */
    val rowErrors: Map<String, RowValidation>,
    val generalError: String?,
) {
    val isValid: Boolean get() = generalError == null && rowErrors.isEmpty()
}

object GiftValidator {
    fun validateRow(draft: GiftDraft): RowValidation {
        val nameError = if (draft.name.isBlank()) "Gift name is required" else null
        val amountError = (AmountParser.parse(draft.amountText, draft.currency) as? AmountParseResult.Invalid)?.message
        return RowValidation(nameError, amountError)
    }

    fun validate(drafts: List<GiftDraft>): ValidationResult {
        val rowErrors = LinkedHashMap<String, RowValidation>()
        val gifts = ArrayList<GiftItem>(drafts.size)
        for (draft in drafts) {
            val row = validateRow(draft)
            if (!row.isValid) {
                rowErrors[draft.id] = row
                continue
            }
            val amount = (AmountParser.parse(draft.amountText, draft.currency) as AmountParseResult.Valid).amount
            gifts += GiftItem(draft.id, draft.name.trim(), amount, draft.currency)
        }
        val general = when {
            drafts.size < GameRules.MIN_GIFTS -> "Add at least ${GameRules.MIN_GIFTS} gifts to start"
            drafts.size > GameRules.MAX_GIFTS -> "A game can have at most ${GameRules.MAX_GIFTS} gifts"
            rowErrors.isNotEmpty() -> {
                val missing = rowErrors.values.count { it.nameError != null }
                if (missing > 0) "Please name every gift ($missing missing) and fix any highlighted amounts"
                else "Please fix the highlighted amounts"
            }
            else -> null
        }
        return ValidationResult(gifts, rowErrors, general)
    }
}
