package com.lifeyourchoice.core.cinema

import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.story.Scenario
import com.lifeyourchoice.core.story.StoryLibrary

/**
 * All hand-written cinematics, indexed by the story scenario they stage. New episodes are added
 * by registering another [CinePack]; the engine does not change.
 */
class CineLibrary(val packs: List<CinePack>) {
    val all: List<CineScript> = packs.flatMap { it.scripts }
    val byScenario: Map<String, CineScript> = all.associateBy { it.scenarioId }

    /** The scripted cinematic for [sc], or an automatically staged one. */
    fun scriptFor(sc: Scenario, s: GameState): CineScript = byScenario[sc.id] ?: AutoDirector.build(sc, s)

    fun hasScript(scenarioId: String) = scenarioId in byScenario

    /** Content problems (empty list = consistent). Checks actors are on stage before they act, choice indexes, etc. */
    fun validate(story: StoryLibrary): List<String> {
        val problems = mutableListOf<String>()
        val dupes = all.groupBy { it.scenarioId }.filter { it.value.size > 1 }.keys
        dupes.forEach { problems += "Duplicate cinematic for $it" }
        for (cs in all) {
            val sc = story[cs.scenarioId]
            if (sc == null) { problems += "${cs.scenarioId}: no such story scenario"; continue }
            for ((i, c) in cs.choices) {
                if (i !in sc.choices.indices) problems += "${cs.scenarioId}: choice index $i does not exist"
                else {
                    val outcomes = sc.choices[i].outcomes.size
                    for (o in c.byOutcome.keys) if (o !in 0 until outcomes) problems += "${cs.scenarioId}: choice $i has no outcome $o"
                }
            }
            val onStage = mutableSetOf<ActorId>()
            checkBeats(cs.scenarioId, cs.opening, onStage, problems)
            if (cs.opening.none { it is Say }) problems += "${cs.scenarioId}: opening has no dialogue"
            for ((i, c) in cs.choices) {
                val after = onStage.toMutableSet()
                c.say?.let { checkBeats("${cs.scenarioId}#$i", listOf(it), after, problems) }
                checkBeats("${cs.scenarioId}#$i", c.react, after, problems)
                for ((o, bs) in c.byOutcome) checkBeats("${cs.scenarioId}#$i/o$o", bs, after.toMutableSet(), problems)
            }
        }
        return problems
    }

    private fun checkBeats(where: String, beats: List<Beat>, onStage: MutableSet<ActorId>, problems: MutableList<String>) {
        fun need(a: ActorId, what: String) { if (a.hasBody && a !in onStage) problems += "$where: $a $what before being placed" }
        for (b in beats) when (b) {
            is Place -> { onStage += b.actor; if (b.x !in 0f..1f) problems += "$where: place x out of range" }
            is Enter -> onStage += b.actor
            is Exit -> { need(b.actor, "exits"); onStage -= b.actor }
            is Move -> need(b.actor, "moves")
            is Turn -> need(b.actor, "turns")
            is Seat -> need(b.actor, "sits/stands")
            is Look -> need(b.actor, "looks")
            is Mood -> need(b.actor, "changes mood")
            is Act -> need(b.actor, "acts")
            is Say -> {
                if (!b.offscreen) need(b.speaker, "speaks")
                if (b.text.isBlank()) problems += "$where: empty line"
                if (b.text.length > 130) problems += "$where: line too long (${b.text.length}): ${b.text.take(40)}…"
            }
            is Hold -> need(b.actor, "holds")
            is Branch -> { checkBeats(where, b.then, onStage.toMutableSet(), problems); checkBeats(where, b.otherwise, onStage.toMutableSet(), problems) }
            else -> {}
        }
    }
}
