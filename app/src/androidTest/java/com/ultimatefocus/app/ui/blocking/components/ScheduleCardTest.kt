/**
 * Compose UI tests for ScheduleCard. Verifies day pills render with the
 * correct on/off testTag suffix per day so the visual "highlighted short
 * weekday names" requirement (ULT-24 acceptance criteria) is testable, and
 * that the "Active now" badge only appears when the row is currently active.
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.ui.blocking.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.BlockingSchedule
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ScheduleCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun schedule(
        days: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
    ): BlockingSchedule = BlockingSchedule(
        id = 1L, name = "Focus",
        start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
        days = days,
        packageNames = setOf("com.a"),
        createdEpochMs = 0L,
    )

    @Test
    fun dayPills_renderWithCorrectOnOffTags() {
        composeRule.setContent {
            UltimateFocusTheme {
                ScheduleCard(
                    schedule = schedule(setOf(DayOfWeek.MONDAY)),
                    isCurrentlyActive = false,
                    onClick = null,
                )
            }
        }
        composeRule.onNodeWithTag("schedule_card_day_monday_on").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_card_day_tuesday_off").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_card_day_sunday_off").assertIsDisplayed()
    }

    @Test
    fun activeBadge_visibleOnlyWhenActive() {
        composeRule.setContent {
            UltimateFocusTheme {
                ScheduleCard(
                    schedule = schedule(),
                    isCurrentlyActive = true,
                    onClick = null,
                )
            }
        }
        composeRule.onNodeWithTag("schedule_card_active_1").assertIsDisplayed()
    }

    @Test
    fun activeBadge_hiddenWhenInactive() {
        composeRule.setContent {
            UltimateFocusTheme {
                ScheduleCard(
                    schedule = schedule(),
                    isCurrentlyActive = false,
                    onClick = null,
                )
            }
        }
        composeRule.onAllNodesWithTag("schedule_card_active_1").assertCountEquals(0)
    }

    @Test
    fun click_invokesCallback_whenProvided() {
        var clicked = false
        composeRule.setContent {
            UltimateFocusTheme {
                ScheduleCard(
                    schedule = schedule(),
                    isCurrentlyActive = false,
                    onClick = { clicked = true },
                )
            }
        }
        composeRule.onNodeWithTag("schedule_card_1").performClick()
        assertTrue(clicked)
    }
}
