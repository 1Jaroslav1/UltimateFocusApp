/**
 * Unit tests for the gesture-navigation preference surfaces on SettingsViewModel:
 * search-provider selection (ULT-16) and double-tap-to-lock toggle (ULT-18).
 * Covers: ULT-16, ULT-18
 */
package com.ultimatefocus.app.ui.settings

import app.cash.turbine.test
import com.ultimatefocus.app.domain.model.UserPreferences
import com.ultimatefocus.app.domain.repository.FakeUserPreferencesSource
import com.ultimatefocus.app.domain.service.FakeSystemActions
import com.ultimatefocus.app.domain.service.WebSearchProvider
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
class SettingsViewModelGestureTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakePreferences: FakeUserPreferencesSource
    private lateinit var fakeSystemActions: FakeSystemActions
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePreferences = FakeUserPreferencesSource()
        fakeSystemActions = FakeSystemActions(
            providers = listOf(
                WebSearchProvider("com.duckduckgo.mobile.android", "DuckDuckGo"),
                WebSearchProvider("com.android.chrome", "Chrome"),
            ),
        )
        viewModel = SettingsViewModel(fakePreferences, fakeSystemActions)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── ULT-16: search provider ──────────────────────────────────────────────

    @Test
    fun `availableSearchProviders exposes list from SystemActions`() {
        val providers = viewModel.availableSearchProviders()

        assertEquals(2, providers.size)
        assertEquals("DuckDuckGo", providers[0].label)
    }

    @Test
    fun `updateSearchProviderPackage persists selected package`() = runTest {
        viewModel.preferences.test {
            awaitItem()
            viewModel.updateSearchProviderPackage("com.duckduckgo.mobile.android")
            assertEquals("com.duckduckgo.mobile.android", awaitItem().searchProviderPackage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateSearchProviderPackage with null clears selection back to system default`() = runTest {
        fakePreferences.setPreferences(
            UserPreferences(searchProviderPackage = "com.android.chrome"),
        )
        viewModel.preferences.test {
            awaitItem() // current value
            viewModel.updateSearchProviderPackage(null)
            assertNull(awaitItem().searchProviderPackage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── ULT-18: two-finger-double-tap-to-lock ────────────────────────────────

    @Test
    fun `setTwoFingerDoubleTapToLock enables the preference`() = runTest {
        viewModel.preferences.test {
            awaitItem()
            viewModel.setTwoFingerDoubleTapToLock(true)
            assertEquals(true, awaitItem().twoFingerDoubleTapToLockEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setTwoFingerDoubleTapToLock to true when admin not granted triggers permission prompt`() {
        fakeSystemActions.setAdminGranted(false)

        viewModel.setTwoFingerDoubleTapToLock(true)

        assertEquals(1, fakeSystemActions.requestLockPermissionCalls)
    }

    @Test
    fun `setTwoFingerDoubleTapToLock to true when admin already granted does not reprompt`() {
        fakeSystemActions.setAdminGranted(true)

        viewModel.setTwoFingerDoubleTapToLock(true)

        assertEquals(0, fakeSystemActions.requestLockPermissionCalls)
    }

    @Test
    fun `setTwoFingerDoubleTapToLock to false disables the preference and does not prompt`() = runTest {
        fakePreferences.setPreferences(UserPreferences(twoFingerDoubleTapToLockEnabled = true))
        fakeSystemActions.setAdminGranted(false)

        viewModel.preferences.test {
            awaitItem()
            viewModel.setTwoFingerDoubleTapToLock(false)
            assertEquals(false, awaitItem().twoFingerDoubleTapToLockEnabled)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, fakeSystemActions.requestLockPermissionCalls)
    }
}
