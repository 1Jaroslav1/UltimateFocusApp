/**
 * Unit tests for BlockingSchedulesListViewModel. Verifies that the row-level
 * isCurrentlyActive flag is computed against the injected Clock + Zone, and
 * that the seeded list propagates through the ViewModel state.
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.ui.blocking

import app.cash.turbine.test
import com.ultimatefocus.app.domain.model.BlockingSchedule
import com.ultimatefocus.app.domain.repository.FakeBlockingScheduleRepository
import com.ultimatefocus.app.domain.service.FakeClock
import com.ultimatefocus.app.domain.service.FixedZoneIdProvider
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
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class BlockingSchedulesListViewModelTest {

    private lateinit var clock: FakeClock
    private lateinit var repo: FakeBlockingScheduleRepository

    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")
    private val zoneProvider = FixedZoneIdProvider(zone)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // 2026-04-22 12:00 — Wednesday mid-day (mid 9-5 window).
        val nowMs = ZonedDateTime.of(2026, 4, 22, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        clock = FakeClock(current = nowMs)
        repo = FakeBlockingScheduleRepository(clock, zoneProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_emitsEmpty_whenRepositoryIsEmpty() = runTest {
        val vm = BlockingSchedulesListViewModel(repo, clock, zoneProvider)
        vm.uiState.test {
            assertTrue(awaitItem().schedules.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_marksRowAsCurrentlyActive_whenWithinWindow() = runTest {
        repo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L, name = "Focus",
                    start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
                    days = setOf(DayOfWeek.WEDNESDAY),
                    packageNames = setOf("com.a"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        val vm = BlockingSchedulesListViewModel(repo, clock, zoneProvider)
        vm.uiState.test {
            // Initial empty emission then loaded list. Skip until non-empty.
            var state = awaitItem()
            while (state.schedules.isEmpty()) state = awaitItem()
            assertEquals(1, state.schedules.size)
            assertTrue(state.schedules[0].isCurrentlyActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_marksRowAsInactive_whenOutsideWindow() = runTest {
        // Move clock to 20:00 (after the 9-5 window).
        clock.current = ZonedDateTime.of(2026, 4, 22, 20, 0, 0, 0, zone)
            .toInstant().toEpochMilli()
        repo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L, name = "Focus",
                    start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
                    days = setOf(DayOfWeek.WEDNESDAY),
                    packageNames = setOf("com.a"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        val vm = BlockingSchedulesListViewModel(repo, clock, zoneProvider)
        vm.uiState.test {
            var state = awaitItem()
            while (state.schedules.isEmpty()) state = awaitItem()
            assertFalse(state.schedules[0].isCurrentlyActive)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
