/**
 * Unit tests for BlockingSourceCoordinator. Verifies the union semantics
 * across all three sources (manual, scheduled, tag-derived), the priority
 * rule (Manual > Scheduled > Tag for activeBlockInfo), and the isBlocked
 * short-circuit.
 * Covers: ULT-24, ULT-75
 */
package com.ultimatefocus.app.domain.service

import app.cash.turbine.test
import com.ultimatefocus.app.domain.model.ActiveBlockInfo
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.BlockingSchedule
import com.ultimatefocus.app.domain.model.DayOfWeekMask
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.domain.repository.FakeBlockedAppRepository
import com.ultimatefocus.app.domain.repository.FakeBlockedTagRepository
import com.ultimatefocus.app.domain.repository.FakeBlockingScheduleRepository
import com.ultimatefocus.app.domain.repository.FakeTagRepository
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
    private lateinit var tagRepo: FakeTagRepository
    private lateinit var blockedTagRepo: FakeBlockedTagRepository
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
        tagRepo = FakeTagRepository()
        blockedTagRepo = FakeBlockedTagRepository(clock)
        coordinator = BlockingSourceCoordinator(
            manualRepository = manualRepo,
            scheduleRepository = scheduleRepo,
            blockedTagRepository = blockedTagRepo,
            tagRepository = tagRepo,
        )
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

    // ── Tag-derived blocks (ULT-75) ────────────────────────────────────────

    @Test
    fun observeActivePackageNames_includesTagBlockedPackages() = runTest {
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        tagRepo.seedAssignment("com.tagged", tagId = 1L)
        blockedTagRepo.setAppsForTag(1L, listOf("com.tagged"))
        blockedTagRepo.blockTag(1L, BlockDuration.HOUR_1)

        coordinator.observeActivePackageNames().test {
            assertEquals(setOf("com.tagged"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeActivePackageNames_unionsAllThreeSources() = runTest {
        manualRepo.block("com.manual", BlockDuration.HOUR_1)
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
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        tagRepo.seedAssignment("com.tagged", tagId = 1L)
        blockedTagRepo.setAppsForTag(1L, listOf("com.tagged"))
        blockedTagRepo.blockTag(1L, BlockDuration.HOUR_1)

        coordinator.observeActivePackageNames().test {
            assertEquals(setOf("com.manual", "com.scheduled", "com.tagged"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isBlocked_returnsTrueForTagBlocked() = runTest {
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        tagRepo.seedAssignment("com.tagged", tagId = 1L)
        blockedTagRepo.blockTag(1L, BlockDuration.HOUR_1)
        assertTrue(coordinator.isBlocked("com.tagged"))
    }

    @Test
    fun activeBlockInfo_returnsTagWhenOnlyTagMatches() = runTest {
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        tagRepo.seedAssignment("com.tagged", tagId = 1L)
        blockedTagRepo.blockTag(1L, BlockDuration.HOUR_1)

        val info = coordinator.activeBlockInfo("com.tagged")
        assertTrue(info is ActiveBlockInfo.Tag)
        assertEquals("1h", (info as ActiveBlockInfo.Tag).durationLabel)
    }

    @Test
    fun activeBlockInfo_priority_manualOverTag() = runTest {
        manualRepo.block("com.both", BlockDuration.DAY_1)
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        tagRepo.seedAssignment("com.both", tagId = 1L)
        blockedTagRepo.blockTag(1L, BlockDuration.HOUR_1)
        // Manual should win even though Tag also covers the package.
        assertTrue(coordinator.activeBlockInfo("com.both") is ActiveBlockInfo.Manual)
    }

    @Test
    fun activeBlockInfo_priority_scheduledOverTag() = runTest {
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
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        tagRepo.seedAssignment("com.both", tagId = 1L)
        blockedTagRepo.blockTag(1L, BlockDuration.HOUR_1)
        // Scheduled should win even though Tag also covers the package.
        assertTrue(coordinator.activeBlockInfo("com.both") is ActiveBlockInfo.Scheduled)
    }
}
