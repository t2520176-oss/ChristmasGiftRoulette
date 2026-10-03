package com.example.christmasgiftroulette.model

import java.math.BigDecimal

/**
 * A single gift. Identity is the [id] — never the name — so duplicate names are fine.
 * [amount] is optional; money uses [BigDecimal] to avoid floating point errors.
 */
data class GiftItem(
    val id: String,
    val name: String,
    val amount: BigDecimal?,
    val currency: CurrencyType,
    val icon: GiftIconType = GiftIconType.GIFT,
) {
    /** e.g. "₩15,000" or null when the gift has no amount. */
    val formattedAmount: String?
        get() = amount?.let { CurrencyFormatter.format(it, currency) }
}

/** Editable (not yet validated) gift row on the setup screen. */
data class GiftDraft(
    val id: String,
    val name: String = "",
    val amountText: String = "",
    val currency: CurrencyType = CurrencyType.DEFAULT,
    val icon: GiftIconType = GiftIconType.GIFT,
)
