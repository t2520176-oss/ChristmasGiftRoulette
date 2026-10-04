package com.bakeyourway.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlourInputTest {
    private fun ok(r: FlourInput.Result) = r as FlourInput.Result.Ok
    private fun err(r: FlourInput.Result) = r as FlourInput.Result.Error

    @Test
    fun `parses decimals fractions and tidy input`() {
        assertEquals(1.5, FlourInput.parse("1.5")!!, 1e-9)
        assertEquals(1.5, FlourInput.parse("1,5")!!, 1e-9)
        assertEquals(0.5, FlourInput.parse("1/2")!!, 1e-9)
        assertEquals(1.5, FlourInput.parse("1 1/2")!!, 1e-9)
        assertEquals(2.25, FlourInput.parse(" 2.25 cups ")!!, 1e-9)
        assertEquals(1.75, FlourInput.parse("1.75")!!, 1e-9)
        assertEquals(0.5, FlourInput.parse(".5")!!, 1e-9)
        assertNull(FlourInput.parse(""))
        assertNull(FlourInput.parse("abc"))
        assertNull(FlourInput.parse("1/0"))
        assertNull(FlourInput.parse("1.2.3"))
        assertNull(FlourInput.parse("1e5"))
    }

    @Test
    fun `zero and negative amounts are rejected`() {
        assertTrue(FlourInput.validate("0", UnitSystem.US_CUPS, 120.0) is FlourInput.Result.Error)
        assertTrue(FlourInput.validate("-1", UnitSystem.US_CUPS, 120.0) is FlourInput.Result.Error)
        assertTrue(FlourInput.validate("", UnitSystem.US_CUPS, 120.0) is FlourInput.Result.Error)
        assertTrue(FlourInput.validate("hello", UnitSystem.US_CUPS, 120.0) is FlourInput.Result.Error)
    }

    @Test
    fun `custom decimal amounts are accepted`() {
        for ((text, cups) in listOf("1.25" to 1.25, "1.75" to 1.75, "2.5" to 2.5, "0.5" to 0.5, "3" to 3.0)) {
            assertEquals(cups, ok(FlourInput.validate(text, UnitSystem.US_CUPS, 120.0)).cups, 1e-9)
        }
    }

    @Test
    fun `very large and very small amounts are limited with friendly messages`() {
        val big = err(FlourInput.validate("7", UnitSystem.US_CUPS, 120.0)).message
        assertTrue(big, big.contains("maximum"))
        val small = err(FlourInput.validate("0.1", UnitSystem.US_CUPS, 120.0)).message
        assertTrue(small, small.contains("minimum"))
        val warn = ok(FlourInput.validate("5", UnitSystem.US_CUPS, 120.0))
        assertTrue(warn.warning!!.contains("big batch"))
        assertNull(ok(FlourInput.validate("2", UnitSystem.US_CUPS, 120.0)).warning)
    }

    @Test
    fun `grams are converted with the recipe's own flour density`() {
        assertEquals(2.0, ok(FlourInput.validate("240", UnitSystem.METRIC, 120.0)).cups, 1e-9)
        assertEquals(2.0, ok(FlourInput.validate("254", UnitSystem.METRIC, 127.0)).cups, 1e-9)
        assertTrue(FlourInput.validate("20", UnitSystem.METRIC, 120.0) is FlourInput.Result.Error)
        assertTrue(FlourInput.validate("900", UnitSystem.METRIC, 120.0) is FlourInput.Result.Error)
        assertEquals("240 g", FlourInput.label(2.0, UnitSystem.METRIC, 120.0))
        assertEquals("2 cups", FlourInput.label(2.0, UnitSystem.US_CUPS, 120.0))
    }
}
