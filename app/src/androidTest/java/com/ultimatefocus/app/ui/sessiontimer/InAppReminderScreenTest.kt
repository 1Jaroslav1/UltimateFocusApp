/**
 * Compose UI tests for InAppReminderContent. Drives the stateless variant
 * directly so the test does not need Hilt or a real DataStore. Verifies the
 * acceptance criteria from ULT-76:
 *  - master switch toggles emit through the supplied callback
 *  - precision row tap propagates only when master is ON
 *  - mindful-launch-delay switch is disabled when master is OFF
 *  - radio group reflects the supplied selection and emits new selections
 *  - selecting a radio is no-op when master is OFF (greyed-out enforcement)
 *
 * Covers: ULT-76
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.InAppReminderSettings
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InAppReminderScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screen_displaysWhenRendered() {
        composeRule.setContent {
            UltimateFocusTheme {
                InAppReminderContent(
                    settings = InAppReminderSettings(),
                    onNavigateBack = {},
                    onSetMaster = {},
                    onTogglePrecision = {},
                    onSetMindfulLaunchDelay = {},
                    onSelectExpiryMode = {},
                )
            }
        }
        composeRule.onNodeWithTag("in_app_reminder_screen").assertIsDisplayed()
    }

    @Test
    fun masterSwitch_callbackFiresOnTap() {
        var newValue: Boolean? = null
        composeRule.setContent {
            UltimateFocusTheme {
                InAppReminderContent(
                    settings = InAppReminderSettings(masterEnabled = false),
                    onNavigateBack = {},
                    onSetMaster = { newValue = it },
                    onTogglePrecision = {},
                    onSetMindfulLaunchDelay = {},
                    onSelectExpiryMode = {},
                )
            }
        }
        composeRule.onNodeWithTag("in_app_reminder_master_switch").assertIsOff().performClick()
        assertEquals(true, newValue)
    }

    @Test
    fun mindfulDelaySwitch_isDisabledWhenMasterOff() {
        composeRule.setContent {
            UltimateFocusTheme {
                InAppReminderContent(
                    settings = InAppReminderSettings(masterEnabled = false),
                    onNavigateBack = {},
                    onSetMaster = {},
                    onTogglePrecision = {},
                    onSetMindfulLaunchDelay = {},
                    onSelectExpiryMode = {},
                )
            }
        }
        composeRule.onNodeWithTag("in_app_reminder_mindful_delay_switch")
            .assertIsNotEnabled()
    }

    @Test
    fun mindfulDelaySwitch_isEnabledAndReflectsStateWhenMasterOn() {
        var emitted: Boolean? = null
        composeRule.setContent {
            UltimateFocusTheme {
                InAppReminderContent(
                    settings = InAppReminderSettings(
                        masterEnabled = true,
                        mindfulLaunchDelay = true,
                    ),
                    onNavigateBack = {},
                    onSetMaster = {},
                    onTogglePrecision = {},
                    onSetMindfulLaunchDelay = { emitted = it },
                    onSelectExpiryMode = {},
                )
            }
        }
        composeRule.onNodeWithTag("in_app_reminder_mindful_delay_switch")
            .assertIsOn()
            .performClick()
        assertEquals(false, emitted)
    }

    @Test
    fun precisionRow_tapsAreIgnoredWhenMasterOff() {
        var fired = false
        composeRule.setContent {
            UltimateFocusTheme {
                InAppReminderContent(
                    settings = InAppReminderSettings(masterEnabled = false),
                    onNavigateBack = {},
                    onSetMaster = {},
                    onTogglePrecision = { fired = true },
                    onSetMindfulLaunchDelay = {},
                    onSelectExpiryMode = {},
                )
            }
        }
        composeRule.onNodeWithTag("in_app_reminder_precision_row").performClick()
        assertFalse(fired)
    }

    @Test
    fun precisionRow_emitsToggleCallbackWhenMasterOn() {
        var fired = false
        composeRule.setContent {
            UltimateFocusTheme {
                InAppReminderContent(
                    settings = InAppReminderSettings(masterEnabled = true),
                    onNavigateBack = {},
                    onSetMaster = {},
                    onTogglePrecision = { fired = true },
                    onSetMindfulLaunchDelay = {},
                    onSelectExpiryMode = {},
                )
            }
        }
        composeRule.onNodeWithTag("in_app_reminder_precision_row").performClick()
        assertTrue(fired)
    }

    @Test
    fun expiryModeRadio_emitsSelectedWhenMasterOn() {
        var selected: SessionExpiryMode? = null
        composeRule.setContent {
            UltimateFocusTheme {
                InAppReminderContent(
                    settings = InAppReminderSettings(
                        masterEnabled = true,
                        expiryMode = SessionExpiryMode.ONLY_REMINDER,
                    ),
                    onNavigateBack = {},
                    onSetMaster = {},
                    onTogglePrecision = {},
                    onSetMindfulLaunchDelay = {},
                    onSelectExpiryMode = { selected = it },
                )
            }
        }
        composeRule.onNodeWithTag(
            "in_app_reminder_mode_row_${SessionExpiryMode.EXIT_AND_BLOCK.name}",
        ).performClick()
        assertEquals(SessionExpiryMode.EXIT_AND_BLOCK, selected)
    }

    @Test
    fun expiryModeRadio_doesNotEmitWhenMasterOff() {
        var selected: SessionExpiryMode? = null
        composeRule.setContent {
            UltimateFocusTheme {
                InAppReminderContent(
                    settings = InAppReminderSettings(masterEnabled = false),
                    onNavigateBack = {},
                    onSetMaster = {},
                    onTogglePrecision = {},
                    onSetMindfulLaunchDelay = {},
                    onSelectExpiryMode = { selected = it },
                )
            }
        }
        composeRule.onNodeWithTag(
            "in_app_reminder_mode_row_${SessionExpiryMode.EXIT_APP.name}",
        ).performClick()
        assertNull(selected)
    }
}
