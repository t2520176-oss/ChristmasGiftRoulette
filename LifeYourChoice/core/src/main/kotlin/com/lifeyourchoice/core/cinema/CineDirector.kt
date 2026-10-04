package com.lifeyourchoice.core.cinema

import com.lifeyourchoice.core.engine.TextTemplate
import com.lifeyourchoice.core.model.ChoiceOutcome
import com.lifeyourchoice.core.model.GameState

/** What one actor is doing right now. The renderer reads this and animates the figure. */
class ActorStage(val id: ActorId) {
    var visible = false
    var x = 0.5f
    var facing = Facing.RIGHT
    var seated = false
    var gesture = Gesture.NONE
    var emotion = Emotion.NEUTRAL
    var lookAt: ActorId? = null
    var prop = Prop.NONE
    var moving = false
    var running = false
    var speaking = false
    internal var targetX: Float? = null
    internal var exitWhenDone = false
}

class Caption(val speaker: ActorId, val name: String, val text: String, val emotion: Emotion, val audio: String?, val serial: Int)

class TitleState(val kind: TitleKind, val text: String, val sub: String?, val serial: Int)

class CameraDirective(val shot: Shot, val a: ActorId?, val b: ActorId?, val serial: Int)

enum class Phase { PLAYING, CHOOSING, REACTING, DONE }

/**
 * Plays a [CineScript]: walks actors, performs dialogue, cuts the camera and pauses for the player's
 * decision. It is deterministic and has no Android/Compose dependency, so every scene can be run
 * headlessly in tests. The UI only calls [update] each frame and draws what it reads here.
 */
