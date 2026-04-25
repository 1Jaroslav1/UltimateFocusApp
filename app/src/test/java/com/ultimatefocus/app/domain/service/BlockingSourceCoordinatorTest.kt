/**
 * Unit tests for BlockingSourceCoordinator. Verifies the union semantics
 * across manual and scheduled sources, the priority rule (manual wins for
 * activeBlockInfo), and the isBlocked short-circuit.
 * Covers: ULT-24
 */
package com.ultimatefocus.app.domain.service

import app.cash.turbine.test
import com.ultimatefocus.app.domain.model.ActiveBlockInfo
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.BlockingSchedule
import com.ultimatefocus.app.domain.model.DayOfWeekMask
import com.ultimatefocus.app.domain.repository.FakeBlockedAppRepository
import com.ultimatefocus.app.domain.repository.FakeBlockingScheduleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class BlockingSourceCoordinatorTest {

    private lateinit var clock: FakeClock
    private lateinit var manualRepo: FakeBlockedAppRepository
    private lateinit var scheduleRepo: FakeBlockingScheduleRepository
    private lateinit var coordinator: BlockingSourceCoordinator

    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")
    private val zoneProvider = FixedZoneIdProvider(zone)

    @Before
    fun setUp() {
        // 2026-04-22 12:00 local — Wednesday, mid-day.
        val nowMs = ZonedDateTime.of(2026, 4, 22, 12, 0, 0, 0, zone)
            .toInstant().toEpochMilli()
        clock = FakeClock(current = nowMs)
        manualRepo = FakeBlockedAppRepository(clock)
        scheduleRepo = FakeBlockingScheduleRepository(clock, zoneProvider)
        coordinator = BlockingSourceCoordinator(manualRepo, scheduleRepo)
    }

    @Test
    fun observeActivePackageNames_unionsManualAndScheduled() = runTest {
        manualRepo.block("com.manual", BlockDuration.HOUR_1)
        scheduleRepo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L,
                    name = "Focus",
                    start = LocalTime.of(9, 0),
                    end = LocalTime.of(17, 0),
                    days = DayOfWeekMask.decode(0b1111111),
                    packageNames = setOf("com.scheduled"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        coordinator.observeActivePackageNames().test {
            val active = awaitItem()
            assertEquals(setOf("com.manual", "com.scheduled"), active)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeActivePackageNames_manualOnly() = runTest {
        manualRepo.block("com.manual", BlockDuration.HOUR_1)
        coordinator.observeActivePackageNames().test {
            assertEquals(setOf("com.manual"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeActivePackageNames_scheduledOnly() = runTest {
        scheduleRepo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L, name = "Focus",
                    start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
                    days = DayOfWeekMask.decode(0b1111111),
                    packageNames = setOf("com.scheduled"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        coordinator.observeActivePackageNames().test {
            assertEquals(setOf("com.scheduled"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeActivePackageNames_neitherSourceActive_emitsEmpty() = runTest {
        coordinator.observeActivePackageNames().test {
            assertEquals(emptySet<String>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isBlocked_shortCircuitsOnManual() = runTest {
        manualRepo.block("com.manual", BlockDuration.HOUR_1)
        assertTrue(coordinator.isBlocked("com.manual"))
    }

    @Test
    fun isBlocked_returnsTrueForScheduled() = runTest {
        scheduleRepo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L, name = "Focus",
                    start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
                    days = setOf(DayOfWeek.WEDNESDAY),
                    packageNames = setOf("com.scheduled"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        assertTrue(coordinator.isBlocked("com.scheduled"))
    }

    @Test
    fun isBlocked_returnsFalseForUnblocked() = runTest {
        assertFalse(coordinator.isBlocked("com.fine"))
    }

    @Test
    fun activeBlockInfo_returnsManual_evenWhenBothApply() = runTest {
        manualRepo.block("com.both", BlockDuration.DAY_1)
        scheduleRepo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L, name = "Focus",
                    start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
                    days = setOf(DayOfWeek.WEDNESDAY),
                    packageNames = setOf("com.both"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        val info = coordinator.activeBlockInfo("com.both")
        assertTrue(info is ActiveBlockInfo.Manual)
    }

    @Test
    fun activeBlockInfo_returnsScheduledWhenOnlyScheduledMatches() = runTest {
        scheduleRepo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L, name = "Work Focus",
                    start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
                    days = setOf(DayOfWeek.WEDNESDAY),
                    packageNames = setOf("com.scheduled"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        val info = coordinator.activeBlockInfo("com.scheduled")
        assertTrue(info is ActiveBlockInfo.Scheduled)
        assertEquals("Work Focus", (info as ActiveBlockInfo.Scheduled).scheduleName)
    }

    @Test
    fun activeBlockInfo_returnsNullForUnblocked() = runTest {
        assertNull(coordinator.activeBlockInfo("com.fine"))
    }
}
