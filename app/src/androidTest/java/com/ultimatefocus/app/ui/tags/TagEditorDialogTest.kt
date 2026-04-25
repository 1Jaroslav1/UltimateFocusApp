/**
 * Compose UI tests for TagEditorDialog.
 * Verifies: name input, color swatch selection, swatches already used by
 * another tag are disabled, the editing tag's own color stays selectable,
 * Save is disabled until name+color valid, Save callback fires with the
 * trimmed name and selected color.
 * Covers: ULT-72
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagEditorDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun creating_save_disabled_until_name_and_color_chosen() {
        composeRule.setContent {
            UltimateFocusTheme {
                TagEditorDialog(
                    existing = null,
                    takenColors = emptySet(),
                    onSave = { _, _ -> },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("tag_editor_save_button").assertIsNotEnabled()

        composeRule.onNodeWithTag("tag_editor_name_field").performTextInput("Social")
        composeRule.onNodeWithTag("tag_editor_save_button").assertIsNotEnabled()

        composeRule.onNodeWithTag("tag_editor_swatch_${TagColor.STEEL_BLUE.name}").performClick()
        composeRule.onNodeWithTag("tag_editor_save_button").assertIsEnabled()
    }

    @Test
    fun creating_save_returns_trimmed_name_and_chosen_color() {
        var savedName: String? = null
        var savedColor: TagColor? = null
        composeRule.setContent {
            UltimateFocusTheme {
                TagEditorDialog(
                    existing = null,
                    takenColors = emptySet(),
                    onSave = { name, color -> savedName = name; savedColor = color },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("tag_editor_name_field").performTextInput("  Productivity  ")
        composeRule.onNodeWithTag("tag_editor_swatch_${TagColor.SAGE_GREEN.name}").performClick()
        composeRule.onNodeWithTag("tag_editor_save_button").performClick()

        assertEquals("Productivity", savedName)
        assertEquals(TagColor.SAGE_GREEN, savedColor)
    }

    @Test
    fun used_colors_are_rendered_disabled_and_not_selectable() {
        composeRule.setContent {
            UltimateFocusTheme {
                TagEditorDialog(
                    existing = null,
                    takenColors = setOf(TagColor.STEEL_BLUE, TagColor.AMBER_GOLD),
                    onSave = { _, _ -> },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("tag_editor_swatch_${TagColor.STEEL_BLUE.name}_disabled")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("tag_editor_swatch_${TagColor.AMBER_GOLD.name}_disabled")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("tag_editor_swatch_${TagColor.SAGE_GREEN.name}")
            .assertIsDisplayed()
    }

    @Test
    fun editing_existing_color_remains_selectable_even_when_in_taken_set() {
        // Editing the "Social Media" tag whose color is STEEL_BLUE.
        // takenColors includes STEEL_BLUE because the editing tag itself uses it
        // — but the swatch must be enabled so the user can keep their color.
        val existing = Tag(id = 7L, name = "Social Media", color = TagColor.STEEL_BLUE)
        composeRule.setContent {
            UltimateFocusTheme {
                TagEditorDialog(
                    existing = existing,
                    takenColors = setOf(TagColor.STEEL_BLUE, TagColor.SAGE_GREEN),
                    onSave = { _, _ -> },
                    onDismiss = {},
                )
            }
        }
        // STEEL_BLUE still selectable (it's the editing tag's own color)
        composeRule.onNodeWithTag("tag_editor_swatch_${TagColor.STEEL_BLUE.name}")
            .assertIsDisplayed()
        // SAGE_GREEN is taken by *another* tag — should be disabled
        composeRule.onNodeWithTag("tag_editor_swatch_${TagColor.SAGE_GREEN.name}_disabled")
            .assertIsDisplayed()
    }
}
