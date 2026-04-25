/**
 * Formats a millisecond duration as a compact human label for the schedule
 * editor's app picker rows ("3h 12m", "47m", "12s", "—").
 *
 * Granularity follows the picker's needs: hours-and-minutes for any
 * duration over an hour, minutes-only between 1m and 1h, seconds when
 * under a minute, and an em-dash for zero — visually distinct so users
 * understand "no recorded usage" vs. "low usage".
 *
 * Covers: ULT-24 (iteration)
 */
package com.ultimatefocus.app.util

object UsageDurationFormatter {

    fun formatWeeklyUsage(millis: Long): String {
        if (millis <= 0L) return "—"
        val totalSeconds = millis / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0L -> if (minutes > 0L) "${hours}h ${minutes}m" else "${hours}h"
            minutes > 0L -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}
