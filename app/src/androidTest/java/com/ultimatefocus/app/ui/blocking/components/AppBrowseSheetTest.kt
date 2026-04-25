/**
 * Compose UI tests for AppBrowseSheet — the modal browser opened from the
 * editor's "Browse all apps" row. Verifies the filter input narrows the
 * visible rows, the empty-state surfaces when nothing matches, and tapping
 * a row fires the toggle callback.
 * Covers: ULT-24 (iteration)
 */
package com.ultimatefocus.app.ui.blocking.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.blocking.AppPickerItem
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppBrowseSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val items = listOf(
        AppPickerItem("com.alpha", "Alpha", 100L),
        AppPickerItem("com.beta", "Beta", 80L),
        AppPickerItem("com.charlie", "Charlie", 60L),
    )

    @Test
    fun rendersAllItems_initially() {
        composeRule.setContent {
            UltimateFocusTheme {
                AppBrowseSheet(items = items, selected = emptySet(), onToggle = {}, onDismiss = {})
            }
        }
        composeRule.onNodeWithTag("schedule_app_row_com.alpha").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_app_row_com.beta").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_app_row_com.charlie").assertIsDisplayed()
    }

    @Test
    fun filterInput_narrowsVisibleRows_caseInsensitive() {
        composeRule.setContent {
            UltimateFocusTheme {
                AppBrowseSheet(items = items, selected = emptySet(), onToggle = {}, onDismiss = {})
            }
        }
        composeRule.onNodeWithTag("schedule_apps_filter_field").performTextInput("BET")
        composeRule.onNodeWithTag("schedule_app_row_com.beta").assertIsDisplayed()
        composeRule.onAllNodesWithTag("schedule_app_row_com.alpha").assertCountEquals(0)
        composeRule.onAllNodesWithTag("schedule_app_row_com.charlie").assertCountEquals(0)
    }

    @Test
    fun filterInput_noMatch_showsEmptyState() {
        composeRule.setContent {
            UltimateFocusTheme {
                AppBrowseSheet(items = items, selected = emptySet(), onToggle = {}, onDismiss = {})
            }
        }
        composeRule.onNodeWithTag("schedule_apps_filter_field").performTextInput("zzz")
        composeRule.onNodeWithTag("schedule_apps_browse_empty").assertIsDisplayed()
    }

    @Test
    fun rowClick_firesToggleCallback() {
        val toggled = mutableListOf<String>()
        composeRule.setContent {
            UltimateFocusTheme {
                AppBrowseSheet(
                    items = items,
                    selected = emptySet(),
                    onToggle = { toggled += it },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("schedule_app_row_com.beta").performClick()
        assertEquals(listOf("com.beta"), toggled)
    }

    @Test
    fun doneButton_invokesDismissCallback() {
        var dismissed = false
        composeRule.setContent {
            UltimateFocusTheme {
                AppBrowseSheet(
                    items = items,
                    selected = emptySet(),
                    onToggle = {},
                    onDismiss = { dismissed = true },
                )
            }
        }
        composeRule.onNodeWithTag("schedule_apps_done_button").performClick()
        assertTrue(dismissed)
    }
}
