package com.example.christmasgiftroulette

import com.example.christmasgiftroulette.game.WheelMath
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelMathTest {

    @Test
    fun targetRotationLandsExactlyOnTheChosenSegment() {
        val random = Random(99)
        for (count in listOf(1, 2, 3, 7, 12, 13, 50)) {
            for (index in 0 until count) {
                repeat(5) {
                    val current = random.nextFloat() * 360f
                    val turns = random.nextInt(5, 8)
                    val offset = random.nextFloat() * 0.7f - 0.35f
                    val target = WheelMath.targetRotation(current, index, count, turns, offset)
                    assertEquals("count=$count index=$index", index, WheelMath.indexAtPointer(target, count))
                    assertTrue("must spin forward at least $turns turns", target - current >= turns * 360f)
                    assertTrue("must stay below turns+1", target - current < (turns + 1) * 360f)
                }
            }
        }
    }

    @Test
    fun centreOffsetPutsSegmentCentreUnderPointer() {
        // 4 segments, winner index 1 -> its centre is at 135deg on the wheel, so rotate to -135 = 225.
        val target = WheelMath.targetRotation(0f, 1, 4, 0, 0f)
        assertEquals(225f, target, 0.001f)
    }

    @Test
    fun indexAtPointerWalksBackwardsAsWheelTurnsClockwise() {
        // 4 segments of 90deg. At rotation 0, segment 0 is just clockwise of the pointer.
        assertEquals(0, WheelMath.indexAtPointer(0f, 4))
        assertEquals(3, WheelMath.indexAtPointer(10f, 4))
        assertEquals(2, WheelMath.indexAtPointer(100f, 4))
        assertEquals(1, WheelMath.indexAtPointer(190f, 4))
        assertEquals(0, WheelMath.indexAtPointer(280f, 4))
    }

    @Test
    fun normalizeHandlesNegativeAndLargeAngles() {
        assertEquals(350f, WheelMath.normalize(-10f), 0.001f)
        assertEquals(10f, WheelMath.normalize(730f), 0.001f)
        assertEquals(0f, WheelMath.normalize(720f), 0.001f)
    }
}
