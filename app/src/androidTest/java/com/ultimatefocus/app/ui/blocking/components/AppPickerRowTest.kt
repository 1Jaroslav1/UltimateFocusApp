/**
 * Compose UI tests for AppPickerRow. Verifies row tap toggles selection,
 * and the trailing usage label renders the formatted weekly usage time.
 * Covers: ULT-24 (iteration)
 */
package com.ultimatefocus.app.ui.blocking.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.blocking.AppPickerItem
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppPickerRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersLabelAndUsage() {
        composeRule.setContent {
            UltimateFocusTheme {
                AppPickerRow(
                    item = AppPickerItem(
                        packageName = "com.a",
                        label = "Alpha",
                        weeklyUsageMillis = 3L * 3_600_000L + 12L * 60_000L,
                    ),
                    isSelected = false,
                    onToggle = {},
                )
            }
        }
        composeRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_app_usage_com.a").assertIsDisplayed()
        composeRule.onNodeWithText("3h 12m").assertIsDisplayed()
    }

    @Test
    fun zeroUsage_rendersEmDash() {
        composeRule.setContent {
            UltimateFocusTheme {
                AppPickerRow(
                    item = AppPickerItem(
                        packageName = "com.a",
                        label = "Alpha",
                        weeklyUsageMillis = 0L,
                    ),
                    isSelected = false,
                    onToggle = {},
                )
            }
        }
        composeRule.onNodeWithText("—").assertIsDisplayed()
    }

    @Test
    fun rowClick_invokesToggleWithPackageName() {
        val toggled = mutableListOf<String>()
        composeRule.setContent {
            UltimateFocusTheme {
                AppPickerRow(
                    item = AppPickerItem("com.a", "Alpha", 0L),
                    isSelected = false,
                    onToggle = { toggled += it },
                )
            }
        }
        composeRule.onNodeWithTag("schedule_app_row_com.a").performClick()
        assertEquals(listOf("com.a"), toggled)
    }
}
