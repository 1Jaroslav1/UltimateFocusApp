/**
 * Unit tests for MindfulLaunchDelayViewModel — the post-picker delay
 * overlay (ULT-79). Validates:
 *  - bind() initializes state and starts the countdown.
 *  - the `Done` event is emitted after exactly DELAY_MS.
 *  - completeLaunch() dispatches AppRepository.launchApp with the
 *    resolved AppInfo (so the existing block gates still apply).
 *  - re-binding while a countdown is in flight is a no-op.
 *
 * Uses kotlinx-coroutines-test virtual time to avoid sleeping the test
 * thread.
 *
 * Covers: ULT-79
 */
package com.ultimatefocus.app.ui.sessiontimer

import android.content.ComponentName
import app.cash.turbine.test
import com.ultimatefocus.app.data.repository.FakeAppRepository
import com.ultimatefocus.app.domain.model.AppInfo
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MindfulLaunchDelayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeApps: FakeAppRepository
    private lateinit var viewModel: MindfulLaunchDelayViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeApps = FakeAppRepository()
        fakeApps.setApps(
            listOf(
                AppInfo(
                    packageName = "com.alpha",
                    label = "Alpha",
                    componentName = ComponentName("com.alpha", "com.alpha.Main"),
                ),
            ),
        )
        viewModel = MindfulLaunchDelayViewModel(fakeApps)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun bind_setsStateAndStartsCountdown() = runTest {
        viewModel.bind("com.alpha", "Alpha")
        val s = viewModel.state.value
        assertEquals("com.alpha", s.packageName)
        assertEquals("Alpha", s.appLabel)
        assertTrue(s.started)
    }

    @Test
    fun done_emitsAfterDelayMs() = runTest {
        viewModel.events.test {
            viewModel.bind("com.alpha", "Alpha")
            // Advance just before the delay finishes — no event yet.
            advanceTimeBy(MindfulLaunchDelayActivity.DELAY_MS - 1)
            expectNoEvents()
            // Advance past — Done should fire.
            advanceTimeBy(2)
            assertEquals(MindfulLaunchDelayViewModel.Event.Done, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun completeLaunch_invokesAppRepositoryLaunchApp() = runTest {
        viewModel.bind("com.alpha", "Alpha")
        advanceUntilIdle()
        viewModel.completeLaunch()
        advanceUntilIdle()
        assertEquals("com.alpha", fakeApps.launchedApp?.packageName)
    }

    @Test
    fun completeLaunch_isNoOpWhenPackageIsBlank() = runTest {
        viewModel.completeLaunch()
        advanceUntilIdle()
        assertNull(fakeApps.launchedApp)
    }

    @Test
    fun rebind_whileStartedIsNoOp() = runTest {
        viewModel.bind("com.alpha", "Alpha")
        viewModel.bind("com.bravo", "Bravo")
        // The first binding wins — re-binding is ignored to avoid restarting
        // the timer on configuration change.
        assertEquals("com.alpha", viewModel.state.value.packageName)
    }
}
