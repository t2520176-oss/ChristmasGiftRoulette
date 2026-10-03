package com.example.christmasgiftroulette.game

/**
 * Geometry shared by the animation and the tests.
 *
 * Conventions: angles are in degrees, clockwise, measured from the TOP of the wheel (where the
 * fixed pointer is). Segment `i` of `n` covers `[i*360/n, (i+1)*360/n)` before rotation. Rotating
 * the wheel clockwise by `r` moves a point at wheel angle `a` to screen angle `a + r`.
 */
object WheelMath {

    fun normalize(degrees: Float): Float {
        val m = degrees % 360f
        return if (m < 0f) m + 360f else m
    }

    /**
     * The absolute rotation at which the segment [winnerIndex] sits under the pointer, at least
     * [extraTurns] full turns beyond [currentRotation] (the wheel only ever spins forward).
     *
     * [offsetFraction] in (-0.5, 0.5) shifts the stopping point inside the segment (0 = exact
     * centre) so results look natural while staying strictly inside the winning segment.
     */
    fun targetRotation(
        currentRotation: Float,
        winnerIndex: Int,
        count: Int,
        extraTurns: Int,
        offsetFraction: Float,
    ): Float {
        require(count > 0 && winnerIndex in 0 until count)
        val segment = 360f / count
        val pointAngleOnWheel = (winnerIndex + 0.5f + offsetFraction) * segment
        val desired = normalize(-pointAngleOnWheel)
        val delta = normalize(desired - currentRotation)
        return currentRotation + extraTurns * 360f + delta
    }

    /** Index of the segment currently under the top pointer for a given [rotation]. */
    fun indexAtPointer(rotation: Float, count: Int): Int {
        if (count <= 1) return 0
        val angleOnWheel = normalize(-rotation)
        return (angleOnWheel / (360f / count)).toInt().coerceIn(0, count - 1)
    }
}
