/**
 * Unit tests for SessionDurationMinutes value class. Validates the
 * acceptable range (1..240), the coerce helper, and millisecond
 * conversion used by SessionTimerCoordinator.
 *
 * Covers: ULT-78
 */
package com.ultimatefocus.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionDurationMinutesTest {

    @Test
    fun construct_withMinValue_succeeds() {
        assertEquals(1, SessionDurationMinutes(1).value)
    }

    @Test
    fun construct_withMaxValue_succeeds() {
        assertEquals(240, SessionDurationMinutes(240).value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun construct_withZero_throws() {
        SessionDurationMinutes(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun construct_aboveMax_throws() {
        SessionDurationMinutes(241)
    }

    @Test
    fun coerce_clampsBelowMinUp() {
        assertEquals(1, SessionDurationMinutes.coerce(0).value)
        assertEquals(1, SessionDurationMinutes.coerce(-99).value)
    }

    @Test
    fun coerce_clampsAboveMaxDown() {
        assertEquals(240, SessionDurationMinutes.coerce(9_999).value)
    }

    @Test
    fun toMillis_returnsValueTimes60_000() {
        assertEquals(60_000L, SessionDurationMinutes(1).toMillis())
        assertEquals(15 * 60_000L, SessionDurationMinutes(15).toMillis())
    }

    @Test
    fun isValid_returnsTrueInsideRange() {
        assertTrue(SessionDurationMinutes.isValid(1))
        assertTrue(SessionDurationMinutes.isValid(240))
        assertTrue(SessionDurationMinutes.isValid(120))
    }

    @Test
    fun isValid_returnsFalseOutsideRange() {
        assertFalse(SessionDurationMinutes.isValid(0))
        assertFalse(SessionDurationMinutes.isValid(-1))
        assertFalse(SessionDurationMinutes.isValid(241))
    }

    @Test
    fun presets_containAllAcceptanceValues() {
        assertEquals(listOf(1, 5, 10, 15), SessionDurationMinutes.PRESETS)
    }
}
