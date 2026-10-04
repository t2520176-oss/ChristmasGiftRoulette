package com.example.christmasgiftfinder.data

import com.example.christmasgiftfinder.model.Country

/** Demo markets. Currency is determined by the selected country (no conversion in the demo). */
object Countries {
    val all: List<Country> = listOf(
        Country("Philippines", "🇵🇭", "PHP", "₱", "200"),
        Country("United States", "🇺🇸", "USD", "$", "20"),
        Country("South Korea", "🇰🇷", "KRW", "₩", "10000"),
        Country("Japan", "🇯🇵", "JPY", "¥", "2000"),
        Country("United Kingdom", "🇬🇧", "GBP", "£", "15"),
        Country("Canada", "🇨🇦", "CAD", "$", "25"),
        Country("Australia", "🇦🇺", "AUD", "$", "25"),
        Country("Singapore", "🇸🇬", "SGD", "$", "25"),
        Country("Thailand", "🇹🇭", "THB", "฿", "300"),
        Country("Indonesia", "🇮🇩", "IDR", "Rp", "150000"),
        Country("Malaysia", "🇲🇾", "MYR", "RM", "40"),
        Country("India", "🇮🇳", "INR", "₹", "500"),
        Country("Germany", "🇩🇪", "EUR", "€", "20"),
        Country("France", "🇫🇷", "EUR", "€", "20"),
        Country("Italy", "🇮🇹", "EUR", "€", "20"),
        Country("Spain", "🇪🇸", "EUR", "€", "20"),
        Country("Netherlands", "🇳🇱", "EUR", "€", "20"),
        Country("Brazil", "🇧🇷", "BRL", "R$", "80"),
        Country("Mexico", "🇲🇽", "MXN", "$", "300"),
        Country("New Zealand", "🇳🇿", "NZD", "$", "30"),
        Country("United Arab Emirates", "🇦🇪", "AED", "د.إ", "70"),
    )

    fun byName(name: String?): Country? = all.firstOrNull { it.name == name }

    /** Case-insensitive search by country name or currency code; blank query returns everything. */
    fun search(query: String): List<Country> {
        val q = query.trim()
        if (q.isEmpty()) return all
        return all.filter {
            it.name.contains(q, ignoreCase = true) || it.currencyCode.contains(q, ignoreCase = true)
        }
    }
}
