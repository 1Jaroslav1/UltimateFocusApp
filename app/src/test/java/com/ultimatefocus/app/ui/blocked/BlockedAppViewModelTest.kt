/**
 * Unit tests for BlockedAppViewModel. Verifies that remainingMs decreases
 * monotonically with FakeClock advancement and that shouldFinish flips to
 * true once the block expires.
 * Covers: ULT-21
 */
package com.ultimatefocus.app.ui.blocked

import androidx.lifecycle.SavedStateHandle
import com.ultimatefocus.app.domain.service.FakeClock
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity.Companion.EXTRA_APP_LABEL
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity.Companion.EXTRA_BLOCKED_UNTIL_MS
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity.Companion.EXTRA_DURATION_LABEL
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity.Companion.EXTRA_PACKAGE_NAME
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
class BlockedAppViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun makeViewModel(
        blockedUntilEpochMs: Long,
        clock: FakeClock,
    ): BlockedAppViewModel {
        val saved = SavedStateHandle(
            mapOf(
                EXTRA_PACKAGE_NAME to "com.test.app",
                EXTRA_APP_LABEL to "TestApp",
                EXTRA_BLOCKED_UNTIL_MS to blockedUntilEpochMs,
                EXTRA_DURATION_LABEL to "1h",
            ),
        )
        return BlockedAppViewModel(saved, clock)
    }

    @Test
    fun uiState_reflectsExtrasOnInit() = runTest {
        val clock = FakeClock(current = 1_000L)
        val vm = makeViewModel(blockedUntilEpochMs = 1_000L + 60_000L, clock = clock)

        val state = vm.uiState.value
        assertEquals("com.test.app", state.packageName)
        assertEquals("TestApp", state.appLabel)
        assertEquals(60_000L, state.remainingMs)
        assertEquals("1h", state.durationLabel)
        assertFalse(state.shouldFinish)
    }

    @Test
    fun remainingMs_decreasesAfterClockAdvances_onRefresh() = runTest {
        val clock = FakeClock(current = 1_000L)
        val vm = makeViewModel(blockedUntilEpochMs = 1_000L + 60_000L, clock = clock)

        clock.current += 10_000L
        vm.refresh()
        assertEquals(50_000L, vm.uiState.value.remainingMs)
    }

    @Test
    fun shouldFinish_flipsToTrue_whenClockPassesExpiry() = runTest {
        val clock = FakeClock(current = 1_000L)
        val vm = makeViewModel(blockedUntilEpochMs = 1_000L + 60_000L, clock = clock)

        clock.current += 60_000L
        vm.refresh()
        assertTrue(vm.uiState.value.shouldFinish)
        assertEquals(0L, vm.uiState.value.remainingMs)
    }

    @Test
    fun shouldFinish_isTrue_whenBlockedUntilIsAlreadyPastAtInit() = runTest {
        val clock = FakeClock(current = 1_000_000L)
        val vm = makeViewModel(blockedUntilEpochMs = 1L, clock = clock)

        assertTrue(vm.uiState.value.shouldFinish)
        assertEquals(0L, vm.uiState.value.remainingMs)
    }

    @Test
    fun remainingMs_isZeroDefensively_whenUntilEqualsNow() = runTest {
        val clock = FakeClock(current = 1_000L)
        val vm = makeViewModel(blockedUntilEpochMs = 1_000L, clock = clock)

        assertEquals(0L, vm.uiState.value.remainingMs)
        assertTrue(vm.uiState.value.shouldFinish)
    }
}
