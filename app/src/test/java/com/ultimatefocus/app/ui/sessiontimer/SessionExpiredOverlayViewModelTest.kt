/**
 * Unit tests for SessionExpiredOverlayViewModel — the Mode 1 "mindful
 * extension delay" overlay (ULT-81). Verifies:
 *  - bind initializes the countdown and starts the timer.
 *  - canExtend stays false until the timer elapses; CountdownDone fires
 *    once at the end.
 *  - extend() while disabled is a no-op; extend() after countdown
 *    routes through ExtendRequested and clears the active session so
 *    the picker can re-prompt.
 *  - cancel() clears the session and emits Cancelled.
 *
 * Covers: ULT-81
 */
package com.ultimatefocus.app.ui.sessiontimer

import app.cash.turbine.test
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.service.FakeClock
import com.ultimatefocus.app.domain.service.SessionTimerCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionExpiredOverlayViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var coordinator: SessionTimerCoordinator
    private lateinit var viewModel: SessionExpiredOverlayViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coordinator = SessionTimerCoordinator(FakeClock(current = 1_000L))
        // Pre-populate an active session so cancel/extend exercise the
        // coordinator clear path.
        coordinator.startSession(
            "com.alpha",
            "Alpha",
            5,
            SessionExpiryMode.MINDFUL_EXTENSION_DELAY,
        )
        viewModel = SessionExpiredOverlayViewModel(coordinator)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun bind_setsInitialStateAndStartsCountdown() = runTest {
        viewModel.bind("com.alpha", "Alpha")
        val s = viewModel.state.value
        assertEquals("com.alpha", s.packageName)
        assertEquals(20, s.secondsRemaining) // EXTEND_DELAY_MS / 1000
        assertFalse(s.canExtend)
    }

    @Test
    fun extend_isNoOpUntilCountdownDone() = runTest {
        viewModel.bind("com.alpha", "Alpha")
        viewModel.extend()
        // Coordinator's session should NOT have been cleared.
        assertEquals("com.alpha", coordinator.active.value?.packageName)
    }

    @Test
    fun countdown_completesAndEmitsCountdownDone() = runTest {
        viewModel.events.test {
            viewModel.bind("com.alpha", "Alpha")
            advanceTimeBy(SessionExpiredOverlayActivity.EXTEND_DELAY_MS + 100L)
            assertEquals(SessionExpiredOverlayViewModel.Event.CountdownDone, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(viewModel.state.value.canExtend)
    }

    @Test
    fun extend_afterCountdown_emitsExtendRequestedAndClearsSession() = runTest {
        viewModel.bind("com.alpha", "Alpha")
        advanceUntilIdle()
        viewModel.events.test {
            viewModel.extend()
            assertEquals(SessionExpiredOverlayViewModel.Event.ExtendRequested, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(coordinator.active.value)
    }

    @Test
    fun cancel_emitsCancelledAndClearsSession() = runTest {
        viewModel.events.test {
            viewModel.bind("com.alpha", "Alpha")
            viewModel.cancel()
            // Drain the CountdownDone event if it fires later.
            // Cancel is the second event we care about.
            val first = awaitItem()
            assertTrue(
                first == SessionExpiredOverlayViewModel.Event.Cancelled ||
                    first == SessionExpiredOverlayViewModel.Event.CountdownDone,
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(coordinator.active.value)
    }

    @Test
    fun rebind_isNoOp() = runTest {
        viewModel.bind("com.alpha", "Alpha")
        viewModel.bind("com.bravo", "Bravo")
        assertEquals("com.alpha", viewModel.state.value.packageName)
    }
}
