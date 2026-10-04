package com.lifeyourchoice.core

import com.lifeyourchoice.core.engine.Flags
import com.lifeyourchoice.core.engine.TextTemplate
import com.lifeyourchoice.core.model.Category
import com.lifeyourchoice.core.story.FlagAudit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentTest {
    private val lib = TestSupport.library

    @Test fun contentIsConsistent() {
        val problems = lib.validate()
        assertTrue("Content problems:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test fun hasAtLeast100PlayableScenarios() {
        println("Scenarios: ${lib.all.size}")
        lib.all.groupBy { it.category }.toSortedMap().forEach { (c, l) -> println("  $c: ${l.size}") }
        assertTrue("only ${lib.all.size} scenarios", lib.all.size >= 100)
        // Each category requested in the brief is represented.
        for (c in listOf(Category.SCHOOL, Category.FAMILY, Category.FRIENDSHIP, Category.CAREER, Category.MONEY,
            Category.LOVE, Category.BUSINESS, Category.MAJOR, Category.RANDOM)) {
            assertTrue("category $c has too few scenarios", lib.all.count { it.category == c } >= 8)
        }
    }

    @Test fun everyReferencedFlagIsSomewhereSet() {
        val undefined = FlagAudit.referenced - FlagAudit.defined - Flags.engineSet
        assertTrue("Flags read but never set (typo?): $undefined", undefined.isEmpty())
        val engineFlagsMissing = Flags.all.filter { it !in FlagAudit.defined && it !in Flags.engineSet }
        assertTrue("Engine flags never set by content: $engineFlagsMissing", engineFlagsMissing.isEmpty())
        val counters = FlagAudit.counterReferenced - FlagAudit.counterDefined - Flags.engineCounters
        assertTrue("Counters read but never changed: $counters", counters.isEmpty())
        val engineCountersMissing = Flags.contentCounters.filter { it !in FlagAudit.counterDefined }
        assertTrue("Engine counters never set by content: $engineCountersMissing", engineCountersMissing.isEmpty())
    }

    @Test fun allPlaceholdersResolve() {
        val s = TestSupport.freshState()
        val bad = mutableListOf<String>()
        for (sc in lib.all) {
            val texts = listOf(sc.text) + sc.extras.map { it.second } +
                sc.choices.flatMap { c -> listOf(c.text, c.result, c.badge ?: "", c.lockedHint ?: "") + c.outcomes.map { it.result } }
            for (t in texts) {
                val left = TextTemplate.unresolved(t, s)
                if (left.isNotEmpty()) bad += "${sc.id}: $left in “$t”"
            }
        }
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun everyScenarioIsReachable() {
        // A scenario must be reachable from the director (age window + not branch-only), a follow-up or a schedule.
        val referenced = lib.all.flatMap { sc -> sc.choices.flatMap { it.refs } }.toSet() + "sch_skip_school"
        val unreachable = lib.all.filter { it.branchOnly && it.id !in referenced }.map { it.id }
        assertTrue("Branch-only scenarios nobody links to: $unreachable", unreachable.isEmpty())
    }

    @Test fun introExists() {
        assertTrue(lib["sch_skip_school"] != null)
        assertEquals(4, lib["sch_skip_school"]!!.choices.size)
    }
}
