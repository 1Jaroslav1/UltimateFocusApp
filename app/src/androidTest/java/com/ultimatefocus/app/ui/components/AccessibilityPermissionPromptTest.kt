/**
 * Compose tests for AccessibilityPermissionPrompt.
 * Verifies rationale text is shown and both action buttons invoke the
 * expected callbacks.
 * Covers: ULT-20, ULT-21
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityPermissionPromptTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun prompt_rendersRationaleAndBothButtons() {
        composeRule.setContent {
            UltimateFocusTheme {
                AccessibilityPermissionPrompt(
                    appLabel = "Instagram",
                    onGrantAccess = {},
                    onContinueWithout = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("accessibility_permission_prompt").assertIsDisplayed()
        composeRule.onNodeWithText("Enforce the block on Instagram?").assertIsDisplayed()
        composeRule.onNodeWithTag("accessibility_permission_grant").assertIsDisplayed()
        composeRule.onNodeWithTag("accessibility_permission_continue").assertIsDisplayed()
    }

    @Test
    fun grantButton_invokesGrantAndDismiss() {
        var grantedCount = 0
        var dismissedCount = 0
        composeRule.setContent {
            UltimateFocusTheme {
                AccessibilityPermissionPrompt(
                    appLabel = "Slack",
                    onGrantAccess = { grantedCount++ },
                    onContinueWithout = {},
                    onDismiss = { dismissedCount++ },
                )
            }
        }
        composeRule.onNodeWithTag("accessibility_permission_grant").performClick()
        assertEquals(1, grantedCount)
        assertEquals(1, dismissedCount)
    }

    @Test
    fun continueButton_invokesContinueAndDismiss() {
        var continueCount = 0
        var dismissedCount = 0
        composeRule.setContent {
            UltimateFocusTheme {
                AccessibilityPermissionPrompt(
                    appLabel = "Slack",
                    onGrantAccess = {},
                    onContinueWithout = { continueCount++ },
                    onDismiss = { dismissedCount++ },
                )
            }
        }
        composeRule.onNodeWithTag("accessibility_permission_continue").performClick()
        assertEquals(1, continueCount)
        assertEquals(1, dismissedCount)
    }
}
