/**
 * Pure formatting helpers for a block's remaining duration and its absolute
 * expiry timestamp. Used by the context-menu status row, the full-screen
 * reminder, and the AccessibilityService-driven redirect screen.
 * Covers: ULT-20, ULT-21
 */
package com.ultimatefocus.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

object BlockedTimeFormatter {

    private val DATE_TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())

    fun formatRemaining(remainingMs: Long): String {
        if (remainingMs < TimeUnit.MINUTES.toMillis(1)) return "< 1m"
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs)
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    /**
     * Human-readable absolute moment at which the block ends, using the
     * device's default time zone. Example: "Apr 19, 18:45".
     * zone is a parameter so tests can pin a fixed zone.
     */
    fun formatUnblockDateTime(
        epochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String =
        Instant.ofEpochMilli(epochMs)
            .atZone(zone)
            .format(DATE_TIME_FORMATTER)
}
