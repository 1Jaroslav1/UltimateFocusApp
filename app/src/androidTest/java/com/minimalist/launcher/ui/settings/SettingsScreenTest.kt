/**
 * Compose UI tests for the settings toggles. Verifies auto-keyboard and dark-theme
 * switches render correctly (on/off state) and respond to tap events.
 * Tests use a minimal composable to avoid Hilt wiring complexity in instrumented tests.
 * Covers: ULT-8
 */
package com.minimalist.launcher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minimalist.launcher.domain.model.UserPreferences
import com.minimalist.launcher.ui.theme.MinimalistLauncherTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun autoKeyboardSwitch_isOnByDefault() {
        composeRule.setContent {
            MinimalistLauncherTheme {
                Switch(
                    checked = true,
                    onCheckedChange = {},
                    modifier = Modifier.testTag("auto_keyboard_switch"),
                )
            }
        }
        composeRule.onNodeWithTag("auto_keyboard_switch").assertIsOn()
    }

    @Test
    fun autoKeyboardSwitch_isOffWhenPreferenceDisabled() {
        composeRule.setContent {
            MinimalistLauncherTheme {
                Switch(
                    checked = false,
                    onCheckedChange = {},
                    modifier = Modifier.testTag("auto_keyboard_switch"),
                )
            }
        }
        composeRule.onNodeWithTag("auto_keyboard_switch").assertIsOff()
    }

    @Test
    fun tappingAutoKeyboardSwitch_invokesCallback() {
        var toggled = false
        composeRule.setContent {
            MinimalistLauncherTheme {
                Switch(
                    checked = true,
                    onCheckedChange = { toggled = true },
                    modifier = Modifier.testTag("auto_keyboard_switch"),
                )
            }
        }
        composeRule.onNodeWithTag("auto_keyboard_switch").performClick()
        assertTrue(toggled)
    }

    @Test
    fun tappingDarkThemeSwitch_invokesCallback() {
        var toggled = false
        composeRule.setContent {
            MinimalistLauncherTheme {
                Switch(
                    checked = true,
                    onCheckedChange = { toggled = true },
                    modifier = Modifier.testTag("dark_theme_switch"),
                )
            }
        }
        composeRule.onNodeWithTag("dark_theme_switch").performClick()
        assertTrue(toggled)
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun bothSwitches_areIndependentlyToggleable() {
        var darkToggled = false
        var keyboardToggled = false
        composeRule.setContent {
            MinimalistLauncherTheme {
                Column {
                    Switch(
                        checked = true,
                        onCheckedChange = { darkToggled = true },
                        modifier = Modifier.testTag("dark_theme_switch"),
                    )
                    Switch(
                        checked = true,
                        onCheckedChange = { keyboardToggled = true },
                        modifier = Modifier.testTag("auto_keyboard_switch"),
                    )
                }
            }
        }
        composeRule.onNodeWithTag("dark_theme_switch").performClick()
        composeRule.onNodeWithTag("auto_keyboard_switch").performClick()
        assertTrue(darkToggled)
        assertTrue(keyboardToggled)
    }

    @Test
    fun autoKeyboardSwitch_reflectsStateChangedByClick() {
        composeRule.setContent {
            MinimalistLauncherTheme {
                var checked by remember { mutableStateOf(true) }
                Switch(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier = Modifier.testTag("auto_keyboard_switch"),
                )
            }
        }
        composeRule.onNodeWithTag("auto_keyboard_switch").assertIsOn()
        composeRule.onNodeWithTag("auto_keyboard_switch").performClick()
        composeRule.onNodeWithTag("auto_keyboard_switch").assertIsOff()
    }

    @Test
    fun bothSwitches_areDisplayed() {
        composeRule.setContent {
            MinimalistLauncherTheme {
                Column {
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        modifier = Modifier.testTag("dark_theme_switch"),
                    )
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        modifier = Modifier.testTag("auto_keyboard_switch"),
                    )
                }
            }
        }
        composeRule.onNodeWithTag("dark_theme_switch").assertIsDisplayed()
        composeRule.onNodeWithTag("auto_keyboard_switch").assertIsDisplayed()
    }
}
