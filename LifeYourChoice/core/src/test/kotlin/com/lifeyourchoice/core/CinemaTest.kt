package com.lifeyourchoice.core

import com.lifeyourchoice.core.cinema.ActorId
import com.lifeyourchoice.core.cinema.CameraMath
import com.lifeyourchoice.core.cinema.ChapterCards
import com.lifeyourchoice.core.cinema.CineDirector
import com.lifeyourchoice.core.cinema.CineScript
import com.lifeyourchoice.core.cinema.MontageBuilder
import com.lifeyourchoice.core.cinema.Phase
import com.lifeyourchoice.core.cinema.Shot
import com.lifeyourchoice.core.cinema.scripts.CineContent
import com.lifeyourchoice.core.engine.GameEngine
import com.lifeyourchoice.core.model.CareerTrack
import com.lifeyourchoice.core.model.ChoiceOutcome
import com.lifeyourchoice.core.model.Education
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.RelationshipStatus
import com.lifeyourchoice.core.model.Stat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CinemaTest {
    private val story = TestSupport.library
    private val cine = CineContent.library()

    private fun runTo(d: CineDirector, target: Phase, maxSteps: Int = 20_000) {
        var n = 0
        while (d.phase != target && d.phase != Phase.DONE && n++ < maxSteps) d.update(100)
        assertTrue("director stuck in ${d.phase} for ${d.script.scenarioId}", d.phase == target || d.phase == Phase.DONE)
    }

    @Test fun cinematicContentIsConsistentWithTheStory() {
        val problems = cine.validate(story)
        assertTrue("Cinematic problems:\n" + problems.joinToString("\n"), problems.isEmpty())
        println("Hand-written cinematics: ${cine.all.size}")
    }

    @Test fun everyScriptPlaysToItsChoiceAndThroughEveryReaction() {
        val thin = mutableListOf<String>()
        for (script in cine.all) {
            val st = TestSupport.freshState()
            val d = CineDirector(script, st)
            runTo(d, Phase.CHOOSING)
            assertEquals(Phase.CHOOSING, d.phase)
            if (d.linesSpoken < 3) thin += "${script.scenarioId}(${d.linesSpoken})"
            for ((i, _) in script.choices) {
                val st2 = TestSupport.freshState()
                val d2 = CineDirector(script, st2)
                runTo(d2, Phase.CHOOSING)
                // Try every random outcome the story allows for this choice (and the "no outcome" case).
                val outcomes = story[script.scenarioId]!!.choices[i].outcomes.size
                for (o in -1 until outcomes) {
                    val d3 = CineDirector(script, TestSupport.freshState())
                    runTo(d3, Phase.CHOOSING)
                    d3.choose(i, ChoiceOutcome(script.scenarioId, "x", "Something happened.", emptyList(), false, outcomeIndex = o, outcomeText = if (o >= 0) "An outcome." else ""))
                    runTo(d3, Phase.DONE)
                    assertEquals(Phase.DONE, d3.phase)
                }
            }
        }
        assertTrue("scenes with too little dialogue: $thin", thin.isEmpty())
    }

    @Test fun skipFastForwardsToTheChoiceAndThroughTheReaction() {
        val script = cine.byScenario.getValue("sch_skip_school")
        val d = CineDirector(script, TestSupport.freshState())
        d.skip()
        assertEquals(Phase.CHOOSING, d.phase)
        d.choose(3, null)
        d.skip()
        assertEquals(Phase.DONE, d.phase)
    }

    @Test fun tappingSpeedsUpDialogueButNotInstantly() {
        val d = CineDirector(cine.byScenario.getValue("fam_money_trouble"), TestSupport.freshState())
        var guard = 0
        while (d.caption == null && guard++ < 200) d.update(50)
        val first = d.caption!!
        d.tap()                       // too soon: ignored
        d.update(16)
        assertEquals(first.serial, d.caption!!.serial)
        d.update(400)
        assertTrue(d.actor(first.speaker).speaking)
        d.tap()
        d.update(16)
        assertFalse("the line should have ended", d.actor(first.speaker).speaking)
    }

    @Test fun conditionalLinesFollowThePlayersHistory() {
        val script = cine.byScenario.getValue("pay_friend_helps_crisis")
        fun lines(setup: (com.lifeyourchoice.core.model.GameState) -> Unit): List<String> {
            val st = TestSupport.freshState(); setup(st)
            val d = CineDirector(script, st)
            val seen = mutableListOf<String>()
            var n = 0
            while (d.phase == Phase.PLAYING && n++ < 5000) { d.update(50); d.caption?.let { if (it.text !in seen) seen += it.text } }
            return seen
        }
        assertTrue(lines { it.flags += "helped_bullied_friend" }.any { it.contains("helped me when nobody else would") })
        assertTrue(lines { it.flags += "housed_friend" }.any { it.contains("took me in") })
        assertTrue(lines { }.any { it.contains("always been there") })
    }

    @Test fun everyStoryScenarioCanBeStagedEvenWithoutAScript() {
        val st = TestSupport.freshState()
        for (sc in story.all) {
            val script = cine.scriptFor(sc, st)
            val d = CineDirector(script, st)
            d.skip()
            assertTrue("${sc.id} produced no scene", d.phase == Phase.CHOOSING || d.phase == Phase.DONE)
            assertTrue("${sc.id}: auto scene has no narration", script.opening.isNotEmpty())
        }
    }

    @Test fun fullCinematicLivesAlwaysPlayAndMostScenesAreCinematic() {
        var scripted = 0; var generated = 0
        for (seed in 1L..120L) {
            val engine = GameEngine.newLife(story, "Tester$seed", if (seed % 2 == 0L) Gender.BOY else Gender.GIRL, 0, seed)
            val rng = Random(seed)
            var steps = 0
            while (!engine.isFinished && steps++ < 300) {
                val p = engine.present()!!
                val sc = story[p.id]!!
                val script = cine.scriptFor(sc, engine.state)
                if (script.generated) generated++ else scripted++
                val d = CineDirector(script, engine.state)
                d.skip()
                val enabled = p.choices.filter { it.enabled }
                val pick = enabled[rng.nextInt(enabled.size)]
                val out = engine.choose(pick.index)
                d.choose(pick.index, out)
                d.skip()
                assertEquals(Phase.DONE, d.phase)
                engine.acknowledgeOutcome()
            }
            assertTrue(engine.isFinished)
        }
        val share = scripted * 100 / (scripted + generated)
        println("Scenes played: $scripted hand-written cinematics, $generated auto-staged ($share% scripted)")
        assertTrue("too few scripted scenes: $share%", share >= 20)
    }

    @Test fun chapterCardsAppearOnceAndFollowTheAge() {
        val st = TestSupport.freshState()
        st.ageMonths = 15 * 12
        val c1 = ChapterCards.cardFor(st)
        assertNotNull(c1); assertTrue(c1!!.text.contains("CHAPTER 1"))
        ChapterCards.markShown(st)
        assertEquals(null, ChapterCards.cardFor(st))
        st.ageMonths = 18 * 12
        assertTrue(ChapterCards.cardFor(st)!!.sub!!.contains("THE ROAD AHEAD"))
        assertEquals(7, ChapterCards.indexFor(60))
        assertEquals("THE LIFE YOU BUILT", ChapterCards.title(7))
        st.chapterShown = 2; st.lastSceneAge = 18; st.ageMonths = 23 * 12
        assertTrue(ChapterCards.timeSkipCard(st)!!.text.contains("5 YEARS LATER"))
    }

    @Test fun cameraTargetsStayInsideTheStage() {
        for (shot in Shot.values()) for (ax in listOf(0.05f, 0.5f, 0.95f)) {
            val t = CameraMath.target(shot, ax, 1f - ax)
            assertTrue(CameraMath.isFinite(t))
            val half = 0.5f / t.zoom
            assertTrue("$shot cx=${t.cx}", t.cx - half >= -0.001f && t.cx + half <= 1.001f)
            assertTrue("$shot cy=${t.cy}", t.cy - half >= -0.001f && t.cy + half <= 1.001f)
        }
    }

    // ---------------------------------------------------------------- ending montage

    private fun ids(s: com.lifeyourchoice.core.model.GameState) = MontageBuilder.build(s).memories.map { it.id }

    @Test fun montageOnlyShowsWhatActuallyHappened() {
        val bare = TestSupport.freshState()
        bare.ageMonths = 78 * 12
        val a = ids(bare)
        assertTrue("school" in a)
        assertFalse("wedding" in a); assertFalse("children" in a); assertFalse("business_success" in a)
        assertFalse("business_failed" in a); assertFalse("comeback" in a); assertFalse("first_job" in a)

        val married = TestSupport.freshState(); married.relationship = RelationshipStatus.MARRIED
        assertTrue("wedding" in ids(married))
        val dating = TestSupport.freshState(); dating.relationship = RelationshipStatus.DATING
        assertFalse("wedding" in ids(dating)); assertTrue("love" in ids(dating))

        val parent = TestSupport.freshState(); parent.children = 2
        assertTrue("children" in ids(parent))

        val failedForGood = TestSupport.freshState()
        failedForGood.flags += listOf("started_business", "business_failed"); failedForGood.businessStage = 0
        val f = ids(failedForGood)
        assertTrue("business_failed" in f); assertFalse("business_success" in f)

        val comeback = TestSupport.freshState()
        comeback.flags += listOf("started_business", "business_failed", "business_success", "comeback"); comeback.businessStage = 3
        val c = ids(comeback)
        assertTrue("business_failed" in c && "business_success" in c && "comeback" in c)

        val noRegret = TestSupport.freshState()
        assertFalse(ids(noRegret).any { it.startsWith("regret") })
        val regret = TestSupport.freshState(); regret.flags += "betrayed_friend"
        assertTrue("regret_friend" in ids(regret))
    }

    @Test fun montageIsShortOrderedAndHasAFinale() {
        for (seed in 1L..200L) {
            val st = TestSupport.playLife(seed, TestSupport.Style.values()[(seed % 9).toInt()]).engine.state
            val plan = MontageBuilder.build(st)
            assertTrue(plan.memories.size in 1..9)
            assertEquals(plan.memories.map { it.playerAge }, plan.memories.map { it.playerAge }.sorted())
            assertTrue(plan.finale.actors.any { it.actor == ActorId.PLAYER && it.seated })
            // No frame may show a partner unless the player ever had one.
            if (st.relationship == RelationshipStatus.SINGLE) assertFalse(plan.memories.any { m -> m.actors.any { it.actor == ActorId.PARTNER } })
            if (st.children == 0) assertFalse(plan.memories.any { m -> m.actors.any { it.actor == ActorId.CHILD } })
        }
    }

    @Test fun finaleLocationReflectsTheLife() {
        val owner = TestSupport.freshState(); owner.businessStage = 2
        assertEquals(com.lifeyourchoice.core.model.SceneArt.SMALL_BUSINESS, MontageBuilder.finale(owner).env)
        val family = TestSupport.freshState(); family.children = 2; family.setStat(Stat.FAMILY, 80)
        assertEquals(com.lifeyourchoice.core.model.SceneArt.LIVING_ROOM, MontageBuilder.finale(family).env)
        val worker = TestSupport.freshState(); worker.lastCareer = CareerTrack.SKILLED_WORKER; worker.lastCareerLevel = 3
        assertEquals(com.lifeyourchoice.core.model.SceneArt.WORKSHOP, MontageBuilder.finale(worker).env)
        assertEquals(com.lifeyourchoice.core.model.SceneArt.PARK, MontageBuilder.finale(TestSupport.freshState()).env)
    }

    @Test fun outcomeIndexIsReportedByTheEngine() {
        val engine = GameEngine.newLife(story, "T", Gender.BOY, 0, 99)
        // sch_skip_school has no random outcomes; follow the bullied branch: choice 0 has two outcomes.
        engine.choose(engine.present()!!.choices.first { it.index == 3 }.index)
        engine.acknowledgeOutcome()
        assertEquals("sch_bullied_friend", engine.present()!!.id)
        val out = engine.choose(0)
        assertTrue(out.outcomeIndex in 0..1)
        assertTrue(out.outcomeText.isNotBlank())
        assertEquals(Education.HIGH_SCHOOL, engine.state.education)
    }
}
