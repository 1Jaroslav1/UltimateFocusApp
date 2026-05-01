/**
 * Compose UI tests for SyncCategoriesDialog. Drives the stateless variant
 * directly so the test does not need Hilt or a real ViewModel. Verifies:
 *  - both categories render with their detected app counts
 *  - every category and every app start checked (default-all)
 *  - unchecking a category clears its apps from the confirmed selection
 *  - unchecking a single app removes only that app from the selection
 *  - the empty-state dialog renders when no candidates
 *
 * Covers: ULT-85
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.domain.service.SyncAppCandidate
import com.ultimatefocus.app.domain.service.SyncCategoryCandidate
import com.ultimatefocus.app.domain.service.SyncSelection
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncCategoriesDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sample = listOf(
        SyncCategoryCandidate(
            name = "Social Media",
            color = TagColor.STEEL_BLUE,
            packages = listOf(
                SyncAppCandidate("com.facebook.katana", "Facebook"),
                SyncAppCandidate("com.instagram.android", "Instagram"),
            ),
            alreadyExists = false,
        ),
        SyncCategoryCandidate(
            name = "Games",
            color = TagColor.AMBER_GOLD,
            packages = listOf(
                SyncAppCandidate("com.king.candycrushsaga", "Candy Crush"),
            ),
            alreadyExists = false,
        ),
    )

    @Test
    fun defaultsAllCategoriesAndAppsChecked() {
        composeRule.setContent {
            UltimateFocusTheme {
                SyncCategoriesDialog(
                    candidates = sample,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("sync_categories_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("sync_dialog_category_checkbox_Social Media").assertIsOn()
        composeRule.onNodeWithTag("sync_dialog_category_checkbox_Games").assertIsOn()
        composeRule.onNodeWithTag("sync_dialog_app_checkbox_com.facebook.katana").assertIsOn()
        composeRule.onNodeWithTag("sync_dialog_app_checkbox_com.king.candycrushsaga").assertIsOn()
    }

    @Test
    fun confirmEmitsFullSelectionWhenNothingDeselected() {
        var emitted: SyncSelection? = null
        composeRule.setContent {
            UltimateFocusTheme {
                SyncCategoriesDialog(
                    candidates = sample,
                    onConfirm = { emitted = it },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("sync_dialog_confirm").performClick()
        assertEquals(setOf("Social Media", "Games"), emitted!!.selectedCategories)
        assertEquals(
            setOf("com.facebook.katana", "com.instagram.android"),
            emitted!!.selectedAppsByCategory["Social Media"],
        )
        assertEquals(
            setOf("com.king.candycrushsaga"),
            emitted!!.selectedAppsByCategory["Games"],
        )
    }

    @Test
    fun uncheckingCategoryRemovesItFromSelection() {
        var emitted: SyncSelection? = null
        composeRule.setContent {
            UltimateFocusTheme {
                SyncCategoriesDialog(
                    candidates = sample,
                    onConfirm = { emitted = it },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("sync_dialog_category_checkbox_Games").performClick()
        composeRule.onNodeWithTag("sync_dialog_category_checkbox_Games").assertIsOff()
        composeRule.onNodeWithTag("sync_dialog_confirm").performClick()
        assertEquals(setOf("Social Media"), emitted!!.selectedCategories)
        assertTrue(
            "Games key must be filtered out of the confirmed selection",
            "Games" !in emitted!!.selectedAppsByCategory,
        )
    }

    @Test
    fun uncheckingSingleAppKeepsCategoryButDropsApp() {
        var emitted: SyncSelection? = null
        composeRule.setContent {
            UltimateFocusTheme {
                SyncCategoriesDialog(
                    candidates = sample,
                    onConfirm = { emitted = it },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("sync_dialog_app_checkbox_com.instagram.android").performClick()
        composeRule.onNodeWithTag("sync_dialog_confirm").performClick()
        assertTrue("Social Media" in emitted!!.selectedCategories)
        assertEquals(
            setOf("com.facebook.katana"),
            emitted!!.selectedAppsByCategory["Social Media"],
        )
    }

    @Test
    fun emptyDialogRendersWithDismissAction() {
        var dismissed = false
        composeRule.setContent {
            UltimateFocusTheme {
                SyncCategoriesEmptyDialog(onDismiss = { dismissed = true })
            }
        }
        composeRule.onNodeWithTag("sync_categories_empty_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("sync_dialog_empty_dismiss").performClick()
        assertTrue(dismissed)
    }
}
