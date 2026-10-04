package com.lifeyourchoice.core.engine

import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.NpcRole

/**
 * Fills placeholders in story text: `{name}`, `{job}`, `{friend}`, `{friend.he}`, `{partner.His}` ...
 * Unknown placeholders are left untouched so content mistakes are visible (and caught by tests).
 */
object TextTemplate {
    private val token = Regex("\\{([a-z]+)(?:\\.([A-Za-z]+))?\\}")

    val roleKeys: Map<String, NpcRole> = mapOf(
        "friend" to NpcRole.BEST_FRIEND,
        "underdog" to NpcRole.UNDERDOG,
        "rival" to NpcRole.RIVAL,
        "mentor" to NpcRole.MENTOR,
        "partner" to NpcRole.PARTNER,
        "sibling" to NpcRole.SIBLING,
        "boss" to NpcRole.BOSS,
        "coworker" to NpcRole.COWORKER
    )

    private val pronounForms = setOf("he", "him", "his", "himself")

    fun render(text: String, s: GameState): String = token.replace(text) { m ->
        val key = m.groupValues[1]
        val form = m.groupValues[2]
        resolve(key, form, s) ?: m.value
    }

    /** True when every placeholder in [text] is one the renderer understands. */
    fun unresolved(text: String, s: GameState): List<String> =
        token.findAll(render(text, s)).map { it.value }.toList()

    private fun resolve(key: String, form: String, s: GameState): String? {
        when (key) {
            "name" -> return s.playerName
            "job" -> return s.jobTitle.lowercase()
            "career" -> return s.career.label.lowercase()
        }
        val role = roleKeys[key] ?: return null
        val npc = s.npcs[role] ?: return null
        if (form.isEmpty()) return npc.name
        val lower = form.lowercase()
        if (lower !in pronounForms) return null
        val boy = npc.gender == Gender.BOY
        val word = when (lower) {
            "he" -> if (boy) "he" else "she"
            "him" -> if (boy) "him" else "her"
            "his" -> if (boy) "his" else "her"
            else -> if (boy) "himself" else "herself"
        }
        return if (form[0].isUpperCase()) word.replaceFirstChar { it.uppercase() } else word
    }
}
