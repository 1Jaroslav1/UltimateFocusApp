/**
 * Unit tests for SessionDurationPickerViewModel — the per-launch session
 * duration picker (ULT-78). Validates state transitions (preset selection,
 * expanded panel toggle, custom-minutes validation, per-session expiry
 * mode override) and the event surface (LaunchTarget on confirmation,
 * Cancelled on dismiss).
 *
 * Covers: ULT-78
 */
package com.ultimatefocus.app.ui.sessiontimer

import android.content.ComponentName
import app.cash.turbine.test
import com.ultimatefocus.app.data.local.dao.FakeSessionTimerAppDao
import com.ultimatefocus.app.data.repository.FakeAppRepository
import com.ultimatefocus.app.data.repository.SessionTimerRepositoryImpl
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.repository.FakeUserPreferencesSource
import com.ultimatefocus.app.domain.service.FakeClock
import com.ultimatefocus.app.domain.service.SessionTimerCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class SessionDurationPickerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeApps: FakeAppRepository
    private lateinit var coordinator: SessionTimerCoordinator
    private lateinit var fakePrefs: FakeUserPreferencesSource
    private lateinit var sessionTimerRepository: SessionTimerRepositoryImpl
    private lateinit var viewModel: SessionDurationPickerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeApps = FakeAppRepository()
        coordinator = SessionTimerCoordinator(FakeClock(current = 1_000L))
        fakePrefs = FakeUserPreferencesSource()
        sessionTimerRepository = SessionTimerRepositoryImpl(
            fakePrefs,
            FakeSessionTimerAppDao(),
            FakeClock(current = 1_000L),
        )
        viewModel = SessionDurationPickerViewModel(
            fakeApps,
            coordinator,
            sessionTimerRepository,
        )
        viewModel.bind("com.alpha", "Alpha App", SessionExpiryMode.EXIT_APP)
        fakeApps.setApps(
            listOf(
                AppInfo(
                    packageName = "com.alpha",
                    label = "Alpha App",
                    componentName = ComponentName("com.alpha", "com.alpha.Main"),
                ),
            ),
        )
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun bind_initializesStateWithDefaultMode() {
        val s = viewModel.state.value
        assertEquals("com.alpha", s.packageName)
        assertEquals("Alpha App", s.appLabel)
        assertEquals(SessionExpiryMode.EXIT_APP, s.selectedMode)
        assertFalse(s.expanded)
    }

    @Test
    fun toggleExpanded_flipsExpandedFlag() {
        viewModel.toggleExpanded()
        assertTrue(viewModel.state.value.expanded)
        viewModel.toggleExpanded()
        assertFalse(viewModel.state.value.expanded)
    }

    @Test
    fun confirmPreset_startsSessionAndEmitsLaunchEvent() = runTest {
        viewModel.events.test {
            viewModel.confirmPreset(5)
            val event = awaitItem()
            assertTrue(event is SessionDurationPickerViewModel.Event.LaunchTarget)
            assertEquals(
                "com.alpha",
                (event as SessionDurationPickerViewModel.Event.LaunchTarget).packageName,
            )
            assertTrue(coordinator.isSessionActiveFor("com.alpha"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmPreset_recordsExpiryModeOnSession() = runTest {
        viewModel.onSelectExpiryMode(SessionExpiryMode.EXIT_AND_BLOCK)
        viewModel.confirmPreset(10)
        assertEquals(SessionExpiryMode.EXIT_AND_BLOCK, coordinator.active.value!!.expiryMode)
        assertEquals(10, coordinator.active.value!!.durationMinutes)
    }

    @Test
    fun cancel_emitsCancelledEvent() = runTest {
        viewModel.events.test {
            viewModel.cancel()
            assertEquals(
                SessionDurationPickerViewModel.Event.Cancelled,
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(coordinator.active.value)
    }

    @Test
    fun launchTarget_dispatchesLaunchAppOnAppRepository() = runTest {
        viewModel.confirmPreset(5)
        viewModel.launchTarget()
        // Wait briefly for the launch coroutine; UnconfinedTestDispatcher
        // makes this synchronous.
        assertEquals("com.alpha", fakeApps.launchedApp?.packageName)
    }

    // ── Custom minutes ───────────────────────────────────────────────────────

    @Test
    fun onCustomMinutesChange_acceptsDigitsOnlyAndValidates() {
        viewModel.onCustomMinutesChange("12abc3")
        val s = viewModel.state.value
        assertEquals("123", s.customMinutes)
        assertTrue(s.customMinutesValid)
    }

    @Test
    fun onCustomMinutesChange_belowMin_isInvalid() {
        viewModel.onCustomMinutesChange("0")
        assertFalse(viewModel.state.value.customMinutesValid)
    }

    @Test
    fun onCustomMinutesChange_aboveMax_isInvalid() {
        viewModel.onCustomMinutesChange("241")
        assertFalse(viewModel.state.value.customMinutesValid)
    }

    @Test
    fun confirmCustom_doesNothingWhenInvalid() = runTest {
        viewModel.onCustomMinutesChange("0")
        viewModel.confirmCustom()
        assertNull(coordinator.active.value)
    }

    @Test
    fun confirmCustom_startsSessionWhenValid() = runTest {
        viewModel.onCustomMinutesChange("42")
        viewModel.events.test {
            viewModel.confirmCustom()
            assertTrue(awaitItem() is SessionDurationPickerViewModel.Event.LaunchTarget)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(42, coordinator.active.value!!.durationMinutes)
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    // ── Mindful launch delay (ULT-79) ────────────────────────────────────────

    @Test
    fun confirmPreset_emitsLaunchAfterDelayWhenMindfulDelayOn() = runTest {
        sessionTimerRepository.updateMindfulLaunchDelay(true)
        viewModel.events.test {
            viewModel.confirmPreset(5)
            val event = awaitItem()
            assertTrue(event is SessionDurationPickerViewModel.Event.LaunchAfterDelay)
            assertEquals(
                "com.alpha",
                (event as SessionDurationPickerViewModel.Event.LaunchAfterDelay).packageName,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmPreset_emitsLaunchTargetWhenMindfulDelayOff() = runTest {
        sessionTimerRepository.updateMindfulLaunchDelay(false)
        viewModel.events.test {
            viewModel.confirmPreset(5)
            assertTrue(awaitItem() is SessionDurationPickerViewModel.Event.LaunchTarget)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmCustom_emitsLaunchAfterDelayWhenMindfulDelayOn() = runTest {
        sessionTimerRepository.updateMindfulLaunchDelay(true)
        viewModel.onCustomMinutesChange("17")
        viewModel.events.test {
            viewModel.confirmCustom()
            assertTrue(awaitItem() is SessionDurationPickerViewModel.Event.LaunchAfterDelay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun perSessionMode_doesNotMutateGlobalDefault() {
        // The picker only mutates its own state; global preferences are owned
        // elsewhere. Since SessionTimerCoordinator stores the snapshot, a
        // change to selectedMode after startSession does not retroactively
        // alter the active session's mode.
        viewModel.confirmPreset(5)
        val recorded = coordinator.active.value!!.expiryMode
        viewModel.onSelectExpiryMode(SessionExpiryMode.MINDFUL_EXTENSION_DELAY)
        assertEquals(recorded, coordinator.active.value!!.expiryMode)
    }
}
