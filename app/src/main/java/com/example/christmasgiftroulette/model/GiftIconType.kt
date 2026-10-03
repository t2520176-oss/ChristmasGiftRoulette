package com.example.christmasgiftroulette.model

/** Picture shown for a gift. Persisted by [key], so keep keys stable when adding new icons. */
enum class GiftIconType(val key: String, val label: String) {
    GIFT("gift", "Gift"),
    CASH("cash", "Cash"),
    CHOCOLATE("chocolate", "Chocolate"),
    MUG("mug", "Mug"),
    TOY_CAR("toy_car", "Toy"),
    PERFUME("perfume", "Perfume"),
    BOOK("book", "Book"),
    SOCKS("socks", "Socks"),
    CARD("card", "Gift card");

    companion object {
        fun fromKey(key: String?): GiftIconType = entries.firstOrNull { it.key == key } ?: GIFT
    }
}
