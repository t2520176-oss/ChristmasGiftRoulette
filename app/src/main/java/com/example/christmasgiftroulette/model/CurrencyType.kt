package com.example.christmasgiftroulette.model

/**
 * Supported currencies. To add another one, append an entry here — the dropdowns,
 * formatting, validation and persistence all pick it up automatically.
 */
enum class CurrencyType(
    val code: String,
    val symbol: String,
    val displayName: String,
    val fractionDigits: Int,
    val flag: String,
) {
    PHP("PHP", "₱", "Philippine Peso", 2, "🇵🇭"),
    KRW("KRW", "₩", "South Korean Won", 0, "🇰🇷"),
    USD("USD", "$", "US Dollar", 2, "🇺🇸"),
    JPY("JPY", "¥", "Japanese Yen", 0, "🇯🇵"),
    EUR("EUR", "€", "Euro", 2, "🇪🇺"),
    GBP("GBP", "£", "British Pound", 2, "🇬🇧"),
    CAD("CAD", "C$", "Canadian Dollar", 2, "🇨🇦"),
    AUD("AUD", "A$", "Australian Dollar", 2, "🇦🇺"),
    CNY("CNY", "¥", "Chinese Yuan", 2, "🇨🇳"),
    SGD("SGD", "S$", "Singapore Dollar", 2, "🇸🇬");

    companion object {
        val DEFAULT: CurrencyType = PHP

        fun fromCode(code: String?): CurrencyType =
            entries.firstOrNull { it.code == code } ?: DEFAULT
    }
}
