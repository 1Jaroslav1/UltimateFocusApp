/**
 * Predefined block durations offered to the user when blocking an app.
 * Values span 1 hour through 30 days as required by the AC. Each entry
 * carries its total millis and a short display label.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.domain.model

import java.util.concurrent.TimeUnit

enum class BlockDuration(val millis: Long, val label: String) {
    HOUR_1(TimeUnit.HOURS.toMillis(1), "1h"),
    HOUR_2(TimeUnit.HOURS.toMillis(2), "2h"),
    HOUR_4(TimeUnit.HOURS.toMillis(4), "4h"),
    HOUR_8(TimeUnit.HOURS.toMillis(8), "8h"),
    DAY_1(TimeUnit.DAYS.toMillis(1), "24h"),
    DAY_3(TimeUnit.DAYS.toMillis(3), "3d"),
    DAY_7(TimeUnit.DAYS.toMillis(7), "7d"),
    DAY_30(TimeUnit.DAYS.toMillis(30), "30d"),
}
