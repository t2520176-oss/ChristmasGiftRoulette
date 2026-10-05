package com.bakeyourway.core

/**
 * The offline substitution database: rules plus the many names people use for ingredients.
 * Everything here comes from local JSON ([SubstitutionFile] and [AliasFile]).
 */
class SubstitutionLibrary(
    val rules: List<IngredientSubstitution>,
    private val nameToIds: Map<String, List<String>>,
) {
    private val byId: Map<String, IngredientSubstitution> = rules.associateBy { it.id }
    private val byOriginal: Map<String, List<IngredientSubstitution>> = rules.groupBy { it.originalIngredient }

    fun rule(id: String): IngredientSubstitution? = byId[id]

    fun rulesFor(ingredientId: String): List<IngredientSubstitution> = byOriginal[ingredientId].orEmpty()

    /** Every ingredient id the typed text may mean ("AP flour", "cooking oil", "icing sugar" ...). */
    fun matchIngredients(text: String): List<String> {
        val norm = normalize(text)
        if (norm.isEmpty()) return emptyList()
        candidates(norm).forEach { c -> nameToIds[c]?.let { return it } }
        // Drop filler words ("unsalted butter", "whole milk", "large eggs") and try again.
        val trimmed = norm.split(' ').filter { it !in NOISE }.joinToString(" ")
        if (trimmed.isNotEmpty() && trimmed != norm) {
            candidates(trimmed).forEach { c -> nameToIds[c]?.let { return it } }
        }
        return emptyList()
    }

    private fun candidates(norm: String): List<String> {
        val out = mutableListOf(norm)
        if (norm.endsWith("ies")) out += norm.removeSuffix("ies") + "y"
        if (norm.endsWith("es")) out += norm.removeSuffix("es")
        if (norm.endsWith("s")) out += norm.removeSuffix("s")
        return out
    }

    companion object {
        val EMPTY = SubstitutionLibrary(emptyList(), emptyMap())

        private val NOISE = setOf(
            "unsalted", "salted", "fresh", "large", "medium", "small", "extra", "pure", "organic", "whole",
            "real", "plain", "regular", "some", "my", "the", "a", "any", "ground", "dry",
        )

        /** Lower case, letters and digits only, single spaces: "All-Purpose Flour" -> "all purpose flour". */
        fun normalize(text: String): String =
            text.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

        /**
         * Builds the lookup from the alias file and the catalog names. Aliases of one ingredient never
         * clash; broad groups ("sugar") list several ids.
         */
        fun build(
            rules: List<IngredientSubstitution>,
            aliasFile: AliasFile,
            ingredients: Map<String, IngredientDef>,
        ): SubstitutionLibrary {
            val map = LinkedHashMap<String, MutableList<String>>()
            fun add(name: String, id: String) {
                val key = normalize(name)
                if (key.isEmpty()) return
                val list = map.getOrPut(key) { mutableListOf() }
                if (id !in list) list += id
            }
            for ((id, def) in ingredients) {
                add(id.replace('_', ' '), id)
                add(def.name, id)
            }
            for ((id, names) in aliasFile.aliases) names.forEach { add(it, id) }
            // Groups are broad words; they must not hide a specific ingredient of the same name.
            for ((word, ids) in aliasFile.groups) {
                val key = normalize(word)
                val existing = map[key]
                if (existing == null) map[key] = ids.toMutableList() else ids.forEach { if (it !in existing) existing += it }
            }
            return SubstitutionLibrary(rules, map)
        }
    }
}
