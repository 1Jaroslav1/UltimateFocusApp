/**
 * End-to-end JVM test exercising the full session-timer expiry pipeline
 * without booting Android: SessionTimerCoordinator → SessionExpiryWatcher
 * → SessionExpiryEffect handler (using fakes for SystemActions and
 * BlockedAppRepository).
 *
 * Confirms the AC for Mode 3 (EXIT_AND_BLOCK): when the timer hits zero
 * the user is sent home AND the package gets a manual block for the same
 * minutes the user just spent — the same blocking pipeline that
 * BlockedAppAccessibilityService already enforces, so the very next
 * launch attempt gets gated by the existing reminder screen.
 *
 * Also verifies Mode 2 (EXIT_APP) goHome path and Mode 4 (ONLY_REMINDER)
 * leaves both side effects untouched.
 *
 * Covers: ULT-81
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.repository.FakeBlockedAppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
class SessionExpiryEndToEndTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var clock: FakeClock
    private lateinit var coordinator: SessionTimerCoordinator
    private lateinit var watcher: SessionExpiryWatcher
    private lateinit var systemActions: FakeSystemActions
    private lateinit var blockedAppRepository: FakeBlockedAppRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        clock = FakeClock(current = 1_000L)
        coordinator = SessionTimerCoordinator(clock)
        watcher = SessionExpiryWatcher(coordinator, clock)
        systemActions = FakeSystemActions()
        blockedAppRepository = FakeBlockedAppRepository(clock)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun exitAndBlock_goesHomeAndBlocksForSessionDuration() = runTest {
        coordinator.startSession("com.alpha", "Alpha", 5, SessionExpiryMode.EXIT_AND_BLOCK)
        val job = launch {
            watcher.expiryEffects(tickIntervalMs = 1_000L).collect { effect ->
                handle(effect)
            }
        }
        clock.current = 1_000L + 5 * 60_000L + 1L
        advanceTimeBy(2_000L)
        advanceUntilIdle()

        assertEquals(1, systemActions.goHomeCalls)
        val block = blockedAppRepository.getBlock("com.alpha")
        assertNotNull(block)
        // Block runs from now → now + 5 minutes.
        assertEquals(clock.current + 5 * 60_000L, block!!.blockedUntilEpochMs)
        assertTrue(blockedAppRepository.isBlocked("com.alpha"))
        job.cancel()
    }

    @Test
    fun exitAppMode_goesHomeWithoutBlocking() = runTest {
        coordinator.startSession("com.alpha", "Alpha", 1, SessionExpiryMode.EXIT_APP)
        val job = launch {
            watcher.expiryEffects(tickIntervalMs = 1_000L).collect { effect ->
                handle(effect)
            }
        }
        clock.current = 1_000L + 60_001L
        advanceTimeBy(2_000L)
        advanceUntilIdle()

        assertEquals(1, systemActions.goHomeCalls)
        assertTrue(blockedAppRepository.blockedPackages.isEmpty())
        job.cancel()
    }

    @Test
    fun onlyReminderMode_takesNoSideEffectActions() = runTest {
        coordinator.startSession("com.alpha", "Alpha", 1, SessionExpiryMode.ONLY_REMINDER)
        val job = launch {
            watcher.expiryEffects(tickIntervalMs = 1_000L).collect { effect ->
                handle(effect)
            }
        }
        clock.current = 1_000L + 60_001L
        advanceTimeBy(2_000L)
        advanceUntilIdle()

        assertEquals(0, systemActions.goHomeCalls)
        assertTrue(blockedAppRepository.blockedPackages.isEmpty())
        job.cancel()
    }

    /** Mirrors the side-effect dispatch in BlockedAppAccessibilityService. */
    private suspend fun handle(effect: SessionExpiryEffect) {
        when (effect) {
            is SessionExpiryEffect.GoHomeOnly -> systemActions.goHome()
            is SessionExpiryEffect.GoHomeAndBlock -> {
                systemActions.goHome()
                blockedAppRepository.blockForMillis(
                    packageName = effect.packageName,
                    durationMillis = effect.blockDurationMinutes.toLong() * 60_000L,
                    label = "${effect.blockDurationMinutes}m",
                )
            }
            is SessionExpiryEffect.LaunchOverlayCountdown -> {
                /* overlay launch is Android-only, not exercised here */
            }
            is SessionExpiryEffect.ShowReminderOnly -> {
                /* toast is Android-only, not exercised here */
            }
        }
    }
}
