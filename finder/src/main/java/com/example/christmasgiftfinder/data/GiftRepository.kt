package com.example.christmasgiftfinder.data

import com.example.christmasgiftfinder.model.Gift
import org.json.JSONArray
import org.json.JSONObject

/** Parses the bundled offline gift list (assets/gifts.json). Malformed entries are skipped, never fatal. */
object GiftRepository {

    fun parse(json: String): List<Gift> {
        val array = try {
            JSONArray(json)
        } catch (e: Exception) {
            return emptyList()
        }
        val gifts = ArrayList<Gift>(array.length())
        for (i in 0 until array.length()) {
            val gift = try {
                parseGift(array.getJSONObject(i))
            } catch (e: Exception) {
                null
            }
            if (gift != null) gifts += gift
        }
        return gifts
    }

    private fun parseGift(o: JSONObject): Gift? {
        val min = o.getInt("minPrice")
        val max = o.getInt("maxPrice")
        if (min < 0 || max < min) return null
        return Gift(
            country = o.getString("country"),
            currencyCode = o.getString("currencyCode"),
            currencySymbol = o.getString("currencySymbol"),
            name = o.getString("giftName"),
            minPrice = min,
            maxPrice = max,
            recipients = o.getJSONArray("recipients").toStringList(),
            categories = o.getJSONArray("categories").toStringList(),
            description = o.optString("description", ""),
            icon = o.optString("icon", "🎁"),
        )
    }

    private fun JSONArray.toStringList(): List<String> = List(length()) { getString(it) }
}
