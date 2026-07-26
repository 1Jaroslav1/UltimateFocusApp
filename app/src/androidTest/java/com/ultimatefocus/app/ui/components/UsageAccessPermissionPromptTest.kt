/**
 * Compose tests for UsageAccessPermissionPrompt.
 * Verifies rationale text is shown and both action buttons invoke the
 * expected callbacks. Mirrors AccessibilityPermissionPromptTest.
 * Covers: ULT-86
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
class UsageAccessPermissionPromptTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun prompt_rendersRationaleAndBothButtons() {
        composeRule.setContent {
            UltimateFocusTheme {
                UsageAccessPermissionPrompt(
                    appLabel = "Instagram",
                    onGrantAccess = {},
                    onContinueWithout = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("usage_access_permission_prompt").assertIsDisplayed()
        composeRule.onNodeWithText("Keep the block on Instagram reliable?").assertIsDisplayed()
        composeRule.onNodeWithTag("usage_access_permission_grant").assertIsDisplayed()
        composeRule.onNodeWithTag("usage_access_permission_continue").assertIsDisplayed()
    }

    @Test
    fun grantButton_invokesGrantAndDismiss() {
        var grantedCount = 0
        var dismissedCount = 0
        composeRule.setContent {
            UltimateFocusTheme {
                UsageAccessPermissionPrompt(
                    appLabel = "Slack",
                    onGrantAccess = { grantedCount++ },
                    onContinueWithout = {},
                    onDismiss = { dismissedCount++ },
                )
            }
        }
        composeRule.onNodeWithTag("usage_access_permission_grant").performClick()
        assertEquals(1, grantedCount)
        assertEquals(1, dismissedCount)
    }

    @Test
    fun continueButton_invokesContinueAndDismiss() {
        var continueCount = 0
        var dismissedCount = 0
        composeRule.setContent {
            UltimateFocusTheme {
                UsageAccessPermissionPrompt(
                    appLabel = "Slack",
                    onGrantAccess = {},
                    onContinueWithout = { continueCount++ },
                    onDismiss = { dismissedCount++ },
                )
            }
        }
        composeRule.onNodeWithTag("usage_access_permission_continue").performClick()
        assertEquals(1, continueCount)
        assertEquals(1, dismissedCount)
    }
}
