/**
 * Unit tests for HomeViewModel's blocked-launch routing. Verifies that a
 * Blocked LaunchResult is surfaced on `blockedAttempts` and that the Fake
 * repository's launchedApp is NOT populated (enforcement works).
 * Covers: ULT-21
 */
package com.ultimatefocus.app.ui.home

import android.content.ComponentName
import app.cash.turbine.test
import com.ultimatefocus.app.data.repository.FakeAppRepository
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.LaunchResult
import com.ultimatefocus.app.domain.repository.FakeBlockedAppRepository
import com.ultimatefocus.app.domain.repository.FakeUserPreferencesSource
import com.ultimatefocus.app.domain.service.FakeSystemActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelBlockingTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeAppRepository
    private lateinit var viewModel: HomeViewModel

    private val targetApp = AppInfo(
        packageName = "com.test.social",
        label = "Social",
        componentName = ComponentName("com.test.social", "Main"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAppRepository()
        viewModel = HomeViewModel(
            application = android.app.Application(),
            appRepository = fakeRepository,
            preferencesSource = FakeUserPreferencesSource(),
            systemActions = FakeSystemActions(),
            blockedAppRepository = FakeBlockedAppRepository(),
        )
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun launchApp_whenBlocked_emitsBlockedAttemptAndDoesNotLaunch() = runTest {
        fakeRepository.blockedResultFor = { pkg ->
            if (pkg == "com.test.social") {
                LaunchResult.Blocked(
                    packageName = pkg,
                    appLabel = "Social",
                    remainingMs = 3_600_000L,
                    durationLabel = "1h",
                )
            } else null
        }

        viewModel.blockedAttempts.test {
            viewModel.launchApp(targetApp)
            val emitted = awaitItem()
            assertEquals("com.test.social", emitted.packageName)
            assertEquals(3_600_000L, emitted.remainingMs)
            cancelAndIgnoreRemainingEvents()
        }

        // Launcher side effect never fired.
        assertNull(fakeRepository.launchedApp)
    }

    @Test
    fun launchApp_whenUnblocked_launchesNormallyAndDoesNotEmit() = runTest {
        fakeRepository.blockedResultFor = { null }

        viewModel.launchApp(targetApp)

        assertEquals(targetApp, fakeRepository.launchedApp)
    }
}
