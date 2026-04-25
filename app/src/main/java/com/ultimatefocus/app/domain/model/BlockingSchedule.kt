/**
 * Domain model for a recurring blocking schedule. The data-layer entity uses
 * minute-of-day Ints + a bitmask; the domain layer exposes typed LocalTime +
 * Set<DayOfWeek> + Set<String> so ViewModels and the predicate operate on
 * normal Kotlin/Java time types instead of raw integers.
 * Covers: ULT-24
 */
package com.ultimatefocus.app.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class BlockingSchedule(
    val id: Long,
    val name: String,
    val start: LocalTime,
    val end: LocalTime,
    val days: Set<DayOfWeek>,
    val packageNames: Set<String>,
    val createdEpochMs: Long,
) {
    val startMinuteOfDay: Int get() = start.hour * 60 + start.minute
    val endMinuteOfDay: Int get() = end.hour * 60 + end.minute

    /** A degenerate window (start == end) is treated as inactive everywhere. */
    val isCrossMidnight: Boolean get() = endMinuteOfDay <= startMinuteOfDay
}
