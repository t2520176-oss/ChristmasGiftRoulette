package com.example.christmasgiftroulette

import com.example.christmasgiftroulette.game.RouletteEngine
import com.example.christmasgiftroulette.model.CurrencyType
import com.example.christmasgiftroulette.model.GiftItem
import java.math.BigDecimal
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RouletteEngineTest {

    private fun gifts(n: Int, name: (Int) -> String = { "Gift $it" }) =
        List(n) { GiftItem("id-$it", name(it), BigDecimal(it), CurrencyType.USD) }

    @Test
    fun selectedGiftIsRemovedFromRemaining() {
        val start = RouletteEngine.newGame(gifts(5))
        val after = RouletteEngine.select(start, "id-2")
        assertEquals(4, after.remainingGifts.size)
        assertFalse(after.remainingGifts.any { it.id == "id-2" })
        assertEquals(listOf("id-2"), after.selectedGifts.map { it.id })
        assertEquals("id-2", after.currentWinner?.id)
        assertEquals(5, after.gifts.size)
    }

    @Test
    fun originalStateIsNotMutated() {
        val start = RouletteEngine.newGame(gifts(3))
        RouletteEngine.select(start, "id-0")
        assertEquals(3, start.remainingGifts.size)
        assertTrue(start.selectedGifts.isEmpty())
    }

    @Test
    fun selectedGiftCannotBeSelectedTwice() {
        val once = RouletteEngine.select(RouletteEngine.newGame(gifts(3)), "id-1")
        val twice = RouletteEngine.select(once, "id-1")
        assertSame(once, twice)
        assertEquals(1, twice.selectedGifts.size)
        assertEquals(2, twice.remainingGifts.size)
    }

    @Test
    fun unknownIdIsIgnored() {
        val start = RouletteEngine.newGame(gifts(3))
        assertSame(start, RouletteEngine.select(start, "nope"))
    }

    @Test
    fun pickWinnerOnlyDrawsFromRemainingAndAllGiftsEventuallyGetSelectedExactlyOnce() {
        for (seed in 0 until 25) {
            val random = Random(seed)
            val total = 12
            var state = RouletteEngine.newGame(gifts(total))
            val order = mutableListOf<String>()
            for (expectedRemaining in total downTo 1) {
                assertEquals(expectedRemaining, state.remainingGifts.size)
                val winner = RouletteEngine.pickWinner(state, random)
                assertNotNull(winner)
                assertTrue("winner must come from remaining", state.remainingGifts.any { it.id == winner!!.id })
                assertFalse("winner must not repeat", winner!!.id in order)
                order += winner.id
                state = RouletteEngine.select(state, winner.id)
            }
            assertTrue(state.isCompleted)
            assertEquals(0, state.remainingGifts.size)
            assertEquals(total, state.selectedGifts.size)
            assertEquals(order, state.selectedGifts.map { it.id })
            assertEquals(total, order.toSet().size)
            assertNull(RouletteEngine.pickWinner(state, random))
        }
    }

    @Test
    fun duplicateNamesAreSeparateItems() {
        val twins = gifts(4) { "Mug" }
        var state = RouletteEngine.newGame(twins)
        state = RouletteEngine.select(state, "id-1")
        assertEquals(3, state.remainingGifts.size)
        assertEquals(listOf("id-0", "id-2", "id-3"), state.remainingGifts.map { it.id })
        // another "Mug" is still selectable
        state = RouletteEngine.select(state, "id-3")
        assertEquals(listOf("id-1", "id-3"), state.selectedGifts.map { it.id })
        assertEquals(listOf("Mug", "Mug"), state.selectedGifts.map { it.name })
    }

    @Test
    fun pickIsDeterministicForSeededRandom() {
        val state = RouletteEngine.newGame(gifts(10))
        val a = RouletteEngine.pickWinner(state, Random(42))
        val b = RouletteEngine.pickWinner(state, Random(42))
        assertEquals(a, b)
    }

    @Test
    fun pickIsRoughlyUniform() {
        val state = RouletteEngine.newGame(gifts(4))
        val random = Random(7)
        val counts = IntArray(4)
        repeat(4000) { counts[RouletteEngine.pickWinner(state, random)!!.id.removePrefix("id-").toInt()]++ }
        counts.forEach { assertTrue("count $it should be near 1000", it in 800..1200) }
    }

    @Test
    fun resetRestoresAllGifts() {
        val original = gifts(6)
        var state = RouletteEngine.newGame(original)
        state = RouletteEngine.select(state, "id-0")
        state = RouletteEngine.select(state, "id-5")
        val reset = RouletteEngine.restart(state)
        assertEquals(original, reset.remainingGifts)
        assertTrue(reset.selectedGifts.isEmpty())
        assertNull(reset.currentWinner)
        assertEquals(original, reset.gifts)
    }

    @Test
    fun twoGiftGameEndsAfterTwoPicks() {
        var state = RouletteEngine.newGame(gifts(2))
        state = RouletteEngine.select(state, RouletteEngine.pickWinner(state, Random(1))!!.id)
        assertEquals(1, state.remainingGifts.size)
        assertFalse(state.isCompleted)
        state = RouletteEngine.select(state, RouletteEngine.pickWinner(state, Random(1))!!.id)
        assertTrue(state.isCompleted)
    }

    @Test
    fun restoreRebuildsStateAndRejectsCorruptData() {
        val all = gifts(4)
        val restored = RouletteEngine.restore(all, listOf("id-0", "id-3"), listOf("id-2", "id-1"), "id-1")
        assertNotNull(restored)
        assertEquals(listOf("id-2", "id-1"), restored!!.selectedGifts.map { it.id })
        assertEquals("id-1", restored.currentWinner?.id)

        assertNull(RouletteEngine.restore(all, listOf("id-0"), listOf("id-1"), null)) // missing gifts
        assertNull(RouletteEngine.restore(all, listOf("id-0", "id-1", "id-2", "id-3"), listOf("id-0"), null)) // overlap
        assertNull(RouletteEngine.restore(all, listOf("ghost", "id-1", "id-2", "id-3"), emptyList(), null))
    }
}
