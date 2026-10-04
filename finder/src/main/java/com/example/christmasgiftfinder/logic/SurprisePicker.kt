package com.example.christmasgiftfinder.logic

import com.example.christmasgiftfinder.model.Gift
import kotlin.random.Random

object SurprisePicker {
    /**
     * Picks one random gift from [pool]. When [excludeId] is given and another gift exists, the
     * previous one is not repeated. Returns null for an empty pool.
     */
    fun pick(pool: List<Gift>, excludeId: String? = null, random: Random = Random.Default): Gift? {
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id != excludeId }.ifEmpty { pool }
        return candidates[random.nextInt(candidates.size)]
    }
}
