/**
 * Validates that each BlockDuration enum entry matches the spec published in
 * the Linear AC: labels {1h,2h,4h,8h,24h,3d,7d,30d} and matching millis.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class BlockDurationTest {

    @Test
    fun `HOUR_1 matches 1 hour in millis and label`() {
        assertEquals(TimeUnit.HOURS.toMillis(1), BlockDuration.HOUR_1.millis)
        assertEquals("1h", BlockDuration.HOUR_1.label)
    }

    @Test
    fun `HOUR_2 matches 2 hours`() {
        assertEquals(TimeUnit.HOURS.toMillis(2), BlockDuration.HOUR_2.millis)
        assertEquals("2h", BlockDuration.HOUR_2.label)
    }

    @Test
    fun `HOUR_4 matches 4 hours`() {
        assertEquals(TimeUnit.HOURS.toMillis(4), BlockDuration.HOUR_4.millis)
        assertEquals("4h", BlockDuration.HOUR_4.label)
    }

    @Test
    fun `HOUR_8 matches 8 hours`() {
        assertEquals(TimeUnit.HOURS.toMillis(8), BlockDuration.HOUR_8.millis)
        assertEquals("8h", BlockDuration.HOUR_8.label)
    }

    @Test
    fun `DAY_1 matches 24 hours`() {
        assertEquals(TimeUnit.DAYS.toMillis(1), BlockDuration.DAY_1.millis)
        assertEquals("24h", BlockDuration.DAY_1.label)
    }

    @Test
    fun `DAY_3 matches 3 days`() {
        assertEquals(TimeUnit.DAYS.toMillis(3), BlockDuration.DAY_3.millis)
        assertEquals("3d", BlockDuration.DAY_3.label)
    }

    @Test
    fun `DAY_7 matches 7 days`() {
        assertEquals(TimeUnit.DAYS.toMillis(7), BlockDuration.DAY_7.millis)
        assertEquals("7d", BlockDuration.DAY_7.label)
    }

    @Test
    fun `DAY_30 matches 30 days`() {
        assertEquals(TimeUnit.DAYS.toMillis(30), BlockDuration.DAY_30.millis)
        assertEquals("30d", BlockDuration.DAY_30.label)
    }

    @Test
    fun `exactly 8 durations are defined`() {
        assertEquals(8, BlockDuration.entries.size)
    }

    @Test
    fun `durations are declared in increasing order`() {
        val values = BlockDuration.entries.map { it.millis }
        assertEquals(values.sorted(), values)
    }
}
