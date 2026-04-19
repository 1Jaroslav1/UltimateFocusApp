/**
 * Unit tests for HomeViewModel. Verifies that favorite apps are correctly
 * exposed, that launching and toggling favorites delegate to the repository,
 * that preferences flow is connected, and that the bottom-bar quick-access
 * shortcut StateFlows resolve defaults / overrides / uninstalled fallback.
 * Uses test doubles only — no mocking.
 * Covers: ULT-6, ULT-13, ULT-14
 */
package com.ultimatefocus.app.ui.home

import android.content.ComponentName
import app.cash.turbine.test
import com.ultimatefocus.app.data.repository.FakeAppRepository
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.ShortcutSlot
import com.ultimatefocus.app.domain.model.ShortcutTarget
import com.ultimatefocus.app.domain.model.UserPreferences
import com.ultimatefocus.app.domain.repository.FakeBlockedAppRepository
import com.ultimatefocus.app.domain.repository.FakeUserPreferencesSource
import com.ultimatefocus.app.domain.service.FakeSystemActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
            systemActions = FakeSystemActions(),
            blockedAppRepository = FakeBlockedAppRepository(),
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

    // ── ULT-13: Shortcut defaults + tap-to-launch ────────────────────────────

    @Test
    fun `leftShortcut emits default Phone when no override is set`() = runTest {
        val phone = makeApp("Phone", isFavorite = false)
        fakeRepository.defaultShortcuts[ShortcutSlot.LEFT] = phone

        viewModel.leftShortcut.test {
            assertEquals(phone, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rightShortcut emits default Camera when no override is set`() = runTest {
        val camera = makeApp("Camera", isFavorite = false)
        fakeRepository.defaultShortcuts[ShortcutSlot.RIGHT] = camera

        viewModel.rightShortcut.test {
            assertEquals(camera, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `leftShortcut emits override target when preference is set`() = runTest {
        val maps = makeApp("Maps", isFavorite = false)
        fakeRepository.setApps(listOf(maps))
        fakePreferences.setPreferences(
            UserPreferences(
                leftShortcut = ShortcutTarget(
                    packageName = maps.packageName,
                    componentName = maps.componentName.flattenToString(),
                    label = maps.label,
                ),
            ),
        )

        viewModel.leftShortcut.test {
            assertEquals(maps, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `launchShortcut LEFT delegates to repository with resolved left app`() = runTest {
        val phone = makeApp("Phone", isFavorite = false)
        fakeRepository.defaultShortcuts[ShortcutSlot.LEFT] = phone
        // Prime the StateFlow so launchShortcut sees a resolved value.
        viewModel.leftShortcut.first { it != null }

        viewModel.launchShortcut(ShortcutSlot.LEFT)

        assertEquals(phone, fakeRepository.launchedApp)
    }

    @Test
    fun `launchShortcut RIGHT delegates to repository with resolved right app`() = runTest {
        val camera = makeApp("Camera", isFavorite = false)
        fakeRepository.defaultShortcuts[ShortcutSlot.RIGHT] = camera
        viewModel.rightShortcut.first { it != null }

        viewModel.launchShortcut(ShortcutSlot.RIGHT)

        assertEquals(camera, fakeRepository.launchedApp)
    }

    // ── ULT-13: Border cases ─────────────────────────────────────────────────

    @Test
    fun `launchShortcut is a no-op when resolved target is null`() = runTest {
        fakeRepository.defaultShortcuts[ShortcutSlot.LEFT] = null

        viewModel.launchShortcut(ShortcutSlot.LEFT)

        assertNull(fakeRepository.launchedApp)
    }

    @Test
    fun `leftShortcut falls back to default when override points to uninstalled package`() =
        runTest {
            val phone = makeApp("Phone", isFavorite = false)
            fakeRepository.defaultShortcuts[ShortcutSlot.LEFT] = phone
            fakeRepository.setApps(emptyList()) // nothing installed matches override
            fakePreferences.setPreferences(
                UserPreferences(
                    leftShortcut = ShortcutTarget(
                        packageName = "com.missing.app",
                        componentName = "com.missing.app/Main",
                        label = "Missing",
                    ),
                ),
            )

            viewModel.leftShortcut.test {
                assertEquals(phone, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `installedApps flow exposes the repository's installed list`() = runTest {
        val apps = listOf(makeApp("Maps", isFavorite = false), makeApp("Notes", isFavorite = false))
        fakeRepository.setApps(apps)

        viewModel.installedApps.test {
            assertEquals(apps, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
