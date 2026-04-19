/**
 * Compose UI integration tests for QuickShortcutsBar.
 * Verifies default placeholder labels ("Phone", "Camera"), rendering of
 * resolved app labels, tap callbacks per slot, long-press callbacks per slot,
 * and that the bar stays displayed inside tight layouts (always-visible guarantee).
 * Covers: ULT-13, ULT-14
 */
package com.ultimatefocus.app.ui.home

import android.content.ComponentName
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.HomeAlignment
import com.ultimatefocus.app.domain.model.ShortcutSlot
import com.ultimatefocus.app.ui.home.components.FavoritesList
import com.ultimatefocus.app.ui.home.components.QuickShortcutsBar
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickShortcutsBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun makeApp(name: String) = AppInfo(
        packageName = "com.test.${name.lowercase()}",
        label = name,
        componentName = ComponentName("com.test.${name.lowercase()}", "MainActivity"),
    )

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun bar_rendersDefaultLabels_whenAppsAreNull() {
        composeRule.setContent {
            UltimateFocusTheme {
                QuickShortcutsBar(
                    leftApp = null,
                    rightApp = null,
                    onTap = {},
                    onLongPress = {},
                )
            }
        }
        composeRule.onNodeWithTag("quick_shortcuts_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Phone").assertIsDisplayed()
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
    }

    @Test
    fun bar_rendersAppLabels_whenAppsProvided() {
        composeRule.setContent {
            UltimateFocusTheme {
                QuickShortcutsBar(
                    leftApp = makeApp("Maps"),
                    rightApp = makeApp("Notes"),
                    onTap = {},
                    onLongPress = {},
                )
            }
        }
        composeRule.onNodeWithText("Maps").assertIsDisplayed()
        composeRule.onNodeWithText("Notes").assertIsDisplayed()
    }

    @Test
    fun tappingLeftSlot_invokesOnTap_withLEFT() {
        var tapped: ShortcutSlot? = null
        composeRule.setContent {
            UltimateFocusTheme {
                QuickShortcutsBar(
                    leftApp = makeApp("Maps"),
                    rightApp = makeApp("Notes"),
                    onTap = { tapped = it },
                    onLongPress = {},
                )
            }
        }
        composeRule.onNodeWithTag("shortcut_slot_left").performClick()
        assertEquals(ShortcutSlot.LEFT, tapped)
    }

    @Test
    fun tappingRightSlot_invokesOnTap_withRIGHT() {
        var tapped: ShortcutSlot? = null
        composeRule.setContent {
            UltimateFocusTheme {
                QuickShortcutsBar(
                    leftApp = makeApp("Maps"),
                    rightApp = makeApp("Notes"),
                    onTap = { tapped = it },
                    onLongPress = {},
                )
            }
        }
        composeRule.onNodeWithTag("shortcut_slot_right").performClick()
        assertEquals(ShortcutSlot.RIGHT, tapped)
    }

    @Test
    fun longPressingLeftSlot_invokesOnLongPress_withLEFT() {
        var longPressed: ShortcutSlot? = null
        composeRule.setContent {
            UltimateFocusTheme {
                QuickShortcutsBar(
                    leftApp = null,
                    rightApp = null,
                    onTap = {},
                    onLongPress = { longPressed = it },
                )
            }
        }
        composeRule.onNodeWithTag("shortcut_slot_left").performTouchInput { longClick() }
        assertEquals(ShortcutSlot.LEFT, longPressed)
    }

    @Test
    fun longPressingRightSlot_invokesOnLongPress_withRIGHT() {
        var longPressed: ShortcutSlot? = null
        composeRule.setContent {
            UltimateFocusTheme {
                QuickShortcutsBar(
                    leftApp = null,
                    rightApp = null,
                    onTap = {},
                    onLongPress = { longPressed = it },
                )
            }
        }
        composeRule.onNodeWithTag("shortcut_slot_right").performTouchInput { longClick() }
        assertEquals(ShortcutSlot.RIGHT, longPressed)
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun bar_staysVisible_whenFavoritesOverflow() {
        val favorites = (1..50).map {
            AppInfo(
                packageName = "com.test.app$it",
                label = "App$it",
                componentName = ComponentName("com.test.app$it", "Main"),
                isFavorite = true,
            )
        }
        composeRule.setContent {
            UltimateFocusTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .size(width = 320.dp, height = 240.dp),
                    ) {
                        FavoritesList(
                            favorites = favorites,
                            alignment = HomeAlignment.LEFT,
                            onAppClick = {},
                            onAppLongClick = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    QuickShortcutsBar(
                        leftApp = null,
                        rightApp = null,
                        onTap = {},
                        onLongPress = {},
                    )
                }
            }
        }
        composeRule.onNodeWithTag("quick_shortcuts_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Phone").assertIsDisplayed()
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
    }

    @Test
    fun tap_doesNotFire_onLongPress() {
        var tapped: ShortcutSlot? = null
        composeRule.setContent {
            UltimateFocusTheme {
                QuickShortcutsBar(
                    leftApp = null,
                    rightApp = null,
                    onTap = { tapped = it },
                    onLongPress = {},
                )
            }
        }
        composeRule.onNodeWithTag("shortcut_slot_left").performTouchInput { longClick() }
        assertNull(tapped)
    }

    @Test
    fun bar_rendersVeryLongLabel_withoutCrash() {
        val longName = "A".repeat(80)
        composeRule.setContent {
            UltimateFocusTheme {
                QuickShortcutsBar(
                    leftApp = AppInfo(
                        packageName = "com.test.long",
                        label = longName,
                        componentName = ComponentName("com.test.long", "Main"),
                    ),
                    rightApp = null,
                    onTap = {},
                    onLongPress = {},
                )
            }
        }
        composeRule.onNodeWithTag("quick_shortcuts_bar").assertIsDisplayed()
    }
}
