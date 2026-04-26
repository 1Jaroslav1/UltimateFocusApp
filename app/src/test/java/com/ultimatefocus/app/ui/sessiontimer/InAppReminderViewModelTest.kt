/**
 * Unit tests for InAppReminderViewModel — covers the global controls
 * (master, precision, mindful delay, expiry mode) and the per-app list
 * surface (search filter, toggle persistence, master/per-app combination).
 *
 * Wires the real SessionTimerRepositoryImpl with FakeUserPreferencesSource
 * + FakeSessionTimerAppDao + FakeClock so the ViewModel ↔ Repository ↔
 * persistence seam is integration-tested end-to-end inside one JVM test.
 *
 * Covers: ULT-76, ULT-77
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
    private lateinit var fakeDao: FakeSessionTimerAppDao
    private lateinit var fakeApps: FakeAppRepository
    private lateinit var viewModel: InAppReminderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrefs = FakeUserPreferencesSource()
        fakeDao = FakeSessionTimerAppDao()
        fakeApps = FakeAppRepository()
        val repo = SessionTimerRepositoryImpl(fakePrefs, fakeDao, FakeClock(current = 1L))
        viewModel = InAppReminderViewModel(repo, fakeApps)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun appInfo(pkg: String, label: String) = AppInfo(
        packageName = pkg,
        label = label,
        componentName = ComponentName(pkg, "$pkg.Main"),
    )

    // ── Global settings ──────────────────────────────────────────────────────

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
    fun `setMasterEnabled true persists through repository`() = runTest {
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
            awaitItem()
            viewModel.togglePrecisionMode()
            assertTrue(awaitItem().precisionMode)
            viewModel.togglePrecisionMode()
            assertFalse(awaitItem().precisionMode)
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

    // ── Per-app list ─────────────────────────────────────────────────────────

    @Test
    fun `uiState surfaces installed apps sorted by label`() = runTest {
        fakeApps.setApps(
            listOf(
                appInfo("com.zeta", "Zeta App"),
                appInfo("com.alpha", "Alpha App"),
                appInfo("com.middle", "Middle App"),
            ),
        )
        viewModel.uiState.test {
            // initial empty before combine completes; pull until apps appear
            var snap = awaitItem()
            while (snap.totalAppCount == 0) snap = awaitItem()
            assertEquals(
                listOf("com.alpha", "com.middle", "com.zeta"),
                snap.visibleApps.map { it.packageName },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setQuery filters list case-insensitively`() = runTest {
        fakeApps.setApps(
            listOf(
                appInfo("com.alpha", "Alpha App"),
                appInfo("com.bravo", "Bravo App"),
            ),
        )
        viewModel.uiState.test {
            var snap = awaitItem()
            while (snap.totalAppCount == 0) snap = awaitItem()
            viewModel.setQuery("BRA")
            snap = awaitItem()
            // The query updates may emit twice (query change, list filter); drain.
            while (snap.query != "BRA") snap = awaitItem()
            assertEquals(listOf("com.bravo"), snap.visibleApps.map { it.packageName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setAppEnabled persists per-app opt-in`() = runTest {
        fakeApps.setApps(listOf(appInfo("com.alpha", "Alpha App")))
        viewModel.setAppEnabled("com.alpha", true)
        assertTrue(fakeDao.isEnabled("com.alpha"))
        viewModel.setAppEnabled("com.alpha", false)
        assertFalse(fakeDao.isEnabled("com.alpha"))
    }

    @Test
    fun `uiState reflects toggled state on visibleApps`() = runTest {
        fakeApps.setApps(listOf(appInfo("com.alpha", "Alpha App")))
        viewModel.uiState.test {
            var snap = awaitItem()
            while (snap.totalAppCount == 0) snap = awaitItem()
            assertFalse(snap.visibleApps.first().enabled)

            viewModel.setAppEnabled("com.alpha", true)
            snap = awaitItem()
            while (snap.visibleApps.firstOrNull()?.enabled != true) snap = awaitItem()
            assertTrue(snap.visibleApps.first().enabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setQuery with no matches keeps totalAppCount but visibleApps empty`() = runTest {
        fakeApps.setApps(
            listOf(
                appInfo("com.alpha", "Alpha App"),
                appInfo("com.bravo", "Bravo App"),
            ),
        )
        viewModel.uiState.test {
            var snap = awaitItem()
            while (snap.totalAppCount == 0) snap = awaitItem()
            viewModel.setQuery("nothing matches this")
            snap = awaitItem()
            while (snap.query != "nothing matches this") snap = awaitItem()
            assertEquals(2, snap.totalAppCount)
            assertTrue(snap.visibleApps.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
