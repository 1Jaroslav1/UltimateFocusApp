/**
 * Unit tests for ScheduleActivePredicate. Pins the zone to Europe/Warsaw so
 * DST transitions can be exercised deterministically. Covers same-day,
 * cross-midnight, and degenerate windows; exclusive end-boundary; empty-day
 * sets; and DST spring-forward / fall-back behavior.
 * Covers: ULT-24
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.BlockingSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleActivePredicateTest {

    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")

    // 2026-04-22 is Wednesday in real calendar.
    private fun wednesdayAt(hour: Int, minute: Int): Long =
        ZonedDateTime.of(2026, 4, 22, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun thursdayAt(hour: Int, minute: Int): Long =
        ZonedDateTime.of(2026, 4, 23, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun schedule(
        start: LocalTime,
        end: LocalTime,
        days: Set<DayOfWeek>,
    ): BlockingSchedule = BlockingSchedule(
        id = 1L,
        name = "Test",
        start = start,
        end = end,
        days = days,
        packageNames = setOf("com.test"),
        createdEpochMs = 0L,
    )

    // ── Same-day window ─────────────────────────────────────────────────────

    @Test
    fun isActive_sameDayWindow_midRange_returnsTrue() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0), setOf(DayOfWeek.WEDNESDAY))
        assertTrue(ScheduleActivePredicate.isActive(s, wednesdayAt(12, 0), zone))
    }

    @Test
    fun isActive_sameDayWindow_atExactStart_returnsTrue() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0), setOf(DayOfWeek.WEDNESDAY))
        assertTrue(ScheduleActivePredicate.isActive(s, wednesdayAt(9, 0), zone))
    }

    @Test
    fun isActive_sameDayWindow_atExactEnd_returnsFalse() {
        // End boundary is exclusive (matches manual-block strict > semantics).
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0), setOf(DayOfWeek.WEDNESDAY))
        assertFalse(ScheduleActivePredicate.isActive(s, wednesdayAt(17, 0), zone))
    }

    @Test
    fun isActive_sameDayWindow_beforeStart_returnsFalse() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0), setOf(DayOfWeek.WEDNESDAY))
        assertFalse(ScheduleActivePredicate.isActive(s, wednesdayAt(8, 59), zone))
    }

    @Test
    fun isActive_sameDayWindow_afterEnd_returnsFalse() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0), setOf(DayOfWeek.WEDNESDAY))
        assertFalse(ScheduleActivePredicate.isActive(s, wednesdayAt(17, 1), zone))
    }

    @Test
    fun isActive_sameDayWindow_dayNotInSet_returnsFalse() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0), setOf(DayOfWeek.MONDAY))
        assertFalse(ScheduleActivePredicate.isActive(s, wednesdayAt(12, 0), zone))
    }

    // ── Cross-midnight window ──────────────────────────────────────────────

    @Test
    fun isActive_crossMidnight_lateEvening_dayMatches_returnsTrue() {
        // 22:00 → 06:00; Wed enabled. 23:00 Wed → active.
        val s = schedule(LocalTime.of(22, 0), LocalTime.of(6, 0), setOf(DayOfWeek.WEDNESDAY))
        assertTrue(ScheduleActivePredicate.isActive(s, wednesdayAt(23, 0), zone))
    }

    @Test
    fun isActive_crossMidnight_earlyMorningOfNextDay_returnsTrue() {
        // 22:00 → 06:00 with Wed enabled. 02:00 Thu → active because yesterday=Wed is in the set.
        val s = schedule(LocalTime.of(22, 0), LocalTime.of(6, 0), setOf(DayOfWeek.WEDNESDAY))
        assertTrue(ScheduleActivePredicate.isActive(s, thursdayAt(2, 0), zone))
    }

    @Test
    fun isActive_crossMidnight_earlyMorningOnlyTodayEnabled_returnsFalse() {
        // 22:00 → 06:00 with only Thursday enabled. 02:00 Thu — yesterday=Wed not in set,
        // and today (Thu) minute < end is true but `minute >= start` is false (02:00 < 22:00)
        // → predicate returns false on the today branch and yesterday is also Wed (not in days).
        val s = schedule(LocalTime.of(22, 0), LocalTime.of(6, 0), setOf(DayOfWeek.THURSDAY))
        assertFalse(ScheduleActivePredicate.isActive(s, thursdayAt(2, 0), zone))
    }

    @Test
    fun isActive_crossMidnight_atExactEnd_returnsFalse() {
        val s = schedule(LocalTime.of(22, 0), LocalTime.of(6, 0), setOf(DayOfWeek.WEDNESDAY))
        assertFalse(ScheduleActivePredicate.isActive(s, thursdayAt(6, 0), zone))
    }

    // ── Degenerate windows ─────────────────────────────────────────────────

    @Test
    fun isActive_startEqualsEnd_returnsFalse() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(9, 0), setOf(DayOfWeek.WEDNESDAY))
        assertFalse(ScheduleActivePredicate.isActive(s, wednesdayAt(9, 0), zone))
    }

    @Test
    fun isActive_emptyDaySet_returnsFalse() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0), emptySet())
        assertFalse(ScheduleActivePredicate.isActive(s, wednesdayAt(12, 0), zone))
    }

    // ── DST behavior ───────────────────────────────────────────────────────

    @Test
    fun isActive_duringDstSpringForward_neverFires() {
        // 2026-03-29 02:00–03:00 in Europe/Warsaw doesn't exist (clocks jump 02:00→03:00).
        // A schedule covering that local hour can never be "true" on that day because
        // the corresponding LocalDateTime can't be reached from any Instant. We pick a
        // moment shortly before the jump (01:59 local) and after (03:01 local) and assert
        // both fall outside a 02:00–03:00 window.
        val s = schedule(LocalTime.of(2, 0), LocalTime.of(3, 0), setOf(DayOfWeek.SUNDAY))
        val before = ZonedDateTime.of(2026, 3, 29, 1, 59, 0, 0, zone).toInstant().toEpochMilli()
        val after = ZonedDateTime.of(2026, 3, 29, 3, 1, 0, 0, zone).toInstant().toEpochMilli()
        assertFalse(ScheduleActivePredicate.isActive(s, before, zone))
        assertFalse(ScheduleActivePredicate.isActive(s, after, zone))
    }

    @Test
    fun isActive_duringDstFallBack_fires() {
        // 2026-10-25 in Europe/Warsaw. Clocks fall back from 03:00 → 02:00. The local
        // hour 02:30 happens twice. A 02:00–03:00 window matches both occurrences.
        val s = schedule(LocalTime.of(2, 0), LocalTime.of(3, 0), setOf(DayOfWeek.SUNDAY))
        // Pre-fallback 02:30 (UTC offset +02:00):
        val first = ZonedDateTime.of(2026, 10, 25, 2, 30, 0, 0, zone).toInstant().toEpochMilli()
        // Post-fallback 02:30 (UTC offset +01:00) — one hour later in real time.
        val second = first + 3_600_000L
        assertTrue(ScheduleActivePredicate.isActive(s, first, zone))
        assertTrue(ScheduleActivePredicate.isActive(s, second, zone))
    }

    // ── Window-end calculation ─────────────────────────────────────────────

    @Test
    fun activeWindowEndEpochMs_sameDay_returnsTodayAtEnd() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0), setOf(DayOfWeek.WEDNESDAY))
        val now = wednesdayAt(12, 0)
        val end = ScheduleActivePredicate.activeWindowEndEpochMs(s, now, zone)
        assertEquals(wednesdayAt(17, 0), end)
    }

    @Test
    fun activeWindowEndEpochMs_crossMidnight_beforeMidnight_returnsTomorrowAtEnd() {
        val s = schedule(LocalTime.of(22, 0), LocalTime.of(6, 0), setOf(DayOfWeek.WEDNESDAY))
        val now = wednesdayAt(23, 0)
        val end = ScheduleActivePredicate.activeWindowEndEpochMs(s, now, zone)
        assertEquals(thursdayAt(6, 0), end)
    }

    @Test
    fun activeWindowEndEpochMs_crossMidnight_afterMidnight_returnsTodayAtEnd() {
        val s = schedule(LocalTime.of(22, 0), LocalTime.of(6, 0), setOf(DayOfWeek.WEDNESDAY))
        val now = thursdayAt(2, 0)
        val end = ScheduleActivePredicate.activeWindowEndEpochMs(s, now, zone)
        assertEquals(thursdayAt(6, 0), end)
    }

    @Test
    fun activeWindowEndEpochMs_inactive_returnsNull() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0), setOf(DayOfWeek.WEDNESDAY))
        val now = wednesdayAt(20, 0)
        assertNull(ScheduleActivePredicate.activeWindowEndEpochMs(s, now, zone))
    }
}
