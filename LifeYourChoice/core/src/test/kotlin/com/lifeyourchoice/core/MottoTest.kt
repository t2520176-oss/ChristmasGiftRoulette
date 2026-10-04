package com.lifeyourchoice.core

import com.lifeyourchoice.core.model.CareerTrack
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.NpcRole
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.Stat
import com.lifeyourchoice.core.model.Trait
import com.lifeyourchoice.core.motto.LessonEngine
import com.lifeyourchoice.core.motto.LifeReportBuilder
import com.lifeyourchoice.core.motto.MottoCategory
import com.lifeyourchoice.core.motto.MottoEngine
import com.lifeyourchoice.core.motto.MottoLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MottoTest {

    /** A neutral, mid-range life to which each test adds the traits of the scenario it describes. */
    private fun life(seed: Long = 3, block: GameState.() -> Unit): GameState {
        val s = TestSupport.freshState(seed = seed)
        for (k in Stat.values()) s.stats[k] = 50
        s.stats[Stat.CAREER] = 40
        s.ageMonths = 78 * 12
        s.block()
        return s
    }

    @Test fun libraryHasAtLeast100OriginalMottosAcrossAllCategories() {
        assertTrue("only ${MottoLibrary.all.size} mottos", MottoLibrary.all.size >= 100)
        for (c in MottoCategory.values()) {
            assertTrue("$c has ${MottoLibrary.byCategory(c).size} mottos", MottoLibrary.byCategory(c).size >= 5)
        }
        // The 20 categories named in the brief all exist.
        val labels = MottoCategory.values().map { it.label }
        for (needed in listOf("Family", "Friendship", "Success", "Failure", "Comeback", "Money", "Career", "Business", "Health",
            "Love", "Courage", "Discipline", "Kindness", "Trust", "Ambition", "Risk", "Happiness", "Regret", "Second Chances", "Balanced Life")) {
            assertTrue("missing category $needed", needed in labels)
        }
        assertEquals("duplicate motto texts", MottoLibrary.all.size, MottoLibrary.all.map { it.text }.toSet().size)
        assertEquals("duplicate motto ids", MottoLibrary.all.size, MottoLibrary.all.map { it.id }.toSet().size)
        assertTrue(MottoLibrary.all.all { it.text.length in 20..110 })
    }

    @Test fun highMoneyLowFamilyLowHappinessGivesSuccessAtACost() {
        val s = life { stats[Stat.MONEY] = 88; stats[Stat.FAMILY] = 30; stats[Stat.HAPPINESS] = 40 }
        assertEquals(MottoCategory.SUCCESS_AT_A_COST, MottoEngine.bestCategory(s))
    }

    @Test fun businessFailureThenLaterSuccessGivesComeback() {
        val s = life { flags += listOf("business_failed", "comeback"); businessStage = 3; stats[Stat.MONEY] = 70 }
        assertEquals(MottoCategory.COMEBACK, MottoEngine.bestCategory(s))
    }

    @Test fun highFamilyAndHappinessGivesFamily() {
        val s = life { stats[Stat.FAMILY] = 88; stats[Stat.HAPPINESS] = 80 }
        assertEquals(MottoCategory.FAMILY, MottoEngine.bestCategory(s))
    }

    @Test fun highDisciplineAndCareerGivesDiscipline() {
        val s = life { stats[Stat.DISCIPLINE] = 88; stats[Stat.CAREER] = 82; career = CareerTrack.ENGINEER; careerLevel = 3 }
        assertEquals(MottoCategory.DISCIPLINE, MottoEngine.bestCategory(s))
    }

    @Test fun lowHealthWithVeryHighCareerGivesHealth() {
        val s = life {
            stats[Stat.HEALTH] = 25; stats[Stat.CAREER] = 92; career = CareerTrack.MANAGER; careerLevel = 5
            flags += "worked_too_much"; stats[Stat.MONEY] = 75
        }
        assertEquals(MottoCategory.HEALTH, MottoEngine.bestCategory(s))
    }

    @Test fun highFriendshipAndLoyalGivesFriendship() {
        val s = life { stats[Stat.FRIENDSHIP] = 88; traits[Trait.LOYAL] = 40 }
        assertEquals(MottoCategory.FRIENDSHIP, MottoEngine.bestCategory(s))
    }

    @Test fun manyEarlyMistakesThenRedemptionGivesSecondChance() {
        val s = life { counters["early_mistakes"] = 4; counters["redemptions"] = 3; flags += "second_chance"; stats[Stat.HAPPINESS] = 60 }
        assertEquals(MottoCategory.SECOND_CHANCES, MottoEngine.bestCategory(s))
    }

    @Test fun everythingHighGivesBalancedLife() {
        val s = life { for (k in listOf(Stat.MONEY, Stat.FAMILY, Stat.HEALTH, Stat.HAPPINESS)) stats[k] = 78 }
        assertEquals(MottoCategory.BALANCED_LIFE, MottoEngine.bestCategory(s))
    }

    @Test fun riskTakingThatWorkedGivesCourage() {
        val s = life { traits[Trait.RISK_TAKER] = 40; stats[Stat.MONEY] = 72; counters["risk_taken"] = 4; stats[Stat.HAPPINESS] = 55 }
        assertEquals(MottoCategory.COURAGE, MottoEngine.bestCategory(s))
    }

    @Test fun riskTakingThatCostMoneyGivesWisdom() {
        val s = life { traits[Trait.RISK_TAKER] = 40; stats[Stat.MONEY] = 20; counters["risk_taken"] = 4 }
        assertEquals(MottoCategory.RISK_WISDOM, MottoEngine.bestCategory(s))
    }

    @Test fun anOldKindnessThatCameBackGivesKindness() {
        val s = life { flags += "kindness_returned"; stats[Stat.HAPPINESS] = 55 }
        assertEquals(MottoCategory.KINDNESS, MottoEngine.bestCategory(s))
    }

    @Test fun mottoComesFromTheChosenCategoryAndIsDeterministic() {
        val s = life(seed = 9) { stats[Stat.FAMILY] = 88; stats[Stat.HAPPINESS] = 80 }
        val a = MottoEngine.select(s, emptyList())
        val b = MottoEngine.select(s, emptyList())
        assertEquals(a.id, b.id)
        assertEquals(MottoCategory.FAMILY, a.category)
    }

    @Test fun recentMottosAreNotRepeatedWhileAnotherAppropriateOneExists() {
        val s = life { stats[Stat.FAMILY] = 88; stats[Stat.HAPPINESS] = 80 }
        val recent = mutableListOf<String>()
        val seenCategories = mutableSetOf<MottoCategory>()
        for (i in 1..MottoLibrary.all.size) {
            val m = MottoEngine.select(s, recent.takeLast(MottoLibrary.all.size))
            assertTrue("repeated ${m.id} after $i lives", m.id !in recent)
            recent += m.id
            if (i <= 6) assertEquals("first lives stay in the best-fit category", MottoCategory.FAMILY, m.category)
            seenCategories += m.category
        }
        assertTrue(seenCategories.size > 1)
        // Once everything has been awarded, repeating is allowed and must not crash.
        assertNotEquals(null, MottoEngine.select(s, recent))
    }

    @Test fun lessonsAreExactlyThreeDistinctAndDeterministic() {
        for (seed in 1L..200L) {
            val st = TestSupport.playLife(seed, TestSupport.Style.values()[(seed % TestSupport.Style.values().size).toInt()]).engine.state
            val lessons = LessonEngine.select(st)
            assertEquals(3, lessons.size)
            assertEquals(3, lessons.map { it.theme }.toSet().size)
            assertEquals(lessons.map { it.id }, LessonEngine.select(st).map { it.id })
        }
    }

    @Test fun lessonsFollowThePlayersJourney() {
        val cost = life { stats[Stat.MONEY] = 85; stats[Stat.FAMILY] = 25; stats[Stat.HEALTH] = 35 }
        val ids = LessonEngine.select(cost).map { it.id }
        assertTrue(ids.toString(), "cost_everyone" in ids && "money_health" in ids)

        val betrayal = life { flags += "betrayed_friend" }
        assertTrue("trust_moment" in LessonEngine.select(betrayal).map { it.id })

        val kind = life { flags += listOf("kindness_returned", "helped_bullied_friend") }
        assertEquals("kindness_returns", LessonEngine.select(kind).first().id)

        val failedThenRose = life { flags += listOf("business_failed", "comeback") }
        val rid = LessonEngine.select(failedThenRose).map { it.id }
        assertTrue(rid.toString(), "failure_experience" in rid && "comeback" in rid)
    }

    @Test fun reportContainsStoryLessonsMottoInTheRightShape() {
        val st = TestSupport.playLife(321, TestSupport.Style.BALANCED).engine.state
        val r = LifeReportBuilder.build(st, 1, emptyList(), 123L)
        assertEquals(3, r.lessons.size)
        assertTrue(r.mottoText.isNotBlank() && MottoLibrary.get(r.mottoId)?.text == r.mottoText)
        assertTrue(r.summary.endsWith("."))
        assertTrue(r.importantChoices.isNotEmpty())
        println("STORY: ${r.summary}")
        println("ENDING: ${r.endingTitle} / MOTTO: ${r.mottoText} / LESSONS: ${r.lessons}")
        println("CHOICES: " + r.importantChoices.joinToString { "${it.ageYears}: ${it.text}" })
    }
}
