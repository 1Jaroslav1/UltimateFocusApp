/**
 * Unit tests for BlockedTimeFormatter. Covers happy paths at each
 * day/hour/minute boundary and defensive behavior at 0 and negative
 * remainingMs values.
 * Covers: ULT-20, ULT-21
 */
package com.ultimatefocus.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class BlockedTimeFormatterTest {

    @Test
    fun `zero millis formats as less than 1m`() {
        assertEquals("< 1m", BlockedTimeFormatter.formatRemaining(0L))
    }

    @Test
    fun `negative millis formats as less than 1m defensively`() {
        assertEquals("< 1m", BlockedTimeFormatter.formatRemaining(-5_000L))
    }

    @Test
    fun `thirty seconds formats as less than 1m`() {
        assertEquals("< 1m", BlockedTimeFormatter.formatRemaining(30_000L))
    }

    @Test
    fun `ninety seconds formats as 1m`() {
        assertEquals("1m", BlockedTimeFormatter.formatRemaining(90_000L))
    }

    @Test
    fun `forty seven minutes formats as 47m`() {
        assertEquals("47m", BlockedTimeFormatter.formatRemaining(TimeUnit.MINUTES.toMillis(47)))
    }

    @Test
    fun `three hours fifteen minutes formats as 3h 15m`() {
        val ms = TimeUnit.HOURS.toMillis(3) + TimeUnit.MINUTES.toMillis(15)
        assertEquals("3h 15m", BlockedTimeFormatter.formatRemaining(ms))
    }

    @Test
    fun `exactly one hour formats as 1h 0m`() {
        assertEquals("1h 0m", BlockedTimeFormatter.formatRemaining(TimeUnit.HOURS.toMillis(1)))
    }

    @Test
    fun `twenty five hours formats as 1d 1h`() {
        assertEquals("1d 1h", BlockedTimeFormatter.formatRemaining(TimeUnit.HOURS.toMillis(25)))
    }

    @Test
    fun `thirty days formats as 30d 0h`() {
        assertEquals("30d 0h", BlockedTimeFormatter.formatRemaining(TimeUnit.DAYS.toMillis(30)))
    }

    @Test
    fun `one day formats as 1d 0h`() {
        assertEquals("1d 0h", BlockedTimeFormatter.formatRemaining(TimeUnit.DAYS.toMillis(1)))
    }
}
