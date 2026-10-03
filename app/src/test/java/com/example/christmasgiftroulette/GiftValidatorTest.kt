package com.example.christmasgiftroulette

import com.example.christmasgiftroulette.game.GiftValidator
import com.example.christmasgiftroulette.model.CurrencyType
import com.example.christmasgiftroulette.model.GiftDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftValidatorTest {

    private fun draft(id: String, name: String, amount: String = "", currency: CurrencyType = CurrencyType.USD) =
        GiftDraft(id, name, amount, currency)

    @Test
    fun validGiftsWithAndWithoutAmountsPass() {
        val result = GiftValidator.validate(
            listOf(draft("1", "Nintendo Switch"), draft("2", "  Coffee Mug ", "18,000", CurrencyType.KRW)),
        )
        assertTrue(result.isValid)
        assertEquals(listOf("Nintendo Switch", "Coffee Mug"), result.gifts.map { it.name })
        assertNull(result.gifts[0].amount)
        assertEquals("₩18,000", result.gifts[1].formattedAmount)
    }

    @Test
    fun missingNameIsReportedPerRow() {
        val result = GiftValidator.validate(listOf(draft("1", "Mug"), draft("2", "   ")))
        assertFalse(result.isValid)
        assertNotNull(result.rowErrors["2"]?.nameError)
        assertNull(result.rowErrors["1"])
        assertNotNull(result.generalError)
    }

    @Test
    fun invalidAmountIsReported() {
        val result = GiftValidator.validate(listOf(draft("1", "Mug", "abc"), draft("2", "Car", "5")))
        assertFalse(result.isValid)
        assertNotNull(result.rowErrors["1"]?.amountError)
    }

    @Test
    fun fewerThanTwoGiftsIsRejected() {
        val result = GiftValidator.validate(listOf(draft("1", "Only one")))
        assertFalse(result.isValid)
        assertNotNull(result.generalError)
    }

    @Test
    fun duplicateNamesAreAllowed() {
        val result = GiftValidator.validate(listOf(draft("1", "Mug"), draft("2", "Mug")))
        assertTrue(result.isValid)
        assertEquals(2, result.gifts.map { it.id }.toSet().size)
    }

    @Test
    fun fiftyGiftsAreAllowedAndFiftyOneAreNot() {
        assertTrue(GiftValidator.validate(List(50) { draft("$it", "G$it") }).isValid)
        assertFalse(GiftValidator.validate(List(51) { draft("$it", "G$it") }).isValid)
    }
}
