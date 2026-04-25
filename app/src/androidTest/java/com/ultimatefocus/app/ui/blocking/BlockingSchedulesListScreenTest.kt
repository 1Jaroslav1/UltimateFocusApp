/**
 * Compose UI tests for BlockingSchedulesListContent. Drives the stateless
 * variant directly to bypass Hilt wiring; verifies empty-state copy, card
 * rendering, "Active now" indicator, and the Add / card-click callbacks.
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.ui.blocking

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.BlockingSchedule
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class BlockingSchedulesListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_visibleWhenNoSchedules() {
        composeRule.setContent {
            UltimateFocusTheme {
                BlockingSchedulesListContent(
                    uiState = BlockingSchedulesListUiState(),
                    onNavigateBack = {},
                    onAddSchedule = {},
                )
            }
        }
        composeRule.onNodeWithTag("blocking_schedules_empty_state").assertIsDisplayed()
    }

    @Test
    fun addButton_invokesCallback() {
        var clicked = false
        composeRule.setContent {
            UltimateFocusTheme {
                BlockingSchedulesListContent(
                    uiState = BlockingSchedulesListUiState(),
                    onNavigateBack = {},
                    onAddSchedule = { clicked = true },
                )
            }
        }
        composeRule.onNodeWithTag("add_schedule_button").performClick()
        assertTrue(clicked)
    }

    @Test
    fun listItem_rendersCardWithName() {
        val schedule = BlockingSchedule(
            id = 7L,
            name = "Work Focus",
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0),
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            packageNames = setOf("com.a"),
            createdEpochMs = 1L,
        )
        composeRule.setContent {
            UltimateFocusTheme {
                BlockingSchedulesListContent(
                    uiState = BlockingSchedulesListUiState(
                        schedules = listOf(BlockingScheduleRow(schedule, isCurrentlyActive = false)),
                    ),
                    onNavigateBack = {},
                    onAddSchedule = {},
                )
            }
        }
        composeRule.onNodeWithTag("schedule_card_7").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_card_name_7").assertIsDisplayed()
    }

    @Test
    fun activeNowBadge_visibleWhenScheduleIsActive() {
        val schedule = BlockingSchedule(
            id = 1L, name = "Active",
            start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
            days = setOf(DayOfWeek.MONDAY),
            packageNames = setOf("com.a"),
            createdEpochMs = 0L,
        )
        composeRule.setContent {
            UltimateFocusTheme {
                BlockingSchedulesListContent(
                    uiState = BlockingSchedulesListUiState(
                        schedules = listOf(BlockingScheduleRow(schedule, isCurrentlyActive = true)),
                    ),
                    onNavigateBack = {},
                    onAddSchedule = {},
                )
            }
        }
        composeRule.onNodeWithTag("schedule_card_active_1").assertIsDisplayed()
    }

    @Test
    fun cardClick_invokesCallbackWithId() {
        val schedule = BlockingSchedule(
            id = 42L, name = "Edit me",
            start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
            days = setOf(DayOfWeek.MONDAY),
            packageNames = setOf("com.a"),
            createdEpochMs = 0L,
        )
        var receivedId: Long? = null
        composeRule.setContent {
            UltimateFocusTheme {
                BlockingSchedulesListContent(
                    uiState = BlockingSchedulesListUiState(
                        schedules = listOf(BlockingScheduleRow(schedule, isCurrentlyActive = false)),
                    ),
                    onNavigateBack = {},
                    onAddSchedule = {},
                    onEditSchedule = { id -> receivedId = id },
                )
            }
        }
        composeRule.onNodeWithTag("schedule_card_42").performClick()
        assertEquals(42L, receivedId)
    }

    @Test
    fun cardClick_isNoOp_whenEditCallbackNull() {
        // ULT-24 commit ships the list with onEditSchedule = null. Tapping
        // a card must not crash the UI. (We can't assert "nothing happened"
        // beyond the test not throwing.)
        val schedule = BlockingSchedule(
            id = 1L, name = "Read only",
            start = LocalTime.of(9, 0), end = LocalTime.of(17, 0),
            days = setOf(DayOfWeek.MONDAY),
            packageNames = setOf("com.a"),
            createdEpochMs = 0L,
        )
        composeRule.setContent {
            UltimateFocusTheme {
                BlockingSchedulesListContent(
                    uiState = BlockingSchedulesListUiState(
                        schedules = listOf(BlockingScheduleRow(schedule, isCurrentlyActive = false)),
                    ),
                    onNavigateBack = {},
                    onAddSchedule = {},
                    onEditSchedule = null,
                )
            }
        }
        composeRule.onNodeWithTag("schedule_card_1").performClick()
        composeRule.onNodeWithTag("schedule_card_1").assertIsDisplayed()
    }
}
