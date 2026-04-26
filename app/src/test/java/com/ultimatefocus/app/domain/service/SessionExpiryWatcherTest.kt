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
 *  - a brand-new session restarts eligibility.
 *
 * Covers: ULT-81
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
}
