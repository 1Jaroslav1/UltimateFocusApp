/**
 * Unit tests for SettingsViewModel. Verifies that preference toggles correctly
 * delegate updates to the UserPreferencesSource, including the autoKeyboard toggle.
 * Uses UnconfinedTestDispatcher for immediate StateFlow propagation.
 * Covers: ULT-8
 */
package com.minimalist.launcher.ui.settings

import app.cash.turbine.test
import com.minimalist.launcher.domain.model.UserPreferences
import com.minimalist.launcher.domain.repository.FakeUserPreferencesSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakePreferences: FakeUserPreferencesSource
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePreferences = FakeUserPreferencesSource()
        viewModel = SettingsViewModel(fakePreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun `preferences emits defaults initially`() = runTest {
        viewModel.preferences.test {
            val prefs = awaitItem()
            assertTrue(prefs.isDarkTheme)
            assertTrue(prefs.autoKeyboard)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleDarkTheme flips isDarkTheme from true to false`() = runTest {
        fakePreferences.setPreferences(UserPreferences(isDarkTheme = true))
        // Let the stateIn collect the updated fake value
        viewModel.preferences.test {
            awaitItem() // current value
            viewModel.toggleDarkTheme()
            assertFalse(awaitItem().isDarkTheme)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleDarkTheme flips isDarkTheme from false to true`() = runTest {
        fakePreferences.setPreferences(UserPreferences(isDarkTheme = false))
        viewModel.preferences.test {
            awaitItem()
            viewModel.toggleDarkTheme()
            assertTrue(awaitItem().isDarkTheme)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleAutoKeyboard disables autoKeyboard when currently enabled`() = runTest {
        fakePreferences.setPreferences(UserPreferences(autoKeyboard = true))
        viewModel.preferences.test {
            awaitItem()
            viewModel.toggleAutoKeyboard()
            assertFalse(awaitItem().autoKeyboard)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleAutoKeyboard enables autoKeyboard when currently disabled`() = runTest {
        fakePreferences.setPreferences(UserPreferences(autoKeyboard = false))
        viewModel.preferences.test {
            awaitItem()
            viewModel.toggleAutoKeyboard()
            assertTrue(awaitItem().autoKeyboard)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun `toggling autoKeyboard twice restores original value`() = runTest {
        fakePreferences.setPreferences(UserPreferences(autoKeyboard = true))
        viewModel.preferences.test {
            awaitItem()
            viewModel.toggleAutoKeyboard()
            assertFalse(awaitItem().autoKeyboard)
            viewModel.toggleAutoKeyboard()
            assertTrue(awaitItem().autoKeyboard)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggling darkTheme does not affect autoKeyboard`() = runTest {
        fakePreferences.setPreferences(UserPreferences(isDarkTheme = true, autoKeyboard = true))
        viewModel.preferences.test {
            awaitItem()
            viewModel.toggleDarkTheme()
            val prefs = awaitItem()
            assertFalse(prefs.isDarkTheme)
            assertTrue(prefs.autoKeyboard)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
