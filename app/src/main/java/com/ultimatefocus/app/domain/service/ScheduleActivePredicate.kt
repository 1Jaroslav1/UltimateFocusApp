/**
 * Pure-logic decision: is a given BlockingSchedule currently active for the
 * supplied wall-clock instant in the supplied zone?
 *
 *   same-day window  (start < end): active iff today in days AND minute in [start, end)
 *   cross-midnight   (start > end): active iff (today in days AND minute >= start)
 *                                        OR    (yesterday in days AND minute < end)
 *   degenerate       (start == end): always inactive (validation rejects this on save)
 *
 * The end boundary is exclusive (matches the strict > semantics manual
 * blocks already use for expiry — see BlockedAppDao).
 *
 * Factored out as an `object` so it's unit-testable without DI and so callers
 * (BlockingScheduleRepositoryImpl, BlockingScheduleEditorViewModel) share one
 * implementation.
 * Covers: ULT-24
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.BlockingSchedule
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object ScheduleActivePredicate {

    fun isActive(
        schedule: BlockingSchedule,
        nowEpochMs: Long,
        zone: ZoneId,
    ): Boolean {
        if (schedule.days.isEmpty()) return false
        val s = schedule.startMinuteOfDay
        val e = schedule.endMinuteOfDay
        if (s == e) return false

        val zdt = Instant.ofEpochMilli(nowEpochMs).atZone(zone)
        val today = zdt.dayOfWeek
        val minute = zdt.hour * 60 + zdt.minute

        return if (s < e) {
            today in schedule.days && minute in s until e
        } else {
            (today in schedule.days && minute >= s) ||
                (today.minus(1L) in schedule.days && minute < e)
        }
    }

    /**
     * Returns the epoch-ms at which the currently active window ends, or
     * null if the schedule is not active at [nowEpochMs]. Used to populate
     * "Blocked until HH:MM" copy on BlockedAppActivity.
     */
    fun activeWindowEndEpochMs(
        schedule: BlockingSchedule,
        nowEpochMs: Long,
        zone: ZoneId,
    ): Long? {
        if (!isActive(schedule, nowEpochMs, zone)) return null
        val zdt = Instant.ofEpochMilli(nowEpochMs).atZone(zone)
        val s = schedule.startMinuteOfDay
        val e = schedule.endMinuteOfDay
        val endHour = e / 60
        val endMinute = e % 60
        val today = zdt.toLocalDate()

        val endDate = if (s < e) {
            // Same-day window: window ends today at e.
            today
        } else {
            // Cross-midnight window. Two sub-cases for the active state:
            //  (a) we're still on the day-of-start (minute >= s) → window ends tomorrow at e
            //  (b) we're past midnight on the next morning (minute < e) → window ends today at e
            val minute = zdt.hour * 60 + zdt.minute
            if (minute >= s) today.plusDays(1) else today
        }
        val endLocal = LocalDateTime.of(endDate, java.time.LocalTime.of(endHour, endMinute))
        return endLocal.atZone(zone).toInstant().toEpochMilli()
    }
}
