package com.lifeyourchoice.core

import com.lifeyourchoice.core.engine.Endings
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.motto.MottoEngine
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulationTest {

    @Test fun thousandsOfLivesAlwaysEndCleanly() {
        val used = mutableMapOf<String, Int>()
        val endings = mutableMapOf<String, Int>()
        val mottoCats = mutableMapOf<String, Int>()
        var total = 0; var steps = 0; var ageSum = 0
        val stats = Stat.values().associateWith { 0L }.toMutableMap()
        val perStyle = TestSupport.Style.values()
        val lives = 2500
        for (seed in 1L..lives) {
            val style = perStyle[(seed % perStyle.size).toInt()]
            val r = TestSupport.playLife(seed, style)
            total++; steps += r.steps; ageSum += r.engine.state.ageYears
            r.playedIds.forEach { used.merge(it, 1, Int::plus) }
            val st = r.engine.state
            for (k in Stat.values()) {
                val v = st.stat(k)
                assertTrue("stat $k out of range: $v", v in 0..100)
                stats[k] = stats.getValue(k) + v
            }
            endings.merge(Endings.select(st).id, 1, Int::plus)
            mottoCats.merge(MottoEngine.bestCategory(st).label, 1, Int::plus)
            assertTrue("life too short: ${r.steps} steps", r.steps >= 25)
            assertTrue("life too long: ${r.steps} steps", r.steps <= 120)
        }
        println("Lives: $total, avg decisions/life: ${steps / total}, avg final age: ${ageSum / total}")
        println("Distinct scenarios used: ${used.size} / ${TestSupport.library.all.size}")
        println("Never played: " + TestSupport.library.all.map { it.id }.filter { it !in used })
        println("Average final stats: " + stats.entries.joinToString { "${it.key.label}=${it.value / total}" })
        println("Endings: " + endings.entries.sortedByDescending { it.value }.joinToString { "${it.key}=${it.value}" })
        println("Motto categories: " + mottoCats.entries.sortedByDescending { it.value }.joinToString { "${it.key}=${it.value}" })
        assertTrue("too few distinct scenarios: ${used.size}", used.size >= 100)
        assertTrue("too few distinct endings: ${endings.size}", endings.size >= 8)
    }

    @Test fun goalDrivenPlayStylesReachTheirEndings() {
        for (style in listOf(TestSupport.Style.BALANCED, TestSupport.Style.MONEY_FIRST, TestSupport.Style.FAMILY_FIRST, TestSupport.Style.RISK_AVERSE)) {
            val endings = mutableMapOf<String, Int>()
            val cats = mutableMapOf<String, Int>()
            val stats = Stat.values().associateWith { 0L }.toMutableMap()
            val n = 150
            for (seed in 1L..n) {
                val st = TestSupport.playLife(seed + 5000, style).engine.state
                endings.merge(Endings.select(st).id, 1, Int::plus)
                cats.merge(MottoEngine.bestCategory(st).label, 1, Int::plus)
                for (k in Stat.values()) stats[k] = stats.getValue(k) + st.stat(k)
            }
            println("[$style] stats: " + stats.entries.joinToString { "${it.key.label}=${it.value / n}" })
            println("[$style] endings: " + endings.entries.sortedByDescending { it.value }.joinToString { "${it.key}=${it.value}" })
            println("[$style] mottos: " + cats.entries.sortedByDescending { it.value }.joinToString { "${it.key}=${it.value}" })
        }
    }

    @Test fun differentSeedsGiveDifferentLives() {
        val a = TestSupport.playLife(11, TestSupport.Style.RANDOM).playedIds
        val b = TestSupport.playLife(12, TestSupport.Style.RANDOM).playedIds
        assertTrue("two lives were identical", a != b)
    }

    @Test fun sameSeedAndChoicesIsDeterministic() {
        val a = TestSupport.playLife(77, TestSupport.Style.RANDOM)
        val b = TestSupport.playLife(77, TestSupport.Style.RANDOM)
        assertTrue(a.playedIds == b.playedIds)
        assertTrue(a.engine.state.ageYears == b.engine.state.ageYears)
    }
}
