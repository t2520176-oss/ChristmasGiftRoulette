package com.lifeyourchoice.core.cinema

import com.lifeyourchoice.core.model.SceneArt
import com.lifeyourchoice.core.story.Condition

/**
 * One step of a cinematic. A scene is just data: a list of beats played in order. The same
 * handful of beat types (and the same figure rig / camera / emotions) power every scene.
 * Any beat can carry a [requires] condition (e.g. "only if Family > 70") and is skipped otherwise.
 */
sealed class Beat {
    var requires: Condition? = null
}

/** Puts an actor on stage instantly. [x] is 0 (left edge) .. 1 (right edge). */
class Place(val actor: ActorId, val x: Float, val facing: Facing, val seated: Boolean = false) : Beat()

/** Walks (or runs) an actor in from off-screen. Blocks until arrival unless [concurrent]. */
class Enter(val actor: ActorId, val from: Edge, val toX: Float, val run: Boolean = false, val concurrent: Boolean = false) : Beat()

class Exit(val actor: ActorId, val to: Edge, val run: Boolean = false, val concurrent: Boolean = false) : Beat()

class Move(val actor: ActorId, val toX: Float, val run: Boolean = false, val concurrent: Boolean = false) : Beat()

/** Turns an actor to a facing, or toward another actor. */
class Turn(val actor: ActorId, val facing: Facing? = null, val toward: ActorId? = null) : Beat()

class Seat(val actor: ActorId, val sit: Boolean) : Beat()

/** Sets an actor's emotion and gesture instantly and leaves them (used to pose memory frames). */
class Mood(val actor: ActorId, val emotion: Emotion, val gesture: Gesture = Gesture.NONE) : Beat()

/** Gaze direction: toward another actor, or null to look away / straight ahead. */
class Look(val actor: ActorId, val toward: ActorId?) : Beat()

/** A silent performance: the actor reacts with an emotion and/or gesture for [ms]. */
class Act(val actor: ActorId, val gesture: Gesture, val emotion: Emotion?, val ms: Int) : Beat()

/** A line of dialogue with its performance metadata. [audio] is an optional bundled voice clip key. */
class Say(
    val speaker: ActorId,
    val text: String,
    val emotion: Emotion = Emotion.NEUTRAL,
    val gesture: Gesture = Gesture.NONE,
    val shot: Shot? = null,
    val audio: String? = null,
    val ms: Int? = null,
    /** True for a voice heard but not seen (phone call, someone behind a door). */
    val offscreen: Boolean = false
) : Beat()

/** A camera instruction. [a] is the subject; [b] the other participant for two-character shots. */
class Cam(val shot: Shot, val a: ActorId? = null, val b: ActorId? = null) : Beat()

class Pause(val ms: Int) : Beat()

class Card(val kind: TitleKind, val text: String, val sub: String? = null, val ms: Int = 2200) : Beat()

class Fade(val toBlack: Boolean, val ms: Int = 600) : Beat()

class Hold(val actor: ActorId, val prop: Prop) : Beat()

/** A sound cue ("door", "phone", "cheer", "whoosh") played by the platform audio layer. */
class Cue(val key: String) : Beat()

class Branch(val cond: Condition, val then: List<Beat>, val otherwise: List<Beat> = emptyList()) : Beat()

/** What happens when the player picks one of the scenario's choices. */
class CineChoice(
    /** The player's spoken line (also the button label). Null = use the Version 1 choice text. */
    val say: Say?,
    val react: List<Beat>,
    /** Extra reactions for specific random outcomes of the Version 1 choice (by outcome index). */
    val byOutcome: Map<Int, List<Beat>>
)

/**
 * A complete cinematic for one story scenario. [choices] are keyed by the Version 1 choice index,
 * so the Version 1 engine still resolves stats, flags, traits, relationships and delayed consequences.
 */
class CineScript(
    val scenarioId: String,
    val env: SceneArt,
    val time: TimeOfDay,
    val opening: List<Beat>,
    val choices: Map<Int, CineChoice>,
    val aftermath: List<Beat> = emptyList(),
    /** True for scripts generated from Version 1 text (the fallback director). */
    val generated: Boolean = false
)
