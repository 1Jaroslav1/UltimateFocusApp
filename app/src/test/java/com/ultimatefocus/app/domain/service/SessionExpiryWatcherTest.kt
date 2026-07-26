/**
 * Integration tests for SessionExpiryWatcher. Drives the internal ticker
 * with kotlinx-coroutines-test virtual time and FakeClock for the wall
 * clock so the entire pipeline (active session → ticker → effect emit)
 * is covered without sleeping.
 *
 * Validates:
 *  - exactly one effect per session (latched until session clears);
 *  - no emission while remaining > 0;
 *  - the effect carries the right payload per mode;
 *  - a brand-new session restarts eligibility;
 *  - switching away from the session's package (ULT-86) pauses the
 *    countdown so expiry never fires against an unrelated foreground app,
 *    and resuming lets it eventually expire against the remaining budget.
 *
 * Covers: ULT-81, ULT-86
 */
package com.ultimatefocus.app.domain.service

import app.cash.turbine.test
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionExpiryWatcherTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var clock: FakeClock
    private lateinit var coordinator: SessionTimerCoordinator
    private lateinit var watcher: SessionExpiryWatcher

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        clock = FakeClock(current = 1_000L)
        coordinator = SessionTimerCoordinator(clock)
        watcher = SessionExpiryWatcher(coordinator, clock)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun expiry_emitsExactlyOnce() = runTest {
        coordinator.startSession("com.alpha", "Alpha", 1, SessionExpiryMode.EXIT_APP)
        watcher.expiryEffects(tickIntervalMs = 1_000L).test {
            // Pre-expiry — no event.
            advanceTimeBy(2_000L)
            expectNoEvents()
            // Cross expiry.
            clock.current = 1_000L + 60_000L
            advanceTimeBy(1_100L)
            val effect = awaitItem()
            assertTrue(effect is SessionExpiryEffect.GoHomeOnly)

            // Subsequent ticks — no re-emission.
            advanceTimeBy(5_000L)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun exitAndBlock_emitsBlockDurationMatchingSession() = runTest {
        coordinator.startSession("com.alpha", "Alpha", 7, SessionExpiryMode.EXIT_AND_BLOCK)
        watcher.expiryEffects(tickIntervalMs = 1_000L).test {
            clock.current = 1_000L + 7 * 60_000L + 1L
            advanceTimeBy(1_100L)
            val effect = awaitItem() as SessionExpiryEffect.GoHomeAndBlock
            assertEquals(7, effect.blockDurationMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun mindfulExtensionDelay_emitsOverlayCountdown() = runTest {
        coordinator.startSession(
            "com.alpha", "Alpha", 1, SessionExpiryMode.MINDFUL_EXTENSION_DELAY,
        )
        watcher.expiryEffects(tickIntervalMs = 1_000L).test {
            clock.current = 1_000L + 60_001L
            advanceTimeBy(1_100L)
            assertTrue(awaitItem() is SessionExpiryEffect.LaunchOverlayCountdown)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun consecutiveSessions_eachGetTheirOwnEffect() = runTest {
        coordinator.startSession("com.alpha", "Alpha", 1, SessionExpiryMode.EXIT_APP)
        watcher.expiryEffects(tickIntervalMs = 1_000L).test {
            clock.current = 1_000L + 60_001L
            advanceTimeBy(1_100L)
            assertEquals("com.alpha", awaitItem().packageName)

            // Clear and start a new session.
            coordinator.endSession()
            advanceTimeBy(2_000L)
            clock.current = 200_000L
            coordinator.startSession("com.bravo", "Bravo", 1, SessionExpiryMode.EXIT_APP)
            clock.current = 200_000L + 60_001L
            advanceTimeBy(1_100L)
            assertEquals("com.bravo", awaitItem().packageName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noActiveSession_doesNotEmit() = runTest {
        watcher.expiryEffects(tickIntervalMs = 1_000L).test {
            advanceTimeBy(10_000L)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun awayFromSessionPackage_doesNotExpireEvenPastOriginalDeadline() = runTest {
        coordinator.startSession("com.alpha", "Alpha", 1, SessionExpiryMode.EXIT_APP)
        watcher.expiryEffects(tickIntervalMs = 1_000L).test {
            // 10s of real use, then the user switches to an untouched app.
            clock.current = 1_000L + 10_000L
            coordinator.onForegroundPackageChanged("com.messenger", clock.current)
            // Well past the original 1-minute deadline — must not fire.
            clock.current = 1_000L + 120_000L
            advanceTimeBy(1_100L)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun resumingSessionPackage_eventuallyExpiresAgainstRemainingBudget() = runTest {
        coordinator.startSession("com.alpha", "Alpha", 1, SessionExpiryMode.EXIT_APP)
        watcher.expiryEffects(tickIntervalMs = 1_000L).test {
            clock.current = 1_000L + 10_000L
            coordinator.onForegroundPackageChanged("com.messenger", clock.current)
            clock.current = 1_000L + 120_000L
            advanceTimeBy(1_100L)
            expectNoEvents()

            // Resume: only 50s of the original 60s budget remain.
            coordinator.onForegroundPackageChanged("com.alpha", clock.current)
            clock.current += 50_000L + 1L
            advanceTimeBy(1_100L)
            val effect = awaitItem()
            assertEquals("com.alpha", effect.packageName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun budgetExhaustedExactlyAtSwitchAway_doesNotFireAgainstNewForegroundApp() = runTest {
        // Regression test for the exact tie-race: the budget hits zero in
        // the very same instant the user switches away. remainingMs alone
        // would read 0 here; the explicit isPaused gate must still block it.
        coordinator.startSession("com.alpha", "Alpha", 1, SessionExpiryMode.EXIT_APP)
        watcher.expiryEffects(tickIntervalMs = 1_000L).test {
            clock.current = 1_000L + 60_000L // exactly the full budget consumed
            coordinator.onForegroundPackageChanged("com.messenger", clock.current)
            advanceTimeBy(1_100L)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun resumingWithAlreadyExhaustedBudget_firesImmediately() = runTest {
        coordinator.startSession("com.alpha", "Alpha", 1, SessionExpiryMode.EXIT_APP)
        watcher.expiryEffects(tickIntervalMs = 1_000L).test {
            clock.current = 1_000L + 60_000L
            coordinator.onForegroundPackageChanged("com.messenger", clock.current)
            advanceTimeBy(1_100L)
            expectNoEvents()

            // User comes back to the session's own package with 0 budget
            // left — must evict immediately, not silently stay paused.
            coordinator.onForegroundPackageChanged("com.alpha", clock.current)
            advanceTimeBy(1_100L)
            val effect = awaitItem()
            assertEquals("com.alpha", effect.packageName)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
