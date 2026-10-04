package com.lifeyourchoice.core.cinema

import com.lifeyourchoice.core.engine.TextTemplate
import com.lifeyourchoice.core.model.Category
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.SceneArt
import com.lifeyourchoice.core.story.Scenario

/**
 * Stages any Version 1 scenario as a cinematic when it has no hand-written script yet:
 * the right environment, the player on screen, the characters the text mentions, a camera that
 * pushes in, and the original text as subtitled narration. Nothing is ever left unplayable.
 */
object AutoDirector {

    fun build(sc: Scenario, s: GameState): CineScript {
        val roles = TextTemplate.roleKeys.entries
            .filter { (key, _) -> sc.text.contains("{$key") }
            .map { it.value }
            .distinct()
        val npc = roles.firstOrNull()?.let { r -> ActorId.values().firstOrNull { it.role == r } }
        val time = timeFor(sc.art)
        val mood = moodFor(sc.category)
        val beats = BeatBuilder()
        with(beats) {
            if (npc != null) {
                place(ActorId.PLAYER, 0.32f, Facing.RIGHT)
                place(npc, 0.68f, Facing.LEFT)
            } else {
                place(ActorId.PLAYER, 0.5f, Facing.RIGHT)
            }
            act(ActorId.PLAYER, Gesture.NONE, mood, 200)
            cam(Shot.ESTABLISHING)
            val lines = CineDirector.splitNarration(sc.text)
            lines.forEachIndexed { i, line ->
                if (i == 1) cam(if (npc != null) Shot.TWO_SHOT else Shot.MEDIUM, ActorId.PLAYER, npc)
                if (i == lines.lastIndex && lines.size > 1) cam(Shot.PUSH_IN, ActorId.PLAYER)
                narrate(line)
            }
            if (lines.size == 1) cam(Shot.PUSH_IN, ActorId.PLAYER)
        }
        return CineScript(sc.id, sc.art, time, beats.beats.toList(), emptyMap(), generated = true)
    }

    fun timeFor(art: SceneArt): TimeOfDay = when (art) {
        SceneArt.NIGHT_CITY, SceneArt.APARTMENT, SceneArt.BEDROOM, SceneArt.LIVING_ROOM, SceneArt.HOSPITAL -> TimeOfDay.NIGHT
        SceneArt.COFFEE_SHOP, SceneArt.WEDDING, SceneArt.STREET, SceneArt.NEW_HOME, SceneArt.AIRPORT, SceneArt.BASKETBALL_COURT, SceneArt.RESTAURANT -> TimeOfDay.EVENING
        SceneArt.KITCHEN, SceneArt.SKYLINE -> TimeOfDay.MORNING
        else -> TimeOfDay.DAY
    }

    private fun moodFor(c: Category): Emotion = when (c) {
        Category.MAJOR -> Emotion.WORRIED
        Category.LOVE -> Emotion.HAPPY
        Category.BUSINESS, Category.CAREER -> Emotion.CONFIDENT
        Category.LATER_LIFE -> Emotion.THOUGHTFUL
        else -> Emotion.NEUTRAL
    }
}
