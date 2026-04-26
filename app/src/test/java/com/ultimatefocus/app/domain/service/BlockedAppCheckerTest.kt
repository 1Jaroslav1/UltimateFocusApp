/**
 * Unit tests for BlockedAppChecker. Uses a BlockingSourceCoordinator over
 * Fake repositories + FakeBlockedTagRepository + FakeTagRepository + FakeClock
 * so the suppression rule fires for any package blocked by a manual block
 * (ULT-22), an active recurring schedule (ULT-24), or via a blocked tag
 * (ULT-75). Clock advancement past expiry / window-end / tag expiry flips
 * the decision back off.
 * Covers: ULT-22, ULT-24, ULT-75
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.BlockingSchedule
import com.ultimatefocus.app.domain.model.DayOfWeekMask
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.domain.repository.FakeBlockedAppRepository
import com.ultimatefocus.app.domain.repository.FakeBlockedTagRepository
import com.ultimatefocus.app.domain.repository.FakeBlockingScheduleRepository
import com.ultimatefocus.app.domain.repository.FakeTagRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class BlockedAppCheckerTest {

    private lateinit var clock: FakeClock
    private lateinit var manualRepo: FakeBlockedAppRepository
    private lateinit var scheduleRepo: FakeBlockingScheduleRepository
    private lateinit var tagBlockRepo: FakeBlockedTagRepository
    private lateinit var tagRepo: FakeTagRepository
    private lateinit var checker: BlockedAppChecker

    private val zoneId = ZoneId.of("Europe/Warsaw")
    private val zoneProvider = FixedZoneIdProvider(zoneId)

    @Before
    fun setUp() {
        clock = FakeClock(current = wednesdayAt10AmEpochMs())
        manualRepo = FakeBlockedAppRepository(clock)
        scheduleRepo = FakeBlockingScheduleRepository(clock, zoneProvider)
        tagBlockRepo = FakeBlockedTagRepository(clock)
        tagRepo = FakeTagRepository()
        val coordinator = BlockingSourceCoordinator(
            manualRepository = manualRepo,
            scheduleRepository = scheduleRepo,
            blockedTagRepository = tagBlockRepo,
            tagRepository = tagRepo,
        )
        checker = BlockedAppChecker(coordinator)
    }

    // ── Manual blocks (ULT-22) ──────────────────────────────────────────────

    @Test
    fun shouldSuppress_returnsTrueForManuallyBlockedPackage() = runTest {
        manualRepo.block("com.blocked", BlockDuration.HOUR_1)
        assertTrue(checker.shouldSuppress("com.blocked"))
    }

    @Test
    fun shouldSuppress_returnsTrueForScheduleBlockedPackage() = runTest {
        scheduleRepo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L,
                    name = "Work Focus",
                    start = LocalTime.of(9, 0),
                    end = LocalTime.of(17, 0),
                    days = DayOfWeekMask.decode(0b1111111), // any day
                    packageNames = setOf("com.blocked"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        assertTrue(checker.shouldSuppress("com.blocked"))
    }

    @Test
    fun shouldSuppress_returnsFalseForUnblockedPackage() = runTest {
        assertFalse(checker.shouldSuppress("com.fine"))
    }

    @Test
    fun shouldSuppress_returnsFalseAfterManualBlockExpires() = runTest {
        manualRepo.block("com.blocked", BlockDuration.HOUR_1)
        clock.current += BlockDuration.HOUR_1.millis + 1L
        assertFalse(checker.shouldSuppress("com.blocked"))
    }

    @Test
    fun shouldSuppress_returnsFalseOutsideScheduleWindow() = runTest {
        scheduleRepo.seed(
            listOf(
                BlockingSchedule(
                    id = 1L,
                    name = "Sleep",
                    start = LocalTime.of(22, 0),
                    end = LocalTime.of(6, 0),
                    days = DayOfWeekMask.decode(0b1111111),
                    packageNames = setOf("com.blocked"),
                    createdEpochMs = 0L,
                ),
            ),
        )
        // 10:00 Wed local — outside the 22:00–06:00 window.
        assertFalse(checker.shouldSuppress("com.blocked"))
    }

    @Test
    fun shouldSuppress_isPackageScoped() = runTest {
        manualRepo.block("com.blocked", BlockDuration.HOUR_1)
        assertFalse(checker.shouldSuppress("com.other"))
    }

    // ── Tag-derived blocks (ULT-75) ─────────────────────────────────────────

    @Test
    fun shouldSuppress_returnsTrueWhenAppHasABlockedTag() = runTest {
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        tagRepo.seedAssignment("com.tagged", tagId = 1L)
        tagBlockRepo.blockTag(tagId = 1L, duration = BlockDuration.HOUR_1)
        assertTrue(checker.shouldSuppress("com.tagged"))
    }

    @Test
    fun shouldSuppress_returnsFalseAfterTagBlockExpires() = runTest {
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        tagRepo.seedAssignment("com.tagged", tagId = 1L)
        tagBlockRepo.blockTag(tagId = 1L, duration = BlockDuration.HOUR_1)
        clock.current += BlockDuration.HOUR_1.millis + 1L
        assertFalse(checker.shouldSuppress("com.tagged"))
    }

    @Test
    fun shouldSuppress_returnsFalseForUntaggedAppEvenIfOtherTagsAreBlocked() = runTest {
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        tagBlockRepo.blockTag(tagId = 1L, duration = BlockDuration.HOUR_1)
        // com.untagged is not assigned to tag 1
        assertFalse(checker.shouldSuppress("com.untagged"))
    }

    @Test
    fun shouldSuppress_remainsTrueWhenAppHasMultipleTagsAndOneIsBlocked() = runTest {
        tagRepo.seedTag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        tagRepo.seedTag(id = 2L, name = "Work", color = TagColor.SAGE_GREEN)
        tagRepo.seedAssignment("com.app", tagId = 1L)
        tagRepo.seedAssignment("com.app", tagId = 2L)
        tagBlockRepo.blockTag(tagId = 1L, duration = BlockDuration.HOUR_1)
        // Tag 2 is not blocked — but tag 1 covers the app.
        assertTrue(checker.shouldSuppress("com.app"))
    }

    private fun wednesdayAt10AmEpochMs(): Long {
        // 2026-04-22 was a Wednesday.
        val zdt = ZonedDateTime.of(2026, 4, 22, 10, 0, 0, 0, zoneId)
        require(zdt.dayOfWeek == DayOfWeek.WEDNESDAY)
        return zdt.toInstant().toEpochMilli()
    }
}
