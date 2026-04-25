/**
 * Compose UI tests for DayChipRow. Exercises chip render and toggle
 * callback for each day of the week. The narrow-style label is locale-
 * dependent so tests don't assert specific text — they assert that each
 * chip's stable testTag is present and that taps fire the toggle callback.
 * Covers: ULT-24
 */
package com.ultimatefocus.app.ui.blocking.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
class DayChipRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersAllSevenDays() {
        composeRule.setContent {
            UltimateFocusTheme {
                DayChipRow(selectedDays = emptySet(), onToggleDay = {})
            }
        }
        DayOfWeek.entries.forEach { day ->
            composeRule.onNodeWithTag("day_chip_${day.name.lowercase()}").assertIsDisplayed()
        }
    }

    @Test
    fun tappingChip_invokesToggleCallback() {
        val toggled = mutableListOf<DayOfWeek>()
        composeRule.setContent {
            UltimateFocusTheme {
                DayChipRow(selectedDays = emptySet(), onToggleDay = { toggled += it })
            }
        }
        composeRule.onNodeWithTag("day_chip_monday").performClick()
        composeRule.onNodeWithTag("day_chip_friday").performClick()
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), toggled)
    }
}
