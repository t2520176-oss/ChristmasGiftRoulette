package com.lifeyourchoice.app.ui.cinema

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.lifeyourchoice.app.ui.art.Face
import com.lifeyourchoice.app.ui.art.FigurePose
import com.lifeyourchoice.app.ui.art.Look
import com.lifeyourchoice.app.ui.art.Looks
import com.lifeyourchoice.app.ui.art.PoseSolver
import com.lifeyourchoice.core.cinema.ActorId
import com.lifeyourchoice.core.cinema.ActorStage
import com.lifeyourchoice.core.cinema.CameraDirective
import com.lifeyourchoice.core.cinema.CameraMath
import com.lifeyourchoice.core.cinema.CineDirector
import com.lifeyourchoice.core.cinema.Emotion
import com.lifeyourchoice.core.cinema.Facing
import com.lifeyourchoice.core.cinema.Gesture
import com.lifeyourchoice.core.cinema.Prop
import com.lifeyourchoice.core.cinema.Shot
import com.lifeyourchoice.core.cinema.TimeOfDay
import com.lifeyourchoice.core.model.GameState
import com.lifeyourchoice.core.model.Gender
import com.lifeyourchoice.core.model.SceneArt
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** Who an actor is on screen: resolved once per scene. */
class CastInfo(val look: Look, val gender: Gender, val age: Int, val scale: Float)

/** Builds the look/age of every possible actor from the life state (names, genders and looks stay stable). */
fun castFor(state: GameState, playerAge: Int = state.ageYears): Map<ActorId, CastInfo> {
    val out = HashMap<ActorId, CastInfo>()
    for (a in ActorId.values()) {
        if (!a.hasBody) continue
        val g = a.gender(state)
        val age = if (a == ActorId.PLAYER) playerAge else (playerAge + a.ageOffset).coerceIn(if (a.adult) 14 else 4, 95)
        val look = if (a == ActorId.PLAYER) Looks.forPlayer(state.gender, state.playerLook) else Looks.forActor(a, g, state.seed)
        out[a] = CastInfo(look, g, age, if (a.adult) 1f else 0.6f)
    }
    return out
}

/** The smoothed, per-frame visual state of one figure. */
class FigureVisual(val id: ActorId) {
    val pose = FigurePose()
    val target = FigurePose()
    var facing = 1f
    var walk = 0f
    var x = 0.5f
    var visible = false
    var emotion = Emotion.NEUTRAL
    var gesture = Gesture.NONE
    var prop = Prop.NONE
    var seated = false
    var moving = false
    var running = false
    var speaking = false
    var lookX = 0f
    var yaw = 0f
    var blink = 0f
    var blinkTimer = 2f
    var mouth = 0f
    var appeared = 0f
}

/**
 * Turns a [CineDirector]'s discrete state into smooth motion: blended poses, turning, walking,
 * blinking, talking and the camera. The Compose layer only draws what is stored here.
 */
class StageRuntime(var cast: Map<ActorId, CastInfo>) {
    var env: SceneArt = SceneArt.CLASSROOM
    var time: TimeOfDay = TimeOfDay.DAY
    val figures = LinkedHashMap<ActorId, FigureVisual>()

    /** Bumped every update so the draw scope re-reads the figures. */
    var frame by mutableIntStateOf(0)
        private set
    var camX by mutableFloatStateOf(0.5f)
        private set
    var camY by mutableFloatStateOf(0.5f)
        private set
    var camZoom by mutableFloatStateOf(1f)
        private set
    /** 0..1 darkness for fades. */
    var blackout by mutableFloatStateOf(0f)
        private set
    /** Brief dip on hard cuts so editing reads as a cut. */
    var cutFlash by mutableFloatStateOf(0f)
        private set

    private var clock = 0f
    private var lastCamSerial = -1
    private var camFromX = 0.5f; private var camFromY = 0.5f; private var camFromZ = 1f
    private var camElapsed = 0f
    private var camDuration = 0f
    private var camShot = Shot.WIDE
    private var camA: ActorId? = null
    private var camB: ActorId? = null
    /** Multiplies the push-in duration (the ending uses very slow pushes). */
    var pushScale = 1f

    fun reset(env: SceneArt, time: TimeOfDay, cast: Map<ActorId, CastInfo>) {
        this.env = env; this.time = time; this.cast = cast
        figures.clear()
        lastCamSerial = -1
        camX = 0.5f; camY = 0.5f; camZoom = 1f
        blackout = 1f; cutFlash = 0f
    }

    fun figure(id: ActorId): FigureVisual = figures.getOrPut(id) { FigureVisual(id) }

