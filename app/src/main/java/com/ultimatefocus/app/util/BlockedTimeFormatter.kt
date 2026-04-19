/**
 * Pure formatting helper for a block's remaining duration, e.g.
 * "2h 17m" / "47m" / "3d 4h" / "< 1m". Used by both the context-menu
 * status row (ULT-20) and the full-screen reminder (ULT-21).
 * Covers: ULT-20, ULT-21
 */
package com.ultimatefocus.app.util

import java.util.concurrent.TimeUnit

object BlockedTimeFormatter {
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
}
