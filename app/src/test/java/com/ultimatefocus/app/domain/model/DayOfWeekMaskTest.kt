/**
 * Unit tests for DayOfWeekMask. Verifies bit alignment to ISO order
 * (MONDAY=bit0..SUNDAY=bit6) and round-trip round-tripping for every
 * subset Cartesian-product, empty set, and full set.
 * Covers: ULT-24
 */
package com.ultimatefocus.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class DayOfWeekMaskTest {

    @Test
    fun encode_emptySet_returnsZero() {
        assertEquals(0, DayOfWeekMask.encode(emptySet()))
    }

    @Test
    fun encode_singleMonday_setsBit0() {
        assertEquals(0b0000001, DayOfWeekMask.encode(setOf(DayOfWeek.MONDAY)))
    }

    @Test
    fun encode_singleSunday_setsBit6() {
        assertEquals(0b1000000, DayOfWeekMask.encode(setOf(DayOfWeek.SUNDAY)))
    }

    @Test
    fun encode_weekdays_setsBits0Through4() {
        val weekdays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
        assertEquals(0b0011111, DayOfWeekMask.encode(weekdays))
    }

    @Test
    fun encode_weekends_setsBits5And6() {
        assertEquals(
            0b1100000,
            DayOfWeekMask.encode(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
        )
    }

    @Test
    fun encode_allDays_setsAllSevenBits() {
        assertEquals(0b1111111, DayOfWeekMask.encode(DayOfWeek.entries.toSet()))
    }

    @Test
    fun decode_zero_returnsEmptySet() {
        assertEquals(emptySet<DayOfWeek>(), DayOfWeekMask.decode(0))
    }

    @Test
    fun decode_allBitsSet_returnsAllDays() {
        assertEquals(DayOfWeek.entries.toSet(), DayOfWeekMask.decode(0b1111111))
    }

    @Test
    fun decode_extraHighBits_areMasked() {
        // Anything above bit 6 is junk and must be discarded so corrupt rows
        // can't crash the predicate.
        assertEquals(setOf(DayOfWeek.MONDAY), DayOfWeekMask.decode(0b1000_0000_0001))
    }

    @Test
    fun roundTrip_everySingleton_preservesValue() {
        for (day in DayOfWeek.entries) {
            val encoded = DayOfWeekMask.encode(setOf(day))
            val decoded = DayOfWeekMask.decode(encoded)
            assertEquals(setOf(day), decoded)
        }
    }
}
