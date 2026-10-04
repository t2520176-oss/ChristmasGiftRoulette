package com.lifeyourchoice.core.cinema

import com.lifeyourchoice.core.model.SceneArt
import com.lifeyourchoice.core.story.Condition

/**
 * Authoring DSL for cinematic scenes:
 *
 * ```
 * scene("sch_skip_school", HALLWAY) {
 *     place(PLAYER, 0.3f, RIGHT); place(FRIEND, 0.7f, LEFT)
 *     say(FRIEND, "Come on. Let’s skip class.", CONFIDENT, EXPLAIN, shot = MEDIUM)
 *     option(1, "No. I’m staying.", CONFIDENT) { say(FRIEND, "Your loss.", DISAPPOINTED) }
 * }
 * ```
 */
@DslMarker
annotation class CineDsl

@CineDsl
open class BeatBuilder {
    internal val beats = mutableListOf<Beat>()

    private fun <T : Beat> add(b: T, requires: Condition?): T {
        b.requires = requires
        beats += b
        return b
    }

    fun place(actor: ActorId, x: Float, facing: Facing, seated: Boolean = false, requires: Condition? = null) =
        add(Place(actor, x, facing, seated), requires)

    fun enter(actor: ActorId, from: Edge, toX: Float, run: Boolean = false, concurrent: Boolean = false, requires: Condition? = null) =
        add(Enter(actor, from, toX, run, concurrent), requires)

    fun exit(actor: ActorId, to: Edge, run: Boolean = false, concurrent: Boolean = false, requires: Condition? = null) =
        add(Exit(actor, to, run, concurrent), requires)

    fun move(actor: ActorId, toX: Float, run: Boolean = false, concurrent: Boolean = false, requires: Condition? = null) =
        add(Move(actor, toX, run, concurrent), requires)

    fun turn(actor: ActorId, facing: Facing, requires: Condition? = null) = add(Turn(actor, facing = facing), requires)

    fun turnTo(actor: ActorId, toward: ActorId, requires: Condition? = null) = add(Turn(actor, toward = toward), requires)

    fun sit(actor: ActorId, requires: Condition? = null) = add(Seat(actor, true), requires)

    fun stand(actor: ActorId, requires: Condition? = null) = add(Seat(actor, false), requires)

    fun mood(actor: ActorId, emotion: Emotion, gesture: Gesture = Gesture.NONE, requires: Condition? = null) =
        add(Mood(actor, emotion, gesture), requires)

    fun look(actor: ActorId, toward: ActorId?, requires: Condition? = null) = add(Look(actor, toward), requires)

    fun act(actor: ActorId, gesture: Gesture, emotion: Emotion? = null, ms: Int = 1200, requires: Condition? = null) =
        add(Act(actor, gesture, emotion, ms), requires)

    fun say(
        speaker: ActorId, text: String, emotion: Emotion = Emotion.NEUTRAL, gesture: Gesture = Gesture.NONE,
        shot: Shot? = null, audio: String? = null, ms: Int? = null, requires: Condition? = null
    ) = add(Say(speaker, text, emotion, gesture, shot, audio, ms), requires)

    /** A voice heard but not seen: a phone call, someone behind a door. */
    fun voice(speaker: ActorId, text: String, emotion: Emotion = Emotion.NEUTRAL, ms: Int? = null, requires: Condition? = null) =
        add(Say(speaker, text, emotion, Gesture.NONE, null, null, ms, offscreen = true), requires)

    /** Narration (no body on screen): inner thoughts or a short caption. */
    fun narrate(text: String, ms: Int? = null, requires: Condition? = null) =
        add(Say(ActorId.NARRATOR, text, ms = ms), requires)

    fun cam(shot: Shot, a: ActorId? = null, b: ActorId? = null, requires: Condition? = null) = add(Cam(shot, a, b), requires)

    fun pause(ms: Int = 600, requires: Condition? = null) = add(Pause(ms), requires)

    fun card(text: String, sub: String? = null, ms: Int = 2200, kind: TitleKind = TitleKind.CAPTION, requires: Condition? = null) =
        add(Card(kind, text, sub, ms), requires)

    fun fadeOut(ms: Int = 600) = add(Fade(true, ms), null)

    fun fadeIn(ms: Int = 600) = add(Fade(false, ms), null)

    fun hold(actor: ActorId, prop: Prop, requires: Condition? = null) = add(Hold(actor, prop), requires)

    fun cue(key: String, requires: Condition? = null) = add(Cue(key), requires)

    /** Beats that only play when [cond] holds (e.g. a line that needs a high relationship). */
    fun whenever(cond: Condition, otherwise: BeatBuilder.() -> Unit = {}, block: BeatBuilder.() -> Unit) {
        add(Branch(cond, BeatBuilder().apply(block).beats.toList(), BeatBuilder().apply(otherwise).beats.toList()), null)
    }
}

@CineDsl
class SceneBuilder(private val scenarioId: String, private val env: SceneArt, private val time: TimeOfDay) : BeatBuilder() {
    private val choices = mutableMapOf<Int, CineChoice>()
    private val after = mutableListOf<Beat>()

    /**
     * The reaction to Version 1 choice [index]. [line] is what the player says (and the button label);
     * the block holds the other characters' reactions and the consequence.
     */
    fun option(
        index: Int, line: String, emotion: Emotion = Emotion.NEUTRAL, gesture: Gesture = Gesture.NONE,
        shot: Shot? = null, block: OptionBuilder.() -> Unit = {}
    ) {
        val ob = OptionBuilder().apply(block)
        choices[index] = CineChoice(Say(ActorId.PLAYER, line, emotion, gesture, shot), ob.beats.toList(), ob.outcomes.toMap())
    }

    /** A choice with no spoken line (an action). The Version 1 choice text is the button label. */
    fun action(index: Int, block: OptionBuilder.() -> Unit) {
        val ob = OptionBuilder().apply(block)
        choices[index] = CineChoice(null, ob.beats.toList(), ob.outcomes.toMap())
    }

    fun aftermath(block: BeatBuilder.() -> Unit) { after += BeatBuilder().apply(block).beats }

    internal fun build() = CineScript(scenarioId, env, time, beats.toList(), choices.toMap(), after.toList())
}

@CineDsl
class OptionBuilder : BeatBuilder() {
    internal val outcomes = mutableMapOf<Int, List<Beat>>()

    /** Reaction used when Version 1 rolled random outcome [index] for this choice. */
    fun outcome(index: Int, block: BeatBuilder.() -> Unit) { outcomes[index] = BeatBuilder().apply(block).beats.toList() }
}

class CinePack(val id: String, val scripts: List<CineScript>)

@CineDsl
class CinePackBuilder {
    internal val scripts = mutableListOf<CineScript>()

    fun scene(scenarioId: String, env: SceneArt, time: TimeOfDay = TimeOfDay.DAY, block: SceneBuilder.() -> Unit) {
        scripts += SceneBuilder(scenarioId, env, time).apply(block).build()
    }
}

fun cinePack(id: String, block: CinePackBuilder.() -> Unit): CinePack =
    CinePack(id, CinePackBuilder().apply(block).scripts.toList())
