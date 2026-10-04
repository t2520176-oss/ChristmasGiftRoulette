package com.lifeyourchoice.core.cinema

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Turns a [Shot] into a camera target over the stage (stage coordinates: 0..1 in both axes).
 * The renderer eases towards the target; the maths lives here so it is shared and testable.
 */
object CameraMath {
    class Target(val cx: Float, val cy: Float, val zoom: Float)

    /** Where a standing actor's face / chest sit vertically on the stage. */
    const val FACE_Y = 0.40f
    const val CHEST_Y = 0.54f

    /** A seated head is about this much lower on the stage than a standing one. */
    const val SEATED_DROP = 0.13f

    fun target(shot: Shot, ax: Float?, bx: Float?, aSeated: Boolean = false, bSeated: Boolean = false): Target {
        val a = ax ?: 0.5f
        val drop = if (aSeated) SEATED_DROP else 0f
        val pairDrop = (if (aSeated) SEATED_DROP else 0f) * 0.5f + (if (bSeated) SEATED_DROP else 0f) * 0.5f
        val raw = when (shot) {
            Shot.ESTABLISHING, Shot.WIDE -> Target(0.5f, 0.5f, 1f)
            Shot.MEDIUM -> Target(a, CHEST_Y - 0.02f + drop, 1.75f)
            Shot.CLOSE_UP -> Target(a, FACE_Y + drop, 2.9f)
            Shot.REACTION -> Target(a, FACE_Y + 0.02f + drop, 2.35f)
            Shot.PUSH_IN -> Target(a, FACE_Y + 0.03f + drop, 2.5f)
            Shot.FOLLOW -> Target(a, 0.52f + drop * 0.5f, 1.35f)
            Shot.TWO_SHOT -> {
                val b = bx ?: a
                Target((a + b) / 2f, 0.5f + pairDrop * 0.6f, (0.95f / (abs(a - b) + 0.35f)).coerceIn(1.15f, 2.0f))
            }
            Shot.OVER_SHOULDER -> {
                // Camera sits behind the listener: their shoulder frames the speaker.
                val b = bx ?: (a + 0.3f)
                Target(a + (b - a) * min(0.38f, 0.16f / max(abs(b - a), 0.01f)), FACE_Y + 0.07f + pairDrop, 2.05f)
            }
        }
        return clamp(raw)
    }

    /** Keeps the view inside the stage so no empty edges are ever shown. */
    fun clamp(t: Target): Target {
        val half = 0.5f / t.zoom
        return Target(t.cx.coerceIn(half, max(half, 1f - half)), t.cy.coerceIn(half, max(half, 1f - half)), t.zoom)
    }

    /** How long the camera takes to reach the shot (0 = hard cut). */
    fun moveMs(shot: Shot): Int = when (shot) {
        Shot.PUSH_IN -> 3600
        Shot.ESTABLISHING -> 5000
        Shot.FOLLOW -> 700
        else -> 0
    }

    fun isFinite(t: Target) = min(t.cx, min(t.cy, t.zoom)).isFinite()
}
