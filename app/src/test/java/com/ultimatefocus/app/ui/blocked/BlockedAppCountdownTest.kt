/**
 * JVM unit tests for the countdown helper that powers BlockedAppScreen.
 * Replaces the deleted BlockedAppViewModelTest — the per-tick math used
 * to live in BlockedAppViewModel (ULT-21) but moved into the
 * computeRemainingMs helper when the screen was simplified to drop the
 * ViewModel for ULT-84.
 *
 * Covers: ULT-21, ULT-84
 */
package com.ultimatefocus.app.ui.blocked

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockedAppCountdownTest {

    @Test
    fun computeRemaining_returnsDeltaWhenInFuture() {
        val now = 1_000L
        val until = 1_000L + 60_000L
        assertEquals(60_000L, computeRemainingMs(until, now))
    }

    @Test
    fun computeRemaining_returnsZeroWhenAlreadyPast() {
        assertEquals(0L, computeRemainingMs(blockedUntilEpochMs = 1L, nowMs = 1_000_000L))
    }

    @Test
    fun computeRemaining_returnsZeroWhenExactlyEqual() {
        assertEquals(0L, computeRemainingMs(blockedUntilEpochMs = 1_000L, nowMs = 1_000L))
    }

    @Test
    fun computeRemaining_isMonotonicAsNowAdvances() {
        val until = 1_000_000L
        val first = computeRemainingMs(until, nowMs = 100L)
        val second = computeRemainingMs(until, nowMs = 5_000L)
        val third = computeRemainingMs(until, nowMs = 10_000L)
        assert(first > second)
        assert(second > third)
    }
}
