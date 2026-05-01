/**
 * Compose UI tests for InAppReminderContent. Drives the stateless variant
 * directly so the test does not need Hilt or a real DataStore. Verifies the
 * acceptance criteria from ULT-76 + ULT-77 plus the ULT-82 #4 additions:
 *  - master switch toggles emit through the supplied callback
 *  - precision row tap propagates only when master is ON
 *  - mindful-launch-delay switch is disabled when master is OFF
 *  - radio group reflects the supplied selection and emits new selections
 *  - per-app rows render label + Switch and emit toggle callback
 *  - per-app rows render the trailing weekly-usage label (ULT-82 #4)
 *  - per-app Switches are disabled when master is OFF
 *  - search filter empty state shows the documented copy
 *
 * Covers: ULT-76, ULT-77, ULT-82
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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

    private fun render(
        state: InAppReminderUiState,
        onSetMaster: (Boolean) -> Unit = {},
        onTogglePrecision: () -> Unit = {},
        onSetMindfulLaunchDelay: (Boolean) -> Unit = {},
        onSelectExpiryMode: (SessionExpiryMode) -> Unit = {},
        onQueryChange: (String) -> Unit = {},
        onAppToggle: (String, Boolean) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            UltimateFocusTheme {
                InAppReminderContent(
                    state = state,
                    onNavigateBack = {},
                    onSetMaster = onSetMaster,
                    onTogglePrecision = onTogglePrecision,
                    onSetMindfulLaunchDelay = onSetMindfulLaunchDelay,
                    onSelectExpiryMode = onSelectExpiryMode,
                    onQueryChange = onQueryChange,
                    onAppToggle = onAppToggle,
                )
            }
        }
    }

    @Test
    fun screen_displaysWhenRendered() {
        render(InAppReminderUiState())
        composeRule.onNodeWithTag("in_app_reminder_screen").assertIsDisplayed()
    }

    @Test
    fun masterSwitch_callbackFiresOnTap() {
        var newValue: Boolean? = null
        render(state = InAppReminderUiState(), onSetMaster = { newValue = it })
        composeRule.onNodeWithTag("in_app_reminder_master_switch").assertIsOff().performClick()
        assertEquals(true, newValue)
    }

    @Test
    fun mindfulDelaySwitch_isDisabledWhenMasterOff() {
        render(state = InAppReminderUiState(settings = InAppReminderSettings(masterEnabled = false)))
        composeRule.onNodeWithTag("in_app_reminder_mindful_delay_switch")
            .assertIsNotEnabled()
    }

    @Test
    fun mindfulDelaySwitch_isEnabledAndReflectsStateWhenMasterOn() {
        var emitted: Boolean? = null
        render(
            state = InAppReminderUiState(
                settings = InAppReminderSettings(
                    masterEnabled = true,
                    mindfulLaunchDelay = true,
                ),
            ),
            onSetMindfulLaunchDelay = { emitted = it },
        )
        composeRule.onNodeWithTag("in_app_reminder_mindful_delay_switch")
            .assertIsOn()
            .performClick()
        assertEquals(false, emitted)
    }

    @Test
    fun precisionRow_tapsAreIgnoredWhenMasterOff() {
        var fired = false
        render(
            state = InAppReminderUiState(),
            onTogglePrecision = { fired = true },
        )
        composeRule.onNodeWithTag("in_app_reminder_precision_row").performClick()
        assertFalse(fired)
    }

    @Test
    fun precisionRow_emitsToggleCallbackWhenMasterOn() {
        var fired = false
        render(
            state = InAppReminderUiState(settings = InAppReminderSettings(masterEnabled = true)),
            onTogglePrecision = { fired = true },
        )
        composeRule.onNodeWithTag("in_app_reminder_precision_row").performClick()
        assertTrue(fired)
    }

    @Test
    fun expiryModeRadio_emitsSelectedWhenMasterOn() {
        var selected: SessionExpiryMode? = null
        render(
            state = InAppReminderUiState(
                settings = InAppReminderSettings(
                    masterEnabled = true,
                    expiryMode = SessionExpiryMode.ONLY_REMINDER,
                ),
            ),
            onSelectExpiryMode = { selected = it },
        )
        composeRule.onNodeWithTag(
            "in_app_reminder_mode_row_${SessionExpiryMode.EXIT_AND_BLOCK.name}",
        ).performClick()
        assertEquals(SessionExpiryMode.EXIT_AND_BLOCK, selected)
    }

    @Test
    fun expiryModeRadio_doesNotEmitWhenMasterOff() {
        var selected: SessionExpiryMode? = null
        render(
            state = InAppReminderUiState(),
            onSelectExpiryMode = { selected = it },
        )
        composeRule.onNodeWithTag(
            "in_app_reminder_mode_row_${SessionExpiryMode.EXIT_APP.name}",
        ).performClick()
        assertNull(selected)
    }

    // ── ULT-77 per-app list ──────────────────────────────────────────────────

    @Test
    fun appList_rendersRowsForEachVisibleApp() {
        render(
            state = InAppReminderUiState(
                settings = InAppReminderSettings(masterEnabled = true),
                visibleApps = listOf(
                    AppToggleRow("com.alpha", "Alpha", enabled = false),
                    AppToggleRow("com.bravo", "Bravo", enabled = true),
                ),
                totalAppCount = 2,
            ),
        )
        composeRule.onNodeWithTag("in_app_reminder_app_row_com.alpha").assertIsDisplayed()
        composeRule.onNodeWithTag("in_app_reminder_app_row_com.bravo").assertIsDisplayed()
        composeRule.onNodeWithTag("in_app_reminder_app_switch_com.bravo").assertIsOn()
        composeRule.onNodeWithTag("in_app_reminder_app_switch_com.alpha").assertIsOff()
    }

    @Test
    fun appSwitch_emitsToggleCallback() {
        var captured: Pair<String, Boolean>? = null
        render(
            state = InAppReminderUiState(
                settings = InAppReminderSettings(masterEnabled = true),
                visibleApps = listOf(AppToggleRow("com.alpha", "Alpha", enabled = false)),
                totalAppCount = 1,
            ),
            onAppToggle = { pkg, enabled -> captured = pkg to enabled },
        )
        composeRule.onNodeWithTag("in_app_reminder_app_switch_com.alpha").performClick()
        assertEquals("com.alpha" to true, captured)
    }

    @Test
    fun appSwitch_isDisabledWhenMasterOff() {
        render(
            state = InAppReminderUiState(
                settings = InAppReminderSettings(masterEnabled = false),
                visibleApps = listOf(AppToggleRow("com.alpha", "Alpha", enabled = true)),
                totalAppCount = 1,
            ),
        )
        composeRule.onNodeWithTag("in_app_reminder_app_switch_com.alpha").assertIsNotEnabled()
    }

    @Test
    fun appList_emptyAfterFilter_showsEmptyStateCopy() {
        render(
            state = InAppReminderUiState(
                settings = InAppReminderSettings(masterEnabled = true),
                query = "nothing",
                visibleApps = emptyList(),
                totalAppCount = 5,
            ),
        )
        composeRule.onNodeWithTag("in_app_reminder_apps_empty").assertIsDisplayed()
    }

    @Test
    fun appRow_rendersWeeklyUsageLabel() {
        render(
            state = InAppReminderUiState(
                settings = InAppReminderSettings(masterEnabled = true),
                visibleApps = listOf(
                    AppToggleRow(
                        packageName = "com.alpha",
                        label = "Alpha",
                        enabled = false,
                        weeklyUsageMillis = 60L * 60 * 1000L,
                    ),
                ),
                totalAppCount = 1,
            ),
        )
        composeRule.onNodeWithTag("in_app_reminder_app_usage_com.alpha").assertIsDisplayed()
    }

    @Test
    fun filterField_emitsQueryChange() {
        var captured: String? = null
        render(
            state = InAppReminderUiState(
                settings = InAppReminderSettings(masterEnabled = true),
                visibleApps = emptyList(),
                totalAppCount = 1,
            ),
            onQueryChange = { captured = it },
        )
        composeRule.onNodeWithTag("in_app_reminder_filter_field").performTextInput("zoo")
        assertEquals("zoo", captured)
    }
}
