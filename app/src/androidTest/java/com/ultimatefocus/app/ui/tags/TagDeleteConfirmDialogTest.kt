/**
 * Compose UI tests for TagDeleteConfirmDialog.
 * Verifies copy reflects the affected app count and Confirm/Cancel
 * fire the right callbacks.
 * Covers: ULT-72
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagDeleteConfirmDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun confirm_pluralizes_affected_count() {
        composeRule.setContent {
            UltimateFocusTheme {
                TagDeleteConfirmDialog(
                    tagName = "Social",
                    affectedAppCount = 3,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("It will be removed from 3 apps.").assertIsDisplayed()
    }

    @Test
    fun confirm_singular_for_one_app() {
        composeRule.setContent {
            UltimateFocusTheme {
                TagDeleteConfirmDialog(
                    tagName = "Games",
                    affectedAppCount = 1,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("It will be removed from 1 app.").assertIsDisplayed()
    }

    @Test
    fun confirm_unassigned_message_when_no_apps() {
        composeRule.setContent {
            UltimateFocusTheme {
                TagDeleteConfirmDialog(
                    tagName = "Empty",
                    affectedAppCount = 0,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("This tag is not assigned to any apps.").assertIsDisplayed()
    }

    @Test
    fun confirm_button_fires_callback() {
        var confirmed = false
        var dismissed = false
        composeRule.setContent {
            UltimateFocusTheme {
                TagDeleteConfirmDialog(
                    tagName = "Doomed",
                    affectedAppCount = 0,
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true },
                )
            }
        }
        composeRule.onNodeWithTag("tag_delete_confirm_button").performClick()
        assertTrue(confirmed)
        assertFalse(dismissed)
    }

    @Test
    fun cancel_button_fires_dismiss() {
        var confirmed = false
        var dismissed = false
        composeRule.setContent {
            UltimateFocusTheme {
                TagDeleteConfirmDialog(
                    tagName = "Doomed",
                    affectedAppCount = 0,
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true },
                )
            }
        }
        composeRule.onNodeWithTag("tag_delete_cancel_button").performClick()
        assertFalse(confirmed)
        assertTrue(dismissed)
    }
}
