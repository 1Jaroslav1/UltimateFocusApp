/**
 * Unit tests for SessionTimerRepositoryImpl global-settings surface.
 * Backed by FakeUserPreferencesSource (in-memory). Per-app behavior is
 * covered separately when ULT-77 lands the Room-backed surface.
 *
 * Covers: ULT-76
 */
package com.ultimatefocus.app.data.repository

import app.cash.turbine.test
import com.ultimatefocus.app.domain.model.InAppReminderSettings
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.repository.FakeUserPreferencesSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionTimerRepositoryImplTest {

    private lateinit var prefs: FakeUserPreferencesSource
    private lateinit var repo: SessionTimerRepositoryImpl

    @Before
    fun setUp() {
        prefs = FakeUserPreferencesSource()
        repo = SessionTimerRepositoryImpl(prefs)
    }

    @Test
    fun `defaults match documented values`() = runTest {
        repo.observeSettings().test {
            assertEquals(InAppReminderSettings(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateMasterEnabled propagates to settings flow`() = runTest {
        repo.observeSettings().test {
            awaitItem()
            repo.updateMasterEnabled(true)
            assertTrue(awaitItem().masterEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updatePrecisionMode flips precisionMode`() = runTest {
        repo.observeSettings().test {
            assertFalse(awaitItem().precisionMode)
            repo.updatePrecisionMode(true)
            assertTrue(awaitItem().precisionMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateMindfulLaunchDelay flips mindfulLaunchDelay`() = runTest {
        repo.observeSettings().test {
            assertFalse(awaitItem().mindfulLaunchDelay)
            repo.updateMindfulLaunchDelay(true)
            assertTrue(awaitItem().mindfulLaunchDelay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `each expiry mode round-trips`() = runTest {
        for (mode in SessionExpiryMode.entries) {
            repo.updateExpiryMode(mode)
            repo.observeSettings().test {
                assertEquals(mode, awaitItem().expiryMode)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `per-app stub returns empty until ULT-77 wires Room`() = runTest {
        repo.observeEnabledPackages().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(repo.isReminderActiveFor("com.example.any"))
        // setEnabled is a no-op in the global-only build.
        repo.setEnabled("com.example.any", true)
        assertFalse(repo.isReminderActiveFor("com.example.any"))
    }
}
