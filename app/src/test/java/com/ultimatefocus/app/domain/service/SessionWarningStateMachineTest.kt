/**
 * Unit tests for SessionWarningStateMachine — the pure rule that decides
 * when the 1-minute pre-end warning popup should fire. Covers the four
 * AC-derived scenarios explicitly:
 *  - threshold-crossing fires exactly once per session;
 *  - sub-threshold session (≤ 1 minute) fires immediately on start;
 *  - re-entering the threshold window (e.g. user backgrounded + returned)
 *    does not re-warn the same session;
 *  - the next session gets its own warning;
 *  - no warning past expiry (the expiry-mode branch owns that signal).
 *
 * Covers: ULT-80
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.ActiveSession
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionWarningStateMachineTest {

    private lateinit var machine: SessionWarningStateMachine

    private fun session(
        pkg: String = "com.alpha",
        startedEpochMs: Long = 1_000L,
        durationMinutes: Int = 5,
    ) = ActiveSession(
        packageName = pkg,
        appLabel = "Alpha",
        startedEpochMs = startedEpochMs,
        durationMinutes = durationMinutes,
        expiryMode = SessionExpiryMode.ONLY_REMINDER,
    )

    @Before
    fun setUp() { machine = SessionWarningStateMachine() }

    @Test
    fun nullSession_doesNotWarn() {
        assertFalse(machine.shouldWarn(null, nowMs = 1_000L))
    }

    @Test
    fun aboveThreshold_doesNotWarn() {
        val s = session(durationMinutes = 5) // ends at 1_000 + 300_000
        // remaining = 240_000 (4 min) > 60_000
        assertFalse(machine.shouldWarn(s, nowMs = 1_000L + 60_000L))
    }

    @Test
    fun atThresholdBoundary_warns() {
        val s = session(durationMinutes = 5) // ends at 1_000 + 300_000
        // remaining = exactly 60_000
        assertTrue(machine.shouldWarn(s, nowMs = 1_000L + 240_000L))
    }

    @Test
    fun belowThreshold_warns() {
        val s = session(durationMinutes = 5)
        assertTrue(machine.shouldWarn(s, nowMs = 1_000L + 250_000L))
    }

    @Test
    fun secondCallSameSession_doesNotReWarn() {
        val s = session(durationMinutes = 5)
        assertTrue(machine.shouldWarn(s, nowMs = 1_000L + 250_000L))
        assertFalse(machine.shouldWarn(s, nowMs = 1_000L + 260_000L))
    }

    @Test
    fun shortSession_warnsImmediately() {
        // 1-minute session: remaining = 60_000 from the very first tick.
        val s = session(durationMinutes = 1)
        assertTrue(machine.shouldWarn(s, nowMs = 1_000L))
    }

    @Test
    fun afterSessionEnds_doesNotWarn() {
        val s = session(durationMinutes = 1)
        // Past expiry — remaining negative.
        assertFalse(machine.shouldWarn(s, nowMs = 1_000L + 60_001L))
    }

    @Test
    fun newSessionAfterPrior_warnsAgain() {
        val first = session(pkg = "com.alpha", startedEpochMs = 1_000L, durationMinutes = 5)
        assertTrue(machine.shouldWarn(first, nowMs = 1_000L + 250_000L))
        // Session ends — clear the latch.
        machine.shouldWarn(null, nowMs = 1_000L + 300_001L)
        // Next session — different package + different start time.
        val second = session(
            pkg = "com.bravo",
            startedEpochMs = 2_000_000L,
            durationMinutes = 2,
        )
        assertTrue(machine.shouldWarn(second, nowMs = 2_000_000L + 70_000L))
    }

    @Test
    fun reset_releasesTheLatch() {
        val s = session(durationMinutes = 5)
        assertTrue(machine.shouldWarn(s, nowMs = 1_000L + 250_000L))
        machine.reset()
        assertTrue(machine.shouldWarn(s, nowMs = 1_000L + 260_000L))
    }
}
