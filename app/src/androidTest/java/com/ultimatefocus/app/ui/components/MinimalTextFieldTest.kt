/**
 * Compose UI tests for MinimalTextField — the centralized minimal input
 * shared across the drawer search, schedule editor app browser, and
 * In-App Reminder app filter. Verifies the testTag passes through, slots
 * render, typing emits onValueChange, and the rendered height stays under
 * the previous OutlinedTextField baseline (~56 dp) so the user-feedback
 * "shrink the input" requirement holds at runtime.
 *
 * Covers: ULT-82
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.Modifier

@RunWith(AndroidJUnit4::class)
class MinimalTextFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersWithTestTagAndPlaceholder() {
        composeRule.setContent {
            UltimateFocusTheme {
                MinimalTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = "Search apps…",
                    testTag = "minimal_field",
                )
            }
        }
        composeRule.onNodeWithTag("minimal_field").assertIsDisplayed()
        composeRule.onNodeWithText("Search apps…").assertIsDisplayed()
    }

    @Test
    fun emitsTypedTextThroughOnValueChange() {
        var captured: String? = null
        composeRule.setContent {
            UltimateFocusTheme {
                MinimalTextField(
                    value = "",
                    onValueChange = { captured = it },
                    testTag = "minimal_field",
                )
            }
        }
        composeRule.onNodeWithTag("minimal_field").performTextInput("hello")
        assertEquals("hello", captured)
    }

    @Test
    fun rendersLeadingAndTrailingIconSlots() {
        var trailingClicked = false
        composeRule.setContent {
            UltimateFocusTheme {
                MinimalTextField(
                    value = "abc",
                    onValueChange = {},
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Leading",
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { trailingClicked = true }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Trailing",
                            )
                        }
                    },
                    testTag = "minimal_field",
                )
            }
        }
        composeRule.onNodeWithContentDescription("Leading").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Trailing").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Trailing").performClick()
        assertTrue(trailingClicked)
    }

    @Test
    fun heightIsShorterThanLegacyOutlinedTextField() {
        composeRule.setContent {
            UltimateFocusTheme {
                MinimalTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = "Filter apps",
                    testTag = "size_target",
                    modifier = Modifier.testTag("size_target_outer"),
                )
            }
        }
        // OutlinedTextField imposes a 56 dp internal min-height. The
        // MinimalTextField's heightIn(min = 32.dp) means the rendered field
        // should report a measured height materially under 56 dp at rest.
        val bounds = composeRule.onNodeWithTag("size_target").fetchSemanticsNode().size
        val density = composeRule.density
        val heightDp = with(density) { bounds.height.toDp() }
        assertTrue(
            "expected MinimalTextField height < 56.dp, was $heightDp",
            heightDp < 56.dp,
        )
    }
}
