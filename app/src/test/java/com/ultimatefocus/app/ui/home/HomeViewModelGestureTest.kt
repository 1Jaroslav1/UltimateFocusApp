/**
 * Unit tests for the gesture-driven actions exposed by HomeViewModel:
 * swipe-up → web search, swipe-down → notification shade, double-tap → lock.
 * Uses FakeSystemActions + FakeUserPreferencesSource — no Android context.
 * Covers: ULT-16, ULT-17, ULT-18
 */
package com.ultimatefocus.app.ui.home

import com.ultimatefocus.app.data.repository.FakeAppRepository
import com.ultimatefocus.app.domain.model.UserPreferences
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
class HomeViewModelGestureTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeAppRepository
    private lateinit var fakePreferences: FakeUserPreferencesSource
    private lateinit var fakeSystemActions: FakeSystemActions
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAppRepository()
        fakePreferences = FakeUserPreferencesSource()
        fakeSystemActions = FakeSystemActions()
        viewModel = HomeViewModel(
            application = android.app.Application(),
            appRepository = fakeRepository,
            preferencesSource = fakePreferences,
            systemActions = fakeSystemActions,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── ULT-16 ───────────────────────────────────────────────────────────────

    @Test
    fun onSwipeUp_withSystemDefault_opensWebSearchWithNullPackage() = runTest {
        fakePreferences.setPreferences(UserPreferences(searchProviderPackage = null))

        viewModel.onSwipeUp()

        assertEquals(1, fakeSystemActions.searchOpenCount)
        assertNull(fakeSystemActions.lastOpenedSearchPackage)
    }

    @Test
    fun onSwipeUp_withProviderSelected_opensWebSearchWithThatPackage() = runTest {
        fakePreferences.setPreferences(
            UserPreferences(searchProviderPackage = "com.duckduckgo.mobile.android"),
        )

        viewModel.onSwipeUp()

        assertEquals(1, fakeSystemActions.searchOpenCount)
        assertEquals("com.duckduckgo.mobile.android", fakeSystemActions.lastOpenedSearchPackage)
    }

    @Test
    fun onSwipeUp_multipleTimes_invokesSearchEachTime() = runTest {
        viewModel.onSwipeUp()
        viewModel.onSwipeUp()
        viewModel.onSwipeUp()

        assertEquals(3, fakeSystemActions.searchOpenCount)
    }

    // ── ULT-17 ───────────────────────────────────────────────────────────────

    @Test
    fun onSwipeDown_opensNotificationShade() = runTest {
        viewModel.onSwipeDown()

        assertEquals(1, fakeSystemActions.notificationShadeOpenCount)
    }

    @Test
    fun onSwipeDown_isIndependentFromOnSwipeUp() = runTest {
        viewModel.onSwipeDown()

        assertEquals(0, fakeSystemActions.searchOpenCount)
    }

    // ── ULT-18 ───────────────────────────────────────────────────────────────

    @Test
    fun onDoubleTap_whenDisabled_doesNothing() = runTest {
        fakePreferences.setPreferences(UserPreferences(doubleTapToLockEnabled = false))
        fakeSystemActions.setAdminGranted(true)

        viewModel.onDoubleTap()

        assertEquals(0, fakeSystemActions.lockScreenCalls)
        assertEquals(0, fakeSystemActions.requestLockPermissionCalls)
    }

    @Test
    fun onDoubleTap_whenEnabledAndGranted_locksScreen() = runTest {
        fakePreferences.setPreferences(UserPreferences(doubleTapToLockEnabled = true))
        fakeSystemActions.setAdminGranted(true)

        viewModel.onDoubleTap()

        assertEquals(1, fakeSystemActions.lockScreenCalls)
        assertEquals(0, fakeSystemActions.requestLockPermissionCalls)
    }

    @Test
    fun onDoubleTap_whenEnabledButNotGranted_requestsPermission() = runTest {
        fakePreferences.setPreferences(UserPreferences(doubleTapToLockEnabled = true))
        fakeSystemActions.setAdminGranted(false)

        viewModel.onDoubleTap()

        // Still attempts the lock so we can react to MissingPermission,
        // and triggers the admin prompt so the user can grant access next time.
        assertEquals(1, fakeSystemActions.lockScreenCalls)
        assertEquals(1, fakeSystemActions.requestLockPermissionCalls)
    }
}
