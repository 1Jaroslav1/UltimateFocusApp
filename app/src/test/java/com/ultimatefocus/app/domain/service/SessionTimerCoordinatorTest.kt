/**
 * Unit tests for SessionTimerCoordinator. Verifies the lifecycle invariants
 * the launch interceptors and expiry watcher rely on:
 *  - at most one active session at a time;
 *  - isSessionActiveFor returns true only inside the time window for the
 *    matching package;
 *  - remainingMsFor counts down via the injected Clock;
 *  - endSession clears the slot;
 *  - onForegroundPackageChanged pauses the countdown while the user is away
 *    from the session's package and resumes it when they come back (ULT-86).
 *
 * Covers: ULT-78, ULT-86
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.SessionExpiryMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionTimerCoordinatorTest {

    private lateinit var clock: FakeClock
    private lateinit var coordinator: SessionTimerCoordinator

    @Before
    fun setUp() {
        clock = FakeClock(current = 1_000L)
        coordinator = SessionTimerCoordinator(clock)
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun startSession_returnsActiveAndExposesEnd() {
        val session = coordinator.startSession(
            packageName = "com.alpha",
            appLabel = "Alpha",
            durationMinutes = 5,
            expiryMode = SessionExpiryMode.ONLY_REMINDER,
        )
        assertEquals("com.alpha", session.packageName)
        assertEquals(5 * 60_000L, coordinator.remainingMsFor("com.alpha"))
        assertTrue(coordinator.isSessionActiveFor("com.alpha"))
    }

    @Test
    fun isSessionActiveFor_returnsFalseForOtherPackage() {
        coordinator.startSession("com.alpha", "Alpha", 5, SessionExpiryMode.ONLY_REMINDER)
        assertFalse(coordinator.isSessionActiveFor("com.bravo"))
    }

    @Test
    fun remainingMsFor_decreasesAsClockAdvances() {
        coordinator.startSession("com.alpha", "Alpha", 5, SessionExpiryMode.ONLY_REMINDER)
        assertEquals(5 * 60_000L, coordinator.remainingMsFor("com.alpha"))
        clock.current += 60_000L
        assertEquals(4 * 60_000L, coordinator.remainingMsFor("com.alpha"))
    }

    @Test
    fun endSession_clearsActive() {
        coordinator.startSession("com.alpha", "Alpha", 5, SessionExpiryMode.ONLY_REMINDER)
        coordinator.endSession()
        assertFalse(coordinator.isSessionActiveFor("com.alpha"))
        assertNull(coordinator.active.value)
        assertNull(coordinator.remainingMsFor("com.alpha"))
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun isSessionActiveFor_returnsFalseAfterDurationExpires() {
        coordinator.startSession("com.alpha", "Alpha", 1, SessionExpiryMode.ONLY_REMINDER)
        clock.current += 60_000L // exactly at end
        assertFalse(coordinator.isSessionActiveFor("com.alpha"))
        clock.current += 1L // one ms past
        assertFalse(coordinator.isSessionActiveFor("com.alpha"))
    }

    @Test
    fun startSession_replacesPriorSession() {
        coordinator.startSession("com.alpha", "Alpha", 5, SessionExpiryMode.ONLY_REMINDER)
        coordinator.startSession("com.bravo", "Bravo", 10, SessionExpiryMode.EXIT_APP)
        assertFalse(coordinator.isSessionActiveFor("com.alpha"))
        assertTrue(coordinator.isSessionActiveFor("com.bravo"))
    }

    @Test
    fun remainingMsFor_returnsZeroAtBoundaryNotNegative() {
        coordinator.startSession("com.alpha", "Alpha", 1, SessionExpiryMode.ONLY_REMINDER)
        clock.current += 60_000L * 5 // way past end
        assertEquals(0L, coordinator.remainingMsFor("com.alpha"))
    }

    @Test
    fun active_emitsStartedSession() {
        assertNull(coordinator.active.value)
        coordinator.startSession("com.alpha", "Alpha", 5, SessionExpiryMode.EXIT_AND_BLOCK)
        val s = coordinator.active.value
        assertNotNull(s)
        assertEquals(SessionExpiryMode.EXIT_AND_BLOCK, s!!.expiryMode)
    }

    // ── Foreground pause/resume (ULT-86) ────────────────────────────────────

    @Test
    fun onForegroundPackageChanged_awayFromSessionPackage_pausesRemaining() {
        coordinator.startSession("com.alpha", "Alpha", 15, SessionExpiryMode.EXIT_APP)
        clock.current += 10 * 60_000L // 10 min of real use
        coordinator.onForegroundPackageChanged("com.messenger", clock.current)
        clock.current += 60 * 60_000L // an hour goes by on a different app
        assertEquals(5 * 60_000L, coordinator.remainingMsFor("com.alpha"))
        assertTrue(coordinator.isSessionActiveFor("com.alpha"))
    }

    @Test
    fun onForegroundPackageChanged_backToOwnPackage_resumesCountdown() {
        coordinator.startSession("com.alpha", "Alpha", 15, SessionExpiryMode.EXIT_APP)
        clock.current += 10 * 60_000L
        coordinator.onForegroundPackageChanged("com.messenger", clock.current)
        clock.current += 60 * 60_000L
        coordinator.onForegroundPackageChanged("com.alpha", clock.current)
        clock.current += 5 * 60_000L - 1L
        assertTrue(coordinator.isSessionActiveFor("com.alpha"))
        clock.current += 1L
        assertEquals(0L, coordinator.remainingMsFor("com.alpha"))
        assertFalse(coordinator.isSessionActiveFor("com.alpha"))
    }

    @Test
    fun onForegroundPackageChanged_systemUiPackage_doesNotPause() {
        coordinator.startSession("com.alpha", "Alpha", 15, SessionExpiryMode.EXIT_APP)
        clock.current += 5 * 60_000L
        coordinator.onForegroundPackageChanged("com.android.systemui", clock.current)
        clock.current += 60 * 60_000L
        assertEquals(0L, coordinator.remainingMsFor("com.alpha"))
        assertFalse(coordinator.isSessionActiveFor("com.alpha"))
    }

    @Test
    fun onForegroundPackageChanged_noActiveSession_isNoOp() {
        coordinator.onForegroundPackageChanged("com.alpha", clock.current)
        assertNull(coordinator.active.value)
    }

    @Test
    fun onForegroundPackageChanged_nullOrBlankPackage_isNoOp() {
        coordinator.startSession("com.alpha", "Alpha", 15, SessionExpiryMode.EXIT_APP)
        coordinator.onForegroundPackageChanged(null, clock.current)
        coordinator.onForegroundPackageChanged("", clock.current)
        assertEquals(15 * 60_000L, coordinator.remainingMsFor("com.alpha"))
    }
}