    fun update(dtMs: Int, d: CineDirector) {
        val dt = (dtMs / 1000f).coerceIn(0.001f, 0.1f)
        clock += dt
        syncFigures(dt, d)
        updateCamera(dt, d)
        val bt = if (d.blackout) 1f else 0f
        blackout += (bt - blackout) * min(1f, dt / 0.35f)
        cutFlash = (cutFlash - dt / 0.14f).coerceAtLeast(0f)
        frame++
    }

    private fun syncFigures(dt: Float, d: CineDirector) {
        for ((id, st) in d.actors) {
            if (!id.hasBody) continue
            val f = figure(id)
            apply(f, st, d)
            val speed = 1f - exp(-11f * dt)
            val wantFacing = if (st.facing == Facing.RIGHT) 1f else -1f
            f.facing += (wantFacing - f.facing) * min(1f, 9f * dt)
            f.appeared += ((if (st.visible) 1f else 0f) - f.appeared) * min(1f, 8f * dt)
            if (f.moving) f.walk += dt * (if (f.running) 15f else 9.5f)
            PoseSolver.solve(f.gesture, f.emotion, f.speaking, f.moving, f.running, f.seated, clock + id.ordinal * 0.7f, f.walk, f.target, f.prop)
            f.pose.approach(f.target, speed)
            // blinking and talking
            f.blinkTimer -= dt
            f.blink = when {
                f.blinkTimer < 0f && f.blinkTimer > -0.13f -> 1f
                f.blinkTimer <= -0.13f -> { f.blinkTimer = 2.5f + (id.ordinal % 4) * 0.8f; 0f }
                else -> 0f
            }
            f.mouth = if (f.speaking) 0.25f + 0.55f * abs(sin(clock * 11f + id.ordinal)) else f.mouth * 0.7f
            // gaze toward whoever they are looking at
            val target = st.lookAt?.let { d.actors[it] }
            val localSign = if (f.facing >= 0f) 1f else -1f
            val wantLook = if (target != null && target.visible) {
                val dx = target.x - st.x
                (if (dx >= 0f) 1f else -1f) * 0.9f * localSign
            } else 0f
            f.lookX += (wantLook - f.lookX) * min(1f, 8f * dt)
            f.yaw = f.lookX * 0.55f
        }
    }

    private fun apply(f: FigureVisual, st: ActorStage, d: CineDirector) {
        f.x = st.x; f.visible = st.visible; f.emotion = st.emotion; f.gesture = st.gesture
        f.prop = st.prop; f.seated = st.seated; f.moving = st.moving; f.running = st.running; f.speaking = st.speaking
    }

    fun faceOf(f: FigureVisual) = Face(f.emotion, f.mouth, f.blink, f.lookX, 0f, f.yaw)

    // ------------------------------------------------------------------ camera

    private fun updateCamera(dt: Float, d: CineDirector) {
        val cam: CameraDirective = d.camera
        val ax = cam.a?.let { d.actors[it]?.x }
        val bx = cam.b?.let { d.actors[it]?.x }
        if (cam.serial != lastCamSerial) {
            lastCamSerial = cam.serial
            camShot = cam.shot; camA = cam.a; camB = cam.b
            val moveMs = CameraMath.moveMs(cam.shot)
            val t = CameraMath.target(cam.shot, ax, bx)
            when {
                cam.shot == Shot.ESTABLISHING -> {
                    camX = 0.5f; camY = 0.5f; camZoom = 1.12f
                    camFromX = camX; camFromY = camY; camFromZ = camZoom
                    camElapsed = 0f; camDuration = moveMs / 1000f * pushScale
                    cutFlash = 0f
                }
                moveMs > 0 && cam.shot == Shot.PUSH_IN -> {
                    camFromX = camX; camFromY = camY; camFromZ = camZoom
                    camElapsed = 0f; camDuration = moveMs / 1000f * pushScale
                }
                cam.shot == Shot.FOLLOW -> { camDuration = 0f }
                else -> {
                    camX = t.cx; camY = t.cy; camZoom = t.zoom
                    camFromX = camX; camFromY = camY; camFromZ = camZoom
                    camDuration = 0f
                    cutFlash = 0.22f
                }
            }
        }
        val t = CameraMath.target(camShot, camA?.let { d.actors[it]?.x }, camB?.let { d.actors[it]?.x })
        if (camDuration > 0f) {
            camElapsed += dt
            val p = (camElapsed / camDuration).coerceIn(0f, 1f)
            val e = p * p * (3f - 2f * p)
            camX = camFromX + (t.cx - camFromX) * e
            camY = camFromY + (t.cy - camFromY) * e
            camZoom = camFromZ + (t.zoom - camFromZ) * e
        } else {
            // Gently keep the subject framed if they walk.
            val k = min(1f, (if (camShot == Shot.FOLLOW) 5f else 3.5f) * dt)
            camX += (t.cx - camX) * k; camY += (t.cy - camY) * k; camZoom += (t.zoom - camZoom) * k
        }
    }
}
