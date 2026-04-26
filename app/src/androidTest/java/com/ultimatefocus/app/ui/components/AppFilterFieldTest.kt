/**
 * Compose UI tests for AppFilterField — the shared filter input used by
 * both the schedule editor (ULT-24) and the In-App Reminder app list
 * (ULT-77). Verifies the test-tag is honored, the placeholder renders,
 * and onQueryChange is invoked on text input.
 *
 * Covers: ULT-24, ULT-77
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFilterFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersWithTestTagAndPlaceholder() {
        composeRule.setContent {
            UltimateFocusTheme {
                AppFilterField(
                    query = "",
                    onQueryChange = {},
                    placeholder = "Filter apps",
                    testTag = "tag_under_test",
                )
            }
        }
        composeRule.onNodeWithTag("tag_under_test").assertIsDisplayed()
        composeRule.onNodeWithText("Filter apps").assertIsDisplayed()
    }

    @Test
    fun emitsTypedTextThroughOnQueryChange() {
        var captured: String? = null
        composeRule.setContent {
            UltimateFocusTheme {
                AppFilterField(
                    query = "",
                    onQueryChange = { captured = it },
                    testTag = "tag_under_test",
                )
            }
        }
        composeRule.onNodeWithTag("tag_under_test").performTextInput("hello")
        assertEquals("hello", captured)
    }
}
