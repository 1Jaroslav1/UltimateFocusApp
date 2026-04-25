/**
 * Unit tests for buildInlinePickerList — the editor's helper that decides
 * which apps surface in the inline top-N picker. Selected apps must always
 * be visible (so the editor's state is glanceable even for selections
 * outside the top-by-usage window) and remaining slots must be filled with
 * the highest-usage non-selected apps.
 * Covers: ULT-24 (iteration)
 */
package com.ultimatefocus.app.ui.blocking

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildInlinePickerListTest {

    private fun item(pkg: String, usage: Long = 0L) =
        AppPickerItem(packageName = pkg, label = pkg, weeklyUsageMillis = usage)

    @Test
    fun emptyInputReturnsEmpty() {
        assertEquals(emptyList<AppPickerItem>(), buildInlinePickerList(emptyList(), emptySet(), 15))
    }

    @Test
    fun limitTakesTopOfSortedInput_whenNothingSelected() {
        val sorted = (1..20).map { item("com.$it", usage = (20 - it).toLong()) }
        val result = buildInlinePickerList(sorted, selected = emptySet(), limit = 15)
        assertEquals(15, result.size)
        assertEquals(sorted.take(15), result)
    }

    @Test
    fun selectedAppsArePinnedToFront_evenWhenOutsideTopByUsage() {
        // 20 apps; one selected at the very tail (lowest usage).
        val sorted = (1..20).map { item("com.$it", usage = (20 - it).toLong()) }
        val selected = setOf("com.20")
        val result = buildInlinePickerList(sorted, selected = selected, limit = 15)
        assertEquals(15, result.size)
        assertEquals("com.20", result.first().packageName)
        // Remaining 14 slots filled with the top 14 non-selected.
        assertEquals(sorted.take(14), result.drop(1))
    }

    @Test
    fun preservesRelativeOrderWithinSegments() {
        val sorted = listOf(
            item("com.a", usage = 100L),
            item("com.b", usage = 90L),
            item("com.c", usage = 80L),
            item("com.d", usage = 70L),
        )
        val result = buildInlinePickerList(sorted, selected = setOf("com.b", "com.d"), limit = 4)
        // Selected in original order, then unselected in original order.
        assertEquals(
            listOf("com.b", "com.d", "com.a", "com.c"),
            result.map { it.packageName },
        )
    }

    @Test
    fun selectedCountExceedingLimitStillShowsAllSelected() {
        // 17 selected with limit 15 — every selected app must still surface.
        val sorted = (1..20).map { item("com.$it", usage = (20 - it).toLong()) }
        val selected = (1..17).map { "com.$it" }.toSet()
        val result = buildInlinePickerList(sorted, selected = selected, limit = 15)
        assertEquals(17, result.size)
        assertEquals(selected, result.map { it.packageName }.toSet())
    }
}
