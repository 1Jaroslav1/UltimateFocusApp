/**
 * Compose UI tests for DeleteScheduleConfirmDialog. Verifies confirm + cancel
 * callbacks fire and that the schedule name appears in the title so users
 * understand which schedule they're about to delete.
 * Covers: ULT-25
 */
package com.ultimatefocus.app.ui.blocking.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteScheduleConfirmDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun titleShowsScheduleName() {
        composeRule.setContent {
            UltimateFocusTheme {
                DeleteScheduleConfirmDialog(
                    scheduleName = "Work Focus",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("delete_schedule_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Delete \"Work Focus\"?").assertIsDisplayed()
    }

    @Test
    fun confirmButton_invokesCallback() {
        var confirmed = false
        composeRule.setContent {
            UltimateFocusTheme {
                DeleteScheduleConfirmDialog(
                    scheduleName = "Focus",
                    onConfirm = { confirmed = true },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("delete_schedule_confirm").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun cancelButton_invokesDismissCallback() {
        var dismissed = false
        composeRule.setContent {
            UltimateFocusTheme {
                DeleteScheduleConfirmDialog(
                    scheduleName = "Focus",
                    onConfirm = {},
                    onDismiss = { dismissed = true },
                )
            }
        }
        composeRule.onNodeWithTag("delete_schedule_cancel").performClick()
        assertTrue(dismissed)
    }
}