class CineDirector(
    val script: CineScript,
    private val state: GameState,
    preBeats: List<Beat> = emptyList()
) {
    val actors = LinkedHashMap<ActorId, ActorStage>()
    var phase = Phase.PLAYING
        private set
    var caption: Caption? = null
        private set
    var title: TitleState? = null
        private set
    /** True while the scene should be faded to black (the renderer eases towards it). */
    var blackout = false
        private set
    var camera = CameraDirective(Shot.ESTABLISHING, null, null, 0)
        private set
    var linesSpoken = 0
        private set
    var elapsedMs = 0L
        private set

    private val queue = ArrayDeque<Beat>()
    private var current: Beat? = null
    private var remaining = 0
    private var blockElapsed = 0
    @Volatile private var voiceHold = false
    private var serial = 0
    private val cues = mutableListOf<String>()

    init {
        queue.addAll(preBeats)
        queue.addAll(script.opening)
        pump()
    }

    fun actor(id: ActorId): ActorStage = actors.getOrPut(id) { ActorStage(id) }

    val visibleActors: List<ActorStage> get() = actors.values.filter { it.visible && it.id.hasBody }

    fun drainCues(): List<String> {
        if (cues.isEmpty()) return emptyList()
        val out = cues.toList(); cues.clear(); return out
    }

    // ------------------------------------------------------------------ time

    fun update(dtMs: Int) {
        if (dtMs <= 0) return
        elapsedMs += dtMs
        moveActors(dtMs)
        val c = current
        if (c != null) {
            blockElapsed += dtMs
            val waitingForVoice = c is Say && voiceHold && blockElapsed < MAX_VOICE_MS
            if (!waitingForVoice) remaining -= dtMs
            if (remaining <= 0) { finishCurrent(); pump() }
        } else {
            pump()
        }
    }

    /** Player tapped: end the current line / pause sooner. */
    fun tap() {
        val c = current ?: return
        if (blockElapsed < TAP_GUARD_MS) return
        if (c is Say || c is Pause || c is Act || c is Card) { remaining = 0; voiceHold = false }
    }

    /** The platform started speaking the current line; wait for [voiceFinished] (with a safety cap). */
    fun holdForVoice() { if (current is Say) voiceHold = true }

    fun voiceFinished() { voiceHold = false }

    /** Fast-forward to the choice (or to the end of the reaction). Used for SKIP. */
    fun skip() {
        var guard = 0
        while ((phase == Phase.PLAYING || phase == Phase.REACTING) && guard++ < 5000) {
            finishCurrent(instant = true)
            pump(instant = true)
        }
    }

    // --------------------------------------------------------------- choices

    /** The player picked Version 1 choice [index]; [outcome] is what the engine resolved. */
    fun choose(index: Int, outcome: ChoiceOutcome?) {
        if (phase != Phase.CHOOSING) return
        val ch = script.choices[index]
        val beats = mutableListOf<Beat>()
        ch?.say?.let { beats += it }
        val specific = outcome?.outcomeIndex?.let { ch?.byOutcome?.get(it) }
        val hasScripted = ch != null && (ch.react.isNotEmpty() || specific != null)
        if (specific != null) beats += ch!!.react + specific
        else if (hasScripted) {
            beats += ch!!.react
            // A random outcome happened that the script does not stage: narrate what happened.
            if (outcome != null && outcome.outcomeIndex >= 0 && outcome.outcomeText.isNotBlank()) beats += narration(outcome.outcomeText)
        } else if (outcome != null) {
            beats += Act(ActorId.PLAYER, Gesture.NONE, moodFor(outcome), 900)
            beats += narration(outcome.result)
        }
        beats += script.aftermath
        queue.clear()
        queue.addAll(beats)
        phase = Phase.REACTING
        pump()
    }

    // --------------------------------------------------------------- engine

    private fun pump(instant: Boolean = false) {
        var guard = 0
        while (current == null && (phase == Phase.PLAYING || phase == Phase.REACTING) && guard++ < 10_000) {
            val next = queue.removeFirstOrNull()
            if (next == null) {
                phase = if (phase == Phase.PLAYING && (script.choices.isNotEmpty() || script.generated)) Phase.CHOOSING else Phase.DONE
                return
            }
            if (begin(next, instant)) { current = next; blockElapsed = 0 }
        }
    }

    /** Executes [b]. Returns true when it blocks (waits for time), false when it completes instantly. */
    private fun begin(b: Beat, instant: Boolean): Boolean {
        b.requires?.let { if (!it.test(state)) return false }
        when (b) {
            is Place -> actor(b.actor).apply { visible = true; x = b.x; facing = b.facing; seated = b.seated; moving = false; targetX = null }
            is Enter -> {
                val a = actor(b.actor)
                a.visible = true; a.seated = false
                a.x = if (b.from == Edge.LEFT) -0.14f else 1.14f
                return startMove(a, b.toX, b.run, false, b.concurrent, instant)
            }
            is Exit -> {
                val a = actor(b.actor)
                a.seated = false
                return startMove(a, if (b.to == Edge.LEFT) -0.16f else 1.16f, b.run, true, b.concurrent, instant)
            }
            is Move -> return startMove(actor(b.actor), b.toX, b.run, false, b.concurrent, instant)
            is Turn -> actor(b.actor).apply {
                facing = b.facing ?: b.toward?.let { t -> if (actor(t).x >= x) Facing.RIGHT else Facing.LEFT } ?: facing
            }
            is Seat -> { actor(b.actor).seated = b.sit; remaining = if (instant) 0 else 450; return !instant }
            is Look -> actor(b.actor).lookAt = b.toward
            is Mood -> actor(b.actor).apply { emotion = b.emotion; gesture = b.gesture }
            is Act -> {
                val a = actor(b.actor)
                a.gesture = b.gesture
                b.emotion?.let { a.emotion = it }
                if (instant) a.gesture = Gesture.NONE
                remaining = if (instant) 0 else b.ms
                return !instant
            }
            is Say -> return startSay(b, instant)
            is Cam -> setCamera(b.shot, b.a, b.b)
            is Pause -> { remaining = if (instant) 0 else b.ms; if (!instant) clearCaptionIfStale(); return !instant }
            is Card -> {
                title = TitleState(b.kind, TextTemplate.render(b.text, state), b.sub?.let { TextTemplate.render(it, state) }, ++serial)
                caption = null
                remaining = if (instant) 0 else b.ms
                return !instant
            }
            is Fade -> { blackout = b.toBlack; remaining = if (instant) 0 else b.ms; return !instant }
            is Hold -> actor(b.actor).prop = b.prop
            is Cue -> cues += b.key
            is Branch -> {
                val chosen = if (b.cond.test(state)) b.then else b.otherwise
                for (i in chosen.indices.reversed()) queue.addFirst(chosen[i])
            }
        }
        return false
    }

    private fun startMove(a: ActorStage, toX: Float, run: Boolean, exit: Boolean, concurrent: Boolean, instant: Boolean): Boolean {
        a.targetX = toX
        a.exitWhenDone = exit
        a.running = run
        caption = null
        if (instant) { a.x = toX; a.moving = false; a.targetX = null; if (exit) a.visible = false; return false }
        a.facing = if (toX >= a.x) Facing.RIGHT else Facing.LEFT
        a.moving = true
        if (concurrent) return false
        val speed = if (run) RUN_SPEED else WALK_SPEED
        remaining = (kotlin.math.abs(toX - a.x) / speed * 1000f).toInt().coerceAtLeast(150)
        return true
    }

    private fun startSay(b: Say, instant: Boolean): Boolean {
        val text = TextTemplate.render(b.text, state)
        val name = b.speaker.displayName(state)
        caption = Caption(b.speaker, name, text, b.emotion, b.audio, ++serial)
        title = null
        linesSpoken++
        if (b.speaker.hasBody && !b.offscreen) {
            val sp = actor(b.speaker)
            if (!sp.visible) { sp.visible = true }
            sp.emotion = b.emotion
            sp.gesture = b.gesture
            sp.speaking = true
            // Everyone else on stage turns their attention to the speaker.
            for (o in actors.values) if (o.visible && o.id != b.speaker && o.id.hasBody) o.lookAt = b.speaker
        }
        if (b.shot != null) {
            val other = actors.values.firstOrNull { it.visible && it.id != b.speaker && it.id.hasBody }?.id
            when (b.shot) {
                Shot.OVER_SHOULDER, Shot.TWO_SHOT -> setCamera(b.shot, b.speaker.takeIf { it.hasBody }, other)
                Shot.REACTION -> setCamera(b.shot, other ?: b.speaker, null)
                else -> setCamera(b.shot, b.speaker.takeIf { it.hasBody }, other)
            }
        }
        if (instant && b.speaker.hasBody && !b.offscreen) actor(b.speaker).apply { speaking = false; gesture = Gesture.NONE }
        remaining = if (instant) 0 else (b.ms ?: speechMs(text))
        return !instant
    }

    private fun setCamera(shot: Shot, a: ActorId?, b: ActorId?) {
        val bb = b ?: actors.values.firstOrNull { it.visible && it.id != a && it.id.hasBody }?.id
        camera = CameraDirective(shot, a ?: actors.values.firstOrNull { it.visible && it.id.hasBody }?.id, bb, ++serial)
    }

    private fun clearCaptionIfStale() { /* captions linger through short pauses */ }

    private fun finishCurrent(instant: Boolean = false) {
        when (val c = current) {
            is Say -> { if (c.speaker.hasBody && !c.offscreen) actor(c.speaker).apply { speaking = false; gesture = Gesture.NONE } }
            is Act -> actor(c.actor).gesture = Gesture.NONE
            is Enter -> snap(actor(c.actor), c.toX, false)
            is Move -> snap(actor(c.actor), c.toX, false)
            is Exit -> snap(actor(c.actor), if (c.to == Edge.LEFT) -0.16f else 1.16f, true)
            is Card -> title = null
            else -> {}
        }
        current = null
        voiceHold = false
        if (instant) for (a in actors.values) if (a.targetX != null) snap(a, a.targetX!!, a.exitWhenDone)
    }

    private fun snap(a: ActorStage, x: Float, exit: Boolean) {
        a.x = x; a.targetX = null; a.moving = false
        if (exit) a.visible = false
    }

    private fun moveActors(dtMs: Int) {
        for (a in actors.values) {
            val t = a.targetX ?: continue
            val step = (if (a.running) RUN_SPEED else WALK_SPEED) * dtMs / 1000f
            val d = t - a.x
            if (kotlin.math.abs(d) <= step) {
                a.x = t; a.targetX = null; a.moving = false
                if (a.exitWhenDone) a.visible = false
            } else {
                a.x += if (d > 0) step else -step
                a.facing = if (d > 0) Facing.RIGHT else Facing.LEFT
            }
        }
    }

    // --------------------------------------------------------------- helpers

    private fun narration(text: String): List<Beat> = splitNarration(text).map { Say(ActorId.NARRATOR, it) }

    private fun moodFor(o: ChoiceOutcome): Emotion {
        val net = o.deltas.sumOf { it.delta }
        return when {
            net >= 4 -> Emotion.HAPPY
            net <= -4 -> Emotion.WORRIED
            else -> Emotion.THOUGHTFUL
        }
    }

    companion object {
        const val WALK_SPEED = 0.16f
        const val RUN_SPEED = 0.40f
        const val TAP_GUARD_MS = 300
        const val MAX_VOICE_MS = 12_000

        /** Reading time for a subtitle: generous so slow readers are not rushed (tap to speed up). */
        fun speechMs(text: String): Int = (1000 + 52 * text.length).coerceIn(1500, 7000)

        /** Splits long narration into short subtitle-sized lines. */
        fun splitNarration(text: String, maxLen: Int = 150): List<String> {
            val out = mutableListOf<String>()
            for (para in text.split("\n\n")) {
                val sentences = Regex("(?<=[.!?…”])\\s+").split(para.trim()).filter { it.isNotBlank() }
                var cur = StringBuilder()
                for (s in sentences) {
                    if (cur.isNotEmpty() && cur.length + s.length + 1 > maxLen) { out += cur.toString(); cur = StringBuilder() }
                    if (cur.isNotEmpty()) cur.append(' ')
                    cur.append(s)
                }
                if (cur.isNotEmpty()) out += cur.toString()
            }
            return out
        }
    }
}
