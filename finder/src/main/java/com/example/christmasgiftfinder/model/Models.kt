package com.example.christmasgiftfinder.model

import com.example.christmasgiftfinder.R
import java.math.BigDecimal

/** A demo market. [name] must match the "country" field of the entries in assets/gifts.json. */
data class Country(
    val name: String,
    val flag: String,
    val currencyCode: String,
    val currencySymbol: String,
    /** A typical exchange-gift budget in the local currency, used as the pre-filled amount. */
    val typicalBudget: String,
)

/** [key] matches the values used in the "recipients" list of gifts.json. */
enum class Recipient(val key: String, val icon: String, val labelRes: Int) {
    ANYONE("Anyone", "👥", R.string.recipient_anyone),
    MAN("Man", "👨", R.string.recipient_man),
    WOMAN("Woman", "👩", R.string.recipient_woman),
    CHILD("Child", "🧒", R.string.recipient_child),
    COWORKER("Coworker", "👔", R.string.recipient_coworker),
    FRIEND("Friend", "🤝", R.string.recipient_friend),
    FAMILY("Family", "👪", R.string.recipient_family);

    companion object {
        fun fromKey(key: String?): Recipient = entries.firstOrNull { it.key == key } ?: ANYONE
    }
}

/** [key] matches the values used in the "categories" list of gifts.json ("Any" means no filter). */
enum class GiftStyle(val key: String, val icon: String, val labelRes: Int) {
    ANY("Any", "🎁", R.string.style_any),
    USEFUL("Useful", "🛠️", R.string.style_useful),
    CUTE("Cute", "❤️", R.string.style_cute),
    FUNNY("Funny", "😄", R.string.style_funny),
    FOOD("Food", "🍔", R.string.style_food),
    OFFICE("Office", "💼", R.string.style_office),
    TECH("Tech", "📱", R.string.style_tech),
    CHRISTMAS("Christmas", "🎄", R.string.style_christmas);

    companion object {
        fun fromKey(key: String?): GiftStyle = entries.firstOrNull { it.key == key } ?: ANY
    }
}

/** One demo gift idea with an approximate price range in the local currency (not a real store price). */
data class Gift(
    val country: String,
    val currencyCode: String,
    val currencySymbol: String,
    val name: String,
    val minPrice: Int,
    val maxPrice: Int,
    val recipients: List<String>,
    val categories: List<String>,
    val description: String,
    /** Emoji shown as the bundled illustration (no images are downloaded). */
    val icon: String,
) {
    /** Stable identity used for favorites. */
    val id: String get() = "$country|$name"
}

data class GiftQuery(
    val country: Country,
    val budget: BigDecimal,
    val recipient: Recipient,
    val style: GiftStyle,
)

/**
 * [affordable] gifts have maxPrice <= budget. [stretch] gifts only have minPrice <= budget and are
 * shown (clearly marked) when there are too few affordable ones.
 */
data class GiftSearchResult(
    val affordable: List<Gift>,
    val stretch: List<Gift>,
) {
    val isEmpty: Boolean get() = affordable.isEmpty() && stretch.isEmpty()

    /** Gifts the "Surprise me" button picks from: affordable ones, or the marked stretch ones if none. */
    val surprisePool: List<Gift> get() = affordable.ifEmpty { stretch }
}
