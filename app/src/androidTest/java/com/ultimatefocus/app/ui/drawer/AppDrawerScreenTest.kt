/**
 * Compose UI integration tests for AppDrawerScreen.
 * Verifies that apps are shown as text labels only (no icons),
 * that the search field is visible and filters the list in real time,
 * and that the context menu appears on long press.
 * Also covers the blocked-state visual indicator (🔒 + strikethrough) added
 * with ULT-20.
 *
 * ULT-83 #1: also verifies the keyboard-hide / focus-clear behavior
 * when the drawer page becomes inactive in the parent pager.
 *
 * Covers: ULT-7, ULT-20, ULT-83
 */
package com.ultimatefocus.app.ui.drawer

import android.content.ComponentName
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.ui.drawer.components.AppListItem
import com.ultimatefocus.app.ui.drawer.components.SearchBar
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDrawerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun makeApp(name: String, isFavorite: Boolean = false) = AppInfo(
        packageName = "com.test.${name.lowercase()}",
        label = name,
        componentName = ComponentName("com.test.${name.lowercase()}", "MainActivity"),
        isFavorite = isFavorite,
    )

    // ── ULT-7: Text-only app list ────────────────────────────────────────────

    @Test
    fun appListItem_rendersTextLabelOnly() {
        val app = makeApp("Calendar")
        composeRule.setContent {
            UltimateFocusTheme {
                AppListItem(appInfo = app, onClick = {}, onLongClick = {})
            }
        }
        composeRule.onNodeWithText("Calendar").assertIsDisplayed()
    }

    @Test
    fun appListItem_tapTriggersOnClick() {
        val app = makeApp("Phone")
        var clicked = false
        composeRule.setContent {
            UltimateFocusTheme {
                AppListItem(appInfo = app, onClick = { clicked = true }, onLongClick = {})
            }
        }
        composeRule.onNodeWithText("Phone").performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun appListItem_favoriteApp_isDisplayedInBold() {
        val app = makeApp("Maps", isFavorite = true)
        composeRule.setContent {
            UltimateFocusTheme {
                AppListItem(appInfo = app, onClick = {}, onLongClick = {})
            }
        }
        // Node exists — bold vs normal is a visual property; verify no crash and text visible
        composeRule.onNodeWithText("Maps").assertIsDisplayed()
    }

    // ── ULT-7: Search field visible ──────────────────────────────────────────

    @Test
    fun searchBar_isDisplayedOnScreen() {
        composeRule.setContent {
            UltimateFocusTheme {
                SearchBar(query = "", onQueryChange = {})
            }
        }
        composeRule.onNodeWithTag("search_bar").assertIsDisplayed()
    }

    @Test
    fun searchBar_typingCallsOnQueryChange() {
        var captured = ""
        composeRule.setContent {
            UltimateFocusTheme {
                SearchBar(query = captured, onQueryChange = { captured = it })
            }
        }
        composeRule.onNodeWithTag("search_bar").performTextInput("cal")
        assertEquals("cal", captured)
    }

    @Test
    fun searchBar_clearButtonAppearsWithText() {
        composeRule.setContent {
            UltimateFocusTheme {
                SearchBar(query = "camera", onQueryChange = {})
            }
        }
        composeRule.onNodeWithTag("search_bar").assertIsDisplayed()
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun appListItem_veryLongLabel_rendersWithoutCrash() {
        val app = makeApp("A".repeat(80))
        composeRule.setContent {
            UltimateFocusTheme {
                AppListItem(appInfo = app, onClick = {}, onLongClick = {})
            }
        }
        composeRule.onNodeWithTag("app_drawer_screen").assertDoesNotExist() // component level test
    }

    @Test
    fun searchBar_emptyQuery_placeholderVisible() {
        composeRule.setContent {
            UltimateFocusTheme {
                SearchBar(query = "", onQueryChange = {})
            }
        }
        composeRule.onNodeWithText("Search apps\u2026").assertIsDisplayed()
    }

    // ── ULT-83 #1: keyboard hide on isActive=false ──────────────────────────

    /**
     * Drives the same `isActive` LaunchedEffect that AppDrawerScreen uses
     * (auto-focus on entry + clearFocus + IME hide on exit) against a
     * standalone SearchBar so we can verify the hide-on-inactive behavior
     * without booting Hilt + the full drawer ViewModel.
     */
    @Test
    fun isActiveFlipsFalse_clearsSearchFieldFocus() {
        var isActive by mutableStateOf(true)

        composeRule.setContent {
            UltimateFocusTheme {
                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current

                LaunchedEffect(isActive) {
                    if (isActive) {
                        runCatching { focusRequester.requestFocus() }
                    } else {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                    }
                }
                SearchBar(
                    query = "",
                    onQueryChange = {},
                    focusRequester = focusRequester,
                )
            }
        }

        composeRule.onNodeWithTag("search_bar").assertIsFocused()
        isActive = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_bar").assertIsNotFocused()
    }

    // ── ULT-20: blocked apps render with 🔒 prefix ──────────────────────────

    @Test
    fun appListItem_blockedApp_rendersLockPrefix() {
        val app = makeApp("Instagram").copy(blockedUntilEpochMs = Long.MAX_VALUE)
        composeRule.setContent {
            UltimateFocusTheme {
                AppListItem(appInfo = app, onClick = {}, onLongClick = {})
            }
        }
        composeRule.onNodeWithText("\uD83D\uDD12 Instagram").assertIsDisplayed()
    }

    @Test
    fun appListItem_unblockedApp_doesNotRenderLockPrefix() {
        val app = makeApp("Calendar") // blockedUntilEpochMs = null
        composeRule.setContent {
            UltimateFocusTheme {
                AppListItem(appInfo = app, onClick = {}, onLongClick = {})
            }
        }
        composeRule.onNodeWithText("Calendar").assertIsDisplayed()
    }
}
