/**
 * Encode/decode helpers between Set<DayOfWeek> and a 7-bit Int bitmask using
 * ISO ordering (MONDAY=bit0..SUNDAY=bit6). Storing days as one Int keeps the
 * Room schema introspectable and avoids a TypeConverter, while round-tripping
 * cleanly for the predicate that needs `today in days`.
 * Covers: ULT-24
 */
package com.ultimatefocus.app.domain.model

import java.time.DayOfWeek

object DayOfWeekMask {
    private const val ALL_BITS = 0b1111111

    fun encode(days: Set<DayOfWeek>): Int {
        var mask = 0
        for (day in days) {
            mask = mask or (1 shl (day.value - 1))
        }
        return mask and ALL_BITS
    }

    fun decode(mask: Int): Set<DayOfWeek> {
        val sanitized = mask and ALL_BITS
        if (sanitized == 0) return emptySet()
        return DayOfWeek.entries
            .filter { (sanitized and (1 shl (it.value - 1))) != 0 }
            .toSet()
    }
}
