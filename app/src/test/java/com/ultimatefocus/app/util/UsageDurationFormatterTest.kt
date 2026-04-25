/**
 * Unit tests for UsageDurationFormatter — pinning the user-facing strings
 * used by the schedule editor's app picker rows so visual regressions
 * (e.g. "0m" vs "—" for unrecorded usage, "1h 0m" vs "1h" for whole-hour
 * totals) are caught.
 * Covers: ULT-24 (iteration)
 */
package com.ultimatefocus.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageDurationFormatterTest {

    @Test
    fun zeroOrNegative_rendersAsEmDash() {
        assertEquals("—", UsageDurationFormatter.formatWeeklyUsage(0L))
        assertEquals("—", UsageDurationFormatter.formatWeeklyUsage(-100L))
    }

    @Test
    fun subMinute_rendersAsSeconds() {
        assertEquals("12s", UsageDurationFormatter.formatWeeklyUsage(12_000L))
        assertEquals("1s", UsageDurationFormatter.formatWeeklyUsage(1_500L))
    }

    @Test
    fun subHour_rendersAsMinutes() {
        assertEquals("47m", UsageDurationFormatter.formatWeeklyUsage(47L * 60_000L))
        assertEquals("1m", UsageDurationFormatter.formatWeeklyUsage(60_000L))
    }

    @Test
    fun overHour_rendersAsHoursMinutes() {
        assertEquals("3h 12m", UsageDurationFormatter.formatWeeklyUsage(3L * 3_600_000L + 12L * 60_000L))
        assertEquals("12h 5m", UsageDurationFormatter.formatWeeklyUsage(12L * 3_600_000L + 5L * 60_000L))
    }

    @Test
    fun wholeHour_dropsZeroMinutes() {
        assertEquals("2h", UsageDurationFormatter.formatWeeklyUsage(2L * 3_600_000L))
        assertEquals("1h", UsageDurationFormatter.formatWeeklyUsage(3_600_000L))
    }
}
