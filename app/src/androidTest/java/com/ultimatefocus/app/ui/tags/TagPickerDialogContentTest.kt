/**
 * Compose UI tests for the stateless TagPickerDialogContent. Constructs a
 * TagPickerUiState manually so each scenario can be exercised without Hilt
 * or Room. Verifies: empty-state hint with "Manage tags" shortcut, single-app
 * checkbox renders, bulk tri-state renders for SomeAssigned, disabled rows
 * show explanatory message, Done button fires onDismiss.
 * Covers: ULT-73
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagPickerDialogContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun empty_state_offers_manage_tags_shortcut() {
        var manageTagsClicked = false
        composeRule.setContent {
            UltimateFocusTheme {
                TagPickerDialogContent(
                    state = TagPickerUiState(
                        items = emptyList(),
                        packagesUnderEdit = listOf("com.app"),
                        isLoading = false,
                    ),
                    onToggle = {},
                    onManageTags = { manageTagsClicked = true },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("tag_picker_empty_state").assertIsDisplayed()
        composeRule.onNodeWithTag("tag_picker_manage_tags_button").performClick()
        assertTrue(manageTagsClicked)
    }

    @Test
    fun single_app_shows_simple_checkboxes_and_emits_toggle() {
        val tag = Tag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        var toggled: TagPickerItem? = null
        composeRule.setContent {
            UltimateFocusTheme {
                TagPickerDialogContent(
                    state = TagPickerUiState(
                        items = listOf(
                            TagPickerItem(tag = tag, state = TagAssignmentState.NoneAssigned, disabled = false),
                        ),
                        packagesUnderEdit = listOf("com.app"),
                        isLoading = false,
                    ),
                    onToggle = { toggled = it },
                    onManageTags = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("tag_picker_check_${tag.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("tag_picker_check_${tag.id}").performClick()
        assertEquals(tag.id, toggled?.tag?.id)
    }

    @Test
    fun bulk_mode_renders_tri_state_checkboxes() {
        val tag = Tag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        composeRule.setContent {
            UltimateFocusTheme {
                TagPickerDialogContent(
                    state = TagPickerUiState(
                        items = listOf(
                            TagPickerItem(tag = tag, state = TagAssignmentState.SomeAssigned, disabled = false),
                        ),
                        packagesUnderEdit = listOf("com.a", "com.b"),
                        isLoading = false,
                    ),
                    onToggle = {},
                    onManageTags = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("tag_picker_tristate_${tag.id}").assertIsDisplayed()
    }

    @Test
    fun disabled_row_shows_explanatory_message() {
        val tag = Tag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)
        composeRule.setContent {
            UltimateFocusTheme {
                TagPickerDialogContent(
                    state = TagPickerUiState(
                        items = listOf(
                            TagPickerItem(tag = tag, state = TagAssignmentState.NoneAssigned, disabled = true),
                        ),
                        packagesUnderEdit = listOf("com.app"),
                        isLoading = false,
                    ),
                    onToggle = {},
                    onManageTags = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("tag_picker_disabled_message_${tag.id}").assertIsDisplayed()
    }

    @Test
    fun done_button_dismisses() {
        var dismissed = false
        composeRule.setContent {
            UltimateFocusTheme {
                TagPickerDialogContent(
                    state = TagPickerUiState(
                        items = emptyList(),
                        packagesUnderEdit = listOf("com.app"),
                        isLoading = false,
                    ),
                    onToggle = {},
                    onManageTags = {},
                    onDismiss = { dismissed = true },
                )
            }
        }
        composeRule.onNodeWithTag("tag_picker_done_button").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun bulk_title_announces_count() {
        composeRule.setContent {
            UltimateFocusTheme {
                TagPickerDialogContent(
                    state = TagPickerUiState(
                        items = emptyList(),
                        packagesUnderEdit = listOf("com.a", "com.b", "com.c"),
                        isLoading = false,
                    ),
                    onToggle = {},
                    onManageTags = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("Tags for 3 apps").assertIsDisplayed()
    }
}
