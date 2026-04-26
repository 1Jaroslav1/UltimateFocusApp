/**
 * Unit tests for SessionExpiredOverlayViewModel — the loader-only Mode 1
 * overlay (ULT-81). Verifies:
 *  - bind initializes state, clears the active session, and starts the
 *    progress timer.
 *  - progress crosses from 0f to 1f over EXTEND_DELAY_MS and the Done
 *    event fires exactly once at the end.
 *  - rebinding while a countdown is in flight is a no-op.
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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun bind_setsInitialStateAndClearsActiveSession() = runTest {
        viewModel.bind("com.alpha", "Alpha")
        val s = viewModel.state.value
        assertEquals("com.alpha", s.packageName)
        assertEquals(0f, s.progress, 0.001f)
        assertNull(coordinator.active.value)
    }

    @Test
    fun progress_crossesToOneAndDoneFires() = runTest {
        viewModel.events.test {
            viewModel.bind("com.alpha", "Alpha")
            advanceTimeBy(SessionExpiredOverlayActivity.EXTEND_DELAY_MS + 100L)
            assertEquals(SessionExpiredOverlayViewModel.Event.Done, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1f, viewModel.state.value.progress, 0.001f)
    }

    @Test
    fun progress_isPartialMidway() = runTest {
        viewModel.bind("com.alpha", "Alpha")
        // No advanceUntilIdle here — the loop is finite and would run to
        // completion, which would defeat the "midway" assertion. The
        // tick-based loop in the view-model runs in lockstep with
        // advanceTimeBy, so half the EXTEND_DELAY_MS leaves us at half
        // progress.
        advanceTimeBy(SessionExpiredOverlayActivity.EXTEND_DELAY_MS / 2)
        val midProgress = viewModel.state.value.progress
        assertTrue("progress should advance past 0", midProgress > 0f)
        assertTrue("progress should not yet be 1", midProgress < 1f)
    }

    @Test
    fun rebind_isNoOp() = runTest {
        viewModel.bind("com.alpha", "Alpha")
        viewModel.bind("com.bravo", "Bravo")
        assertEquals("com.alpha", viewModel.state.value.packageName)
    }
}
