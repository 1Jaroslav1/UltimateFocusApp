/**
 * Unit tests for HomeViewModel. Verifies that favorite apps are correctly
 * exposed, that launching and toggling favorites delegate to the repository,
 * and that preferences flow is connected. Uses test doubles only — no mocking.
 * Covers: ULT-6
 */
package com.ultimatefocus.app.ui.home

import android.content.ComponentName
import app.cash.turbine.test
import com.ultimatefocus.app.data.repository.FakeAppRepository
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.repository.FakeUserPreferencesSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeAppRepository
    private lateinit var fakePreferences: FakeUserPreferencesSource
    private lateinit var viewModel: HomeViewModel

    private fun makeApp(name: String, isFavorite: Boolean = true) = AppInfo(
        packageName = "com.test.${name.lowercase()}",
        label = name,
        componentName = ComponentName.unflattenFromString("com.test.${name.lowercase()}/MainActivity")!!,
        isFavorite = isFavorite,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAppRepository()
        fakePreferences = FakeUserPreferencesSource()
        viewModel = HomeViewModel(
            application = android.app.Application(),
            appRepository = fakeRepository,
            preferencesSource = fakePreferences,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun `favoriteApps emits empty list when no favorites are set`() = runTest {
        fakeRepository.setFavorites(emptyList())
        viewModel.favoriteApps.test {
            assertEquals(emptyList<AppInfo>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favoriteApps emits favorites in the order provided`() = runTest {
        val favorites = listOf(makeApp("Calendar"), makeApp("Phone"))
        fakeRepository.setFavorites(favorites)
        viewModel.favoriteApps.test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertEquals("Calendar", items[0].label)
            assertEquals("Phone", items[1].label)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favoriteApps updates when repository emits new list`() = runTest {
        fakeRepository.setFavorites(emptyList())
        viewModel.favoriteApps.test {
            awaitItem() // empty initial
            val updated = listOf(makeApp("Maps"))
            fakeRepository.setFavorites(updated)
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Maps", items[0].label)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `launchApp delegates to repository`() = runTest {
        val app = makeApp("Maps")
        viewModel.launchApp(app)
        assertEquals(app, fakeRepository.launchedApp)
    }

    @Test
    fun `toggleFavorite adds app when not yet pinned`() = runTest {
        val app = makeApp("Notes", isFavorite = false)
        fakeRepository.setFavorites(emptyList())
        viewModel.toggleFavorite(app)
        viewModel.favoriteApps.test {
            assertTrue(awaitItem().any { it.packageName == app.packageName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleFavorite removes app when already pinned`() = runTest {
        val app = makeApp("Slack")
        fakeRepository.setFavorites(listOf(app))
        viewModel.toggleFavorite(app)
        viewModel.favoriteApps.test {
            assertTrue(awaitItem().none { it.packageName == app.packageName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun `uiState has a non-null currentTime on creation`() {
        assertNotNull(viewModel.uiState.value.currentTime)
    }

    @Test
    fun `favoriteApps with single item emits list of size one`() = runTest {
        fakeRepository.setFavorites(listOf(makeApp("Clock")))
        viewModel.favoriteApps.test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `preferences emits default values initially`() = runTest {
        viewModel.preferences.test {
            val prefs = awaitItem()
            assertTrue(prefs.isDarkTheme)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
