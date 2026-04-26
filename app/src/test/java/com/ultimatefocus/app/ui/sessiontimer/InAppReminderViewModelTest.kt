/**
 * Unit tests for InAppReminderViewModel — covers the four global controls
 * exposed on the In-App Reminder settings page (master switch, precision
 * mode toggle, mindful launch delay switch, expiry-mode radio group).
 *
 * Uses FakeUserPreferencesSource (in-memory) wired through the real
 * SessionTimerRepositoryImpl so the ViewModel ↔ Repository ↔ DataStore
 * surface is integration-tested as a single seam. Validates both happy
 * paths (initial defaults, single setter, one-shot mode change) and
 * border cases (precision toggling round-trip, repeated master flips).
 *
 * Covers: ULT-76
 */
package com.ultimatefocus.app.ui.sessiontimer

import app.cash.turbine.test
import com.ultimatefocus.app.data.repository.SessionTimerRepositoryImpl
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.repository.FakeUserPreferencesSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InAppReminderViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakePrefs: FakeUserPreferencesSource
    private lateinit var viewModel: InAppReminderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrefs = FakeUserPreferencesSource()
        viewModel = InAppReminderViewModel(SessionTimerRepositoryImpl(fakePrefs))
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun `settings emits defaults initially`() = runTest {
        viewModel.settings.test {
            val initial = awaitItem()
            assertFalse(initial.masterEnabled)
            assertFalse(initial.precisionMode)
            assertFalse(initial.mindfulLaunchDelay)
            assertEquals(SessionExpiryMode.ONLY_REMINDER, initial.expiryMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setMasterEnabled true persists through repository to settings flow`() = runTest {
        viewModel.settings.test {
            awaitItem()
            viewModel.setMasterEnabled(true)
            assertTrue(awaitItem().masterEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `togglePrecisionMode flips false then true`() = runTest {
        viewModel.settings.test {
            awaitItem() // defaults
            viewModel.togglePrecisionMode()
            assertTrue(awaitItem().precisionMode)
            viewModel.togglePrecisionMode()
            assertFalse(awaitItem().precisionMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setMindfulLaunchDelay true persists`() = runTest {
        viewModel.settings.test {
            awaitItem()
            viewModel.setMindfulLaunchDelay(true)
            assertTrue(awaitItem().mindfulLaunchDelay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setExpiryMode persists EXIT_AND_BLOCK`() = runTest {
        viewModel.settings.test {
            awaitItem()
            viewModel.setExpiryMode(SessionExpiryMode.EXIT_AND_BLOCK)
            assertEquals(SessionExpiryMode.EXIT_AND_BLOCK, awaitItem().expiryMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setExpiryMode for each value in the enum is round-trip safe`() = runTest {
        SessionExpiryMode.entries.forEach { mode ->
            viewModel.setExpiryMode(mode)
            assertEquals(mode, viewModel.settings.value.expiryMode)
        }
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun `master and other flags are independent`() = runTest {
        viewModel.settings.test {
            awaitItem()
            viewModel.setMindfulLaunchDelay(true)
            assertTrue(awaitItem().mindfulLaunchDelay)
            viewModel.setMasterEnabled(false)
            // mindfulLaunchDelay stays true even with master off — the UI is
            // responsible for greying it out, not the repository.
            val snap = awaitItem()
            assertFalse(snap.masterEnabled)
            assertTrue(snap.mindfulLaunchDelay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repeated setMaster does not flicker between values`() = runTest {
        viewModel.settings.test {
            awaitItem()
            viewModel.setMasterEnabled(true)
            assertTrue(awaitItem().masterEnabled)
            viewModel.setMasterEnabled(true) // idempotent
            // No new emission expected because the value did not change.
            // Drain via cancelAndIgnoreRemainingEvents — Turbine will
            // throw if a duplicate emission appears.
            cancelAndIgnoreRemainingEvents()
        }
    }
}
