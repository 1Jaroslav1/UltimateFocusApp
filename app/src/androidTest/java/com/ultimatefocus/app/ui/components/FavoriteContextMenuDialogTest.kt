/**
 * Compose UI integration tests for FavoriteContextMenuDialog.
 * Verifies the minimalist context menu renders app name, toggle-favorite
 * button, and app-info button correctly. Covers happy paths and border cases.
 * Covers: ULT-6
 */
package com.ultimatefocus.app.ui.components

import android.content.ComponentName
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteContextMenuDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun makeApp(name: String, isFavorite: Boolean = false) = AppInfo(
        packageName = "com.test.${name.lowercase()}",
        label = name,
        componentName = ComponentName("com.test.${name.lowercase()}", "MainActivity"),
        isFavorite = isFavorite,
    )

    // ── Happy paths ─────────────────────────────────────────────────────────

    @Test
    fun dialog_displaysAppName() {
        composeRule.setContent {
            UltimateFocusTheme {
                FavoriteContextMenuDialog(
                    app = makeApp("Camera"),
                    onToggleFavorite = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("context_menu_app_name").assertIsDisplayed()
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
    }

    @Test
    fun dialog_showsAddToFavorites_whenAppIsNotFavorite() {
        composeRule.setContent {
            UltimateFocusTheme {
                FavoriteContextMenuDialog(
                    app = makeApp("Maps", isFavorite = false),
                    onToggleFavorite = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("Add to Favorites").assertIsDisplayed()
    }

    @Test
    fun dialog_showsRemoveFromFavorites_whenAppIsFavorite() {
        composeRule.setContent {
            UltimateFocusTheme {
                FavoriteContextMenuDialog(
                    app = makeApp("Slack", isFavorite = true),
                    onToggleFavorite = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("Remove from Favorites").assertIsDisplayed()
    }

    @Test
    fun dialog_showsAppInfoButton() {
        composeRule.setContent {
            UltimateFocusTheme {
                FavoriteContextMenuDialog(
                    app = makeApp("Phone"),
                    onToggleFavorite = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("App Info").assertIsDisplayed()
    }

    @Test
    fun tappingToggleFavorite_triggersCallback() {
        var toggledApp: AppInfo? = null
        val app = makeApp("Notes")
        composeRule.setContent {
            UltimateFocusTheme {
                FavoriteContextMenuDialog(
                    app = app,
                    onToggleFavorite = { toggledApp = it },
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("context_menu_toggle_favorite").performClick()
        assertEquals("com.test.notes", toggledApp?.packageName)
    }

    @Test
    fun tappingAppInfo_triggersCallback() {
        var infoApp: AppInfo? = null
        val app = makeApp("Clock")
        composeRule.setContent {
            UltimateFocusTheme {
                FavoriteContextMenuDialog(
                    app = app,
                    onToggleFavorite = {},
                    onAppInfo = { infoApp = it },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("context_menu_app_info").performClick()
        assertEquals("com.test.clock", infoApp?.packageName)
    }

    @Test
    fun tappingToggleFavorite_callsDismiss() {
        var dismissed = false
        composeRule.setContent {
            UltimateFocusTheme {
                FavoriteContextMenuDialog(
                    app = makeApp("Browser"),
                    onToggleFavorite = {},
                    onAppInfo = {},
                    onDismiss = { dismissed = true },
                )
            }
        }
        composeRule.onNodeWithTag("context_menu_toggle_favorite").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun tappingAppInfo_callsDismiss() {
        var dismissed = false
        composeRule.setContent {
            UltimateFocusTheme {
                FavoriteContextMenuDialog(
                    app = makeApp("Files"),
                    onToggleFavorite = {},
                    onAppInfo = {},
                    onDismiss = { dismissed = true },
                )
            }
        }
        composeRule.onNodeWithTag("context_menu_app_info").performClick()
        assertTrue(dismissed)
    }

    // ── Border cases ────────────────────────────────────────────────────────

    @Test
    fun dialog_veryLongAppName_rendersWithoutCrash() {
        composeRule.setContent {
            UltimateFocusTheme {
                FavoriteContextMenuDialog(
                    app = makeApp("A".repeat(80)),
                    onToggleFavorite = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("context_menu_dialog").assertIsDisplayed()
    }

    @Test
    fun dialog_singleCharacterAppName_rendersCorrectly() {
        composeRule.setContent {
            UltimateFocusTheme {
                FavoriteContextMenuDialog(
                    app = makeApp("X"),
                    onToggleFavorite = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("X").assertIsDisplayed()
        composeRule.onNodeWithText("Add to Favorites").assertIsDisplayed()
        composeRule.onNodeWithText("App Info").assertIsDisplayed()
    }
}
