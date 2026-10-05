package com.bakeyourway.core

/**
 * Renders instruction text that refers to ingredients through tokens, so a substitution changes the
 * wording safely (and an omitted ingredient disappears cleanly) without any global find-and-replace.
 *
 * Tokens (each carries the original wording, so without substitutions the text is unchanged):
 *
 *  * `{@butter|melted butter}`  - ingredient with its preparation word ("melted oil" if inherited)
 *  * `{#egg|eggs}`              - the ingredient's plain noun
 *  * `{^butter|Butter}`         - Title Case noun (for step titles)
 *  * `{L|#egg|eggs;#milk|milk;=salt|salt}` - a list written "a, b and c"; omitted items drop out
 *  * `{?vanilla_extract|, then the |vanilla|}` - an optional clause: prefix, ingredient, suffix; the whole
 *    clause disappears when the ingredient is left out
 */
class StepText(private val changes: Map<String, Change>) {

    /** What happened to one ingredient. */
    data class Change(
        /** Nouns that now stand in for it ("oil", or "baking soda" + "cream of tartar"). */
        val nouns: List<String>,
        /** Carry the original preparation word ("melted", "softened") over to the first noun. */
        val inheritAdjective: Boolean,
        val omitted: Boolean,
        /** The original preparation word, if the recipe line had one. */
        val originalAdjective: String?,
        /** A liquid substitute: it drops out of dry-ingredient steps (it is added with the wet ones instead). */
        val dropFromDry: Boolean = false,
    )

    val changedIds: Set<String> get() = changes.keys

    /** [dry] is true for steps that mix dry ingredients, where liquid substitutes must not appear. */
    fun render(text: String, dry: Boolean = false): String {
        if (changes.isEmpty() && !text.contains('{')) return text
        return TOKEN.replace(text) { m -> renderToken(m.groupValues[1], dry) }
    }

    /** Tips and notes written as `@butter,egg: text` are dropped when one of those ingredients was changed. */
    fun filterNote(note: String): String? {
        val m = NOTE_PREFIX.matchEntire(note) ?: return note
        val ids = m.groupValues[1].split(',')
        return if (ids.any { it in changes }) null else m.groupValues[2]
    }

    private fun effective(id: String, dry: Boolean): Change? {
        val change = changes[id] ?: return null
        return if (dry && change.dropFromDry) change.copy(omitted = true) else change
    }

    private fun renderToken(body: String, dry: Boolean): String {
        if (body.startsWith("L|")) {
            val items = body.removePrefix("L|").split(';')
            val parts = items.flatMap { renderListItem(it, dry) }
            return joinNatural(parts)
        }
        val kind = body[0]
        val fields = body.substring(1).split('|')
        return when (kind) {
            '?' -> {
                val (id, before, surface, after) = fields.padded(4)
                val change = effective(id, dry)
                when {
                    change == null -> before + surface + after
                    change.omitted -> ""
                    else -> before + nounsText(change, withAdjective = false, titleCase = false) + after
                }
            }
            else -> {
                val (id, surface) = fields.padded(2)
                val change = effective(id, dry) ?: return surface
                if (change.omitted) return ""
                when (kind) {
                    '@' -> nounsText(change, withAdjective = true, titleCase = false)
                    '^' -> nounsText(change, withAdjective = false, titleCase = true)
                    else -> nounsText(change, withAdjective = false, titleCase = false)
                }
            }
        }
    }

    /** Items of a list token: `#id|surface`, `@id|surface`, `^id|Surface` or `=literal`. */
    private fun renderListItem(item: String, dry: Boolean): List<String> {
        if (item.startsWith("=")) return listOf(item.substring(1))
        val kind = item[0]
        val (id, surface) = item.substring(1).split('|').padded(2)
        val change = effective(id, dry) ?: return listOf(surface)
        if (change.omitted) return emptyList()
        val withAdj = kind == '@'
        val title = kind == '^'
        return if (change.nouns.size <= 1) {
            listOf(nounsText(change, withAdj, title))
        } else {
            change.nouns.mapIndexed { i, noun -> decorate(noun, if (i == 0 && withAdj) change else null, title) }
        }
    }

    private fun nounsText(change: Change, withAdjective: Boolean, titleCase: Boolean): String {
        val parts = change.nouns.mapIndexed { i, noun ->
            decorate(noun, if (i == 0 && withAdjective) change else null, titleCase)
        }
        return parts.joinToString(" and ")
    }

    private fun decorate(noun: String, adjectiveFrom: Change?, titleCase: Boolean): String {
        val adjective = adjectiveFrom?.takeIf { it.inheritAdjective }?.originalAdjective
        val text = if (adjective.isNullOrBlank()) noun else "$adjective $noun"
        return if (titleCase) titleCase(text) else text
    }

    private fun List<String>.padded(n: Int): List<String> = this + List(maxOf(0, n - size)) { "" }

    companion object {
        private val TOKEN = Regex("""\{((?:L\||[@#^?])[^{}]*)\}""")
        private val NOTE_PREFIX = Regex("""@([a-z_,]+): (.*)""", RegexOption.DOT_MATCHES_ALL)
        private val SMALL_WORDS = setOf("of", "and", "the", "a", "or", "with")

        val NONE = StepText(emptyMap())

        fun joinNatural(items: List<String>): String = when (items.size) {
            0 -> ""
            1 -> items[0]
            else -> items.dropLast(1).joinToString(", ") + " and " + items.last()
        }

        fun titleCase(text: String): String = text.split(' ').mapIndexed { i, w ->
            if (i > 0 && w.lowercase() in SMALL_WORDS) w.lowercase() else w.replaceFirstChar { it.uppercase() }
        }.joinToString(" ")

        /** The preparation word of a recipe line: "melted" from "melted", "very cold" from "very cold, cubed". */
        fun adjectiveOf(prep: String?): String? = prep?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() }
    }
}
