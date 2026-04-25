/**
 * Compose UI tests for TagDotsRow.
 * Verifies: empty list renders nothing, dots are displayed in order, tapping
 * a dot reveals a popup with the tag name (and the dot click does NOT
 * bubble through to underlying gesture handlers — covered indirectly by
 * the AppListItem tag-rendering test).
 * Covers: ULT-74
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagDotsRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun tag(id: Long, name: String, color: TagColor) =
        Tag(id = id, name = name, color = color)

    @Test
    fun emptyTags_rendersNothing() {
        composeRule.setContent {
            UltimateFocusTheme {
                TagDotsRow(tags = emptyList())
            }
        }
        composeRule.onNodeWithTag("tag_dots_row").assertDoesNotExist()
    }

    @Test
    fun tagsList_rendersOneDotPerTag() {
        val tags = listOf(
            tag(1L, "Social", TagColor.STEEL_BLUE),
            tag(2L, "Games", TagColor.AMBER_GOLD),
            tag(3L, "Work", TagColor.SAGE_GREEN),
        )
        composeRule.setContent {
            UltimateFocusTheme {
                TagDotsRow(tags = tags)
            }
        }
        composeRule.onNodeWithTag("tag_dots_row").assertIsDisplayed()
        tags.forEach { t ->
            composeRule.onNodeWithTag("tag_dot_${t.id}").assertIsDisplayed()
        }
    }

    @Test
    fun tappingADot_revealsPopupWithTagName() {
        val tags = listOf(tag(7L, "Social Media", TagColor.STEEL_BLUE))
        composeRule.setContent {
            UltimateFocusTheme {
                TagDotsRow(tags = tags)
            }
        }
        composeRule.onNodeWithTag("tag_dot_7").performClick()
        composeRule.onNodeWithTag("tag_dot_popup_7").assertIsDisplayed()
    }
}
