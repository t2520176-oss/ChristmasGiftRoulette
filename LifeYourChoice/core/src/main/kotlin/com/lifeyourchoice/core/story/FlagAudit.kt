package com.lifeyourchoice.core.story

/**
 * Records which flags the content *sets* and which it *reads*, so tests can catch typos such as
 * `has("helped_bullied_freind")` that would otherwise silently never be true.
 */
object FlagAudit {
    val defined: MutableSet<String> = linkedSetOf()
    val referenced: MutableSet<String> = linkedSetOf()
    val counterDefined: MutableSet<String> = linkedSetOf()
    val counterReferenced: MutableSet<String> = linkedSetOf()
}
