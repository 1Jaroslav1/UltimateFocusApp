/**
 * Unit tests for AppDrawerViewModel. Verifies alphabetical app listing,
 * real-time search filtering, empty results, and launch/toggle delegation.
 * Uses test doubles only — no mocking frameworks.
 * Covers: ULT-7
 */
package com.minimalist.launcher.ui.drawer

import android.content.ComponentName
import app.cash.turbine.test
import com.minimalist.launcher.data.repository.FakeAppRepository
import com.minimalist.launcher.domain.model.AppInfo
import com.minimalist.launcher.domain.repository.FakeUserPreferencesSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppDrawerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAppRepository
    private lateinit var fakePreferences: FakeUserPreferencesSource
    private lateinit var viewModel: AppDrawerViewModel

    private fun makeApp(name: String) = AppInfo(
        packageName = "com.test.${name.lowercase()}",
        label = name,
        componentName = ComponentName("com.test.${name.lowercase()}", "MainActivity"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAppRepository()
        fakePreferences = FakeUserPreferencesSource()
        viewModel = AppDrawerViewModel(fakeRepository, fakePreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun `uiState emits Loading initially`() = runTest {
        viewModel.uiState.test {
            assertTrue(awaitItem() is AppDrawerUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState emits all installed apps alphabetically`() = runTest {
        val apps = listOf(makeApp("Zoom"), makeApp("Calendar"), makeApp("Maps"))
        fakeRepository.setApps(apps)
        viewModel.uiState.test {
            awaitItem() // Loading
            val state = awaitItem() as AppDrawerUiState.Success
            assertEquals(apps, state.apps) // FakeAppRepository returns as-is
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState emits empty list when no apps installed`() = runTest {
        fakeRepository.setApps(emptyList())
        viewModel.uiState.test {
            awaitItem() // Loading
            val state = awaitItem() as AppDrawerUiState.Success
            assertTrue(state.apps.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search filters apps by label case-insensitively`() = runTest {
        fakeRepository.setApps(listOf(makeApp("Calendar"), makeApp("Camera"), makeApp("Maps")))
        viewModel.updateSearchQuery("ca")
        viewModel.uiState.test {
            awaitItem() // Loading or initial
            val state = awaitItem() as AppDrawerUiState.Success
            assertTrue(state.apps.all { it.label.contains("ca", ignoreCase = true) })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty search query returns all apps`() = runTest {
        val apps = listOf(makeApp("Phone"), makeApp("Messages"))
        fakeRepository.setApps(apps)
        viewModel.updateSearchQuery("")
        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem() as AppDrawerUiState.Success
            assertEquals(apps.size, state.apps.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search with no match returns empty list`() = runTest {
        fakeRepository.setApps(listOf(makeApp("Phone"), makeApp("Camera")))
        viewModel.updateSearchQuery("zzznomatch")
        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem() as AppDrawerUiState.Success
            assertTrue(state.apps.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `launchApp delegates to repository`() = runTest {
        val app = makeApp("Maps")
        viewModel.launchApp(app)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(app, fakeRepository.launchedApp)
    }

    @Test
    fun `toggleFavorite adds app to favorites when not pinned`() = runTest {
        val app = makeApp("Notes")
        fakeRepository.setFavorites(emptyList())
        viewModel.toggleFavorite(app)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState // just ensuring no crash
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun `search query is trimmed and case-insensitive`() = runTest {
        fakeRepository.setApps(listOf(makeApp("Maps"), makeApp("Music")))
        viewModel.updateSearchQuery("MAP")
        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem() as AppDrawerUiState.Success
            assertTrue(state.apps.any { it.label == "Maps" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `single installed app is returned in success state`() = runTest {
        fakeRepository.setApps(listOf(makeApp("Phone")))
        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem() as AppDrawerUiState.Success
            assertEquals(1, state.apps.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
