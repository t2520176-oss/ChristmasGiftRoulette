package com.example.christmasgiftfinder.logic

import com.example.christmasgiftfinder.model.Gift
import com.example.christmasgiftfinder.model.GiftQuery
import com.example.christmasgiftfinder.model.GiftSearchResult
import com.example.christmasgiftfinder.model.GiftStyle
import com.example.christmasgiftfinder.model.Recipient
import java.math.BigDecimal

/** Pure filtering logic over the local gift list. */
object GiftFilter {
    /** Below this many affordable gifts, slightly-over-budget gifts are added (and marked). */
    const val MIN_AFFORDABLE_BEFORE_STRETCH = 3
    const val MAX_STRETCH = 6

    fun search(all: List<Gift>, query: GiftQuery): GiftSearchResult {
        val matching = all.filter {
            it.country == query.country.name &&
                matchesRecipient(it, query.recipient) &&
                matchesStyle(it, query.style)
        }

        val affordable = matching
            .filter { BigDecimal(it.maxPrice) <= query.budget }
            .sortedWith(compareByDescending<Gift> { it.maxPrice }.thenBy { it.name })

        val stretch = if (affordable.size >= MIN_AFFORDABLE_BEFORE_STRETCH) {
            emptyList()
        } else {
            matching
                .filter { BigDecimal(it.maxPrice) > query.budget && BigDecimal(it.minPrice) <= query.budget }
                .sortedWith(compareBy<Gift> { it.minPrice }.thenBy { it.name })
                .take(MAX_STRETCH)
        }
        return GiftSearchResult(affordable, stretch)
    }

    /** "Anyone" matches every gift; otherwise the gift must list the recipient or be good for Anyone. */
    fun matchesRecipient(gift: Gift, recipient: Recipient): Boolean =
        recipient == Recipient.ANYONE ||
            gift.recipients.contains(recipient.key) ||
            gift.recipients.contains(Recipient.ANYONE.key)

    fun matchesStyle(gift: Gift, style: GiftStyle): Boolean =
        style == GiftStyle.ANY || gift.categories.contains(style.key)
}
