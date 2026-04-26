/**
 * Integration tests for SessionWarningController. Uses kotlinx-coroutines-
 * test virtual time to drive the internal 1-second ticker without sleeping
 * the test thread, and FakeClock to advance the wall clock the controller
 * compares against. Exercises the end-to-end path:
 *  - SessionTimerCoordinator → state-machine → SessionWarningEvent stream.
 *
 * Validates the AC for ULT-80:
 *  - exactly one event per session at the threshold;
 *  - immediate event for short (≤ 1 min) sessions;
 *  - no event when no session is in progress;
 *  - a brand-new session restarts eligibility.
 *
 * Covers: ULT-80
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionWarningControllerTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var clock: FakeClock
    private lateinit var coordinator: SessionTimerCoordinator
    private lateinit var controller: SessionWarningController

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        clock = FakeClock(current = 1_000L)
        coordinator = SessionTimerCoordinator(clock)
        controller = SessionWarningController(coordinator, clock)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun thresholdCrossing_emitsExactlyOnce() = runTest {
        coordinator.startSession("com.alpha", "Alpha", 5, SessionExpiryMode.ONLY_REMINDER)
        controller.warningEvents(tickIntervalMs = 1_000L).test {
            // Cross the 1-minute threshold — advance to remaining = 60s.
            clock.current = 1_000L + 240_000L
            advanceTimeBy(1_100L)
            val event = awaitItem()
            assertEquals("com.alpha", event.packageName)

            // Subsequent ticks within the same session should NOT re-emit.
            clock.current = 1_000L + 270_000L
            advanceTimeBy(5_000L)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun shortSession_emitsImmediately() = runTest {
        coordinator.startSession("com.alpha", "Alpha", 1, SessionExpiryMode.ONLY_REMINDER)
        controller.warningEvents(tickIntervalMs = 1_000L).test {
            // remaining at start = 60_000 ≤ threshold → fires on the first tick.
            advanceTimeBy(1_100L)
            val event = awaitItem()
            assertEquals("com.alpha", event.packageName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noActiveSession_doesNotEmit() = runTest {
        controller.warningEvents(tickIntervalMs = 1_000L).test {
            advanceTimeBy(10_000L)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun consecutiveSessions_eachGetTheirOwnWarning() = runTest {
        coordinator.startSession("com.alpha", "Alpha", 1, SessionExpiryMode.ONLY_REMINDER)
        controller.warningEvents(tickIntervalMs = 1_000L).test {
            advanceTimeBy(1_100L)
            assertEquals("com.alpha", awaitItem().packageName)

            // First session ends.
            coordinator.endSession()
            advanceTimeBy(2_000L)

            // Second session starts.
            clock.current = 100_000L
            coordinator.startSession("com.bravo", "Bravo", 1, SessionExpiryMode.EXIT_APP)
            advanceTimeBy(1_100L)
            assertEquals("com.bravo", awaitItem().packageName)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
