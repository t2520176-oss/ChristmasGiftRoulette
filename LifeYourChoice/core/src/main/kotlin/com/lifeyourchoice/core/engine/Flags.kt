package com.lifeyourchoice.core.engine

/**
 * Flags and counters the engine itself reads (summary, endings, achievements, mottos).
 * Story content sets them with `flag("...")` / `count("...")`; tests verify every one is set somewhere.
 */
object Flags {
    const val FIRST_JOB = "first_job"                 // set by the engine on first employment
    const val LOST_JOB = "lost_job"                   // set by Effects.loseJob
    const val STARTED_BUSINESS = "started_business"
    const val BUSINESS_FAILED = "business_failed"
    const val BUSINESS_SUCCESS = "business_success"
    const val COMEBACK = "comeback"
    const val SECOND_CHANCE = "second_chance"
    const val MENTORED = "mentored"
    const val WENT_ABROAD = "went_abroad"
    const val FAMILY_PROTECTED = "family_protected"
    const val FAMILY_FIRST = "family_first"
    const val HELPED_BULLIED_FRIEND = "helped_bullied_friend"
    const val BETRAYED_FRIEND = "betrayed_friend"
    const val CHEATED = "cheated_exam"
    const val SAVED_MONEY = "saved_money"
    const val TOOK_BUSINESS_RISK = "took_business_risk"
    const val IGNORED_PARENTS = "ignored_parents"
    const val WORKED_TOO_MUCH = "worked_too_much"
    const val PROTECTED_REPUTATION = "protected_reputation"
    const val TOOK_SHORTCUT = "took_shortcut"
    const val REFUSED_UNETHICAL = "refused_unethical"
    const val HELPED_COWORKER = "helped_coworker"
    const val RECONCILED = "reconciled"
    const val ADVENTURE = "adventure"
    const val RETIRED = "retired"
    const val KINDNESS_RETURNED = "kindness_returned"  // set when an old kindness pays off
    const val TRUST_BROKEN = "trust_broken"            // a relationship was damaged by dishonesty

    val all = listOf(
        STARTED_BUSINESS, BUSINESS_FAILED, BUSINESS_SUCCESS, COMEBACK, SECOND_CHANCE, MENTORED, WENT_ABROAD,
        FAMILY_PROTECTED, FAMILY_FIRST, HELPED_BULLIED_FRIEND, BETRAYED_FRIEND, CHEATED, SAVED_MONEY,
        TOOK_BUSINESS_RISK, IGNORED_PARENTS, WORKED_TOO_MUCH, PROTECTED_REPUTATION, TOOK_SHORTCUT,
        REFUSED_UNETHICAL, HELPED_COWORKER, RECONCILED, ADVENTURE, RETIRED, KINDNESS_RETURNED, TRUST_BROKEN
    )
    /** Set by the engine rather than by content. */
    val engineSet = setOf(FIRST_JOB, LOST_JOB)

    // Counters
    const val C_OVERWORK = "overwork"
    const val C_CAREER_CHANGES = "career_changes"
    const val C_RISK_TAKEN = "risk_taken"
    const val C_RISK_AVOIDED = "risk_avoided"
    const val C_CRISES = "crises"
    const val C_KINDNESS = "kindness"
    const val C_MISTAKES = "mistakes"
    const val C_EARLY_MISTAKES = "early_mistakes"     // engine: mistakes made before age 28
    const val C_REDEMPTIONS = "redemptions"
    val engineCounters = setOf(C_CAREER_CHANGES, C_EARLY_MISTAKES)
    val contentCounters = listOf(C_OVERWORK, C_RISK_TAKEN, C_RISK_AVOIDED, C_CRISES, C_KINDNESS, C_MISTAKES, C_REDEMPTIONS)
}
