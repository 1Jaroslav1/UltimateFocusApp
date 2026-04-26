/**
 * Compose UI tests for SessionDurationPickerContent. Drives the stateless
 * variant directly so the test does not need Hilt or a live ViewModel.
 * Verifies the duration picker AC from ULT-78:
 *  - 4 preset buttons render and emit onConfirmPreset with the right value
 *  - the expand arrow toggles the panel (custom field + radios appear)
 *  - the custom-minutes Start button is disabled until the value is valid
 *  - the cancel button emits onDismiss
 *  - per-session expiry-mode override emits onSelectMode
 *
 * Covers: ULT-78
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDurationPickerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        state: SessionDurationPickerUiState =
            SessionDurationPickerUiState(packageName = "com.alpha", appLabel = "Alpha App"),
        onDismiss: () -> Unit = {},
        onConfirmPreset: (Int) -> Unit = {},
        onCustomMinutesChange: (String) -> Unit = {},
        onConfirmCustom: () -> Unit = {},
        onSelectMode: (SessionExpiryMode) -> Unit = {},
        onToggleExpanded: () -> Unit = {},
    ) {
        composeRule.setContent {
            UltimateFocusTheme {
                SessionDurationPickerContent(
                    state = state,
                    onDismiss = onDismiss,
                    onConfirmPreset = onConfirmPreset,
                    onCustomMinutesChange = onCustomMinutesChange,
                    onConfirmCustom = onConfirmCustom,
                    onSelectMode = onSelectMode,
                    onToggleExpanded = onToggleExpanded,
                )
            }
        }
    }

    @Test
    fun pickerDialog_isDisplayed() {
        render()
        composeRule.onNodeWithTag("session_duration_picker").assertIsDisplayed()
    }

    @Test
    fun presetButtons_rendersAllFour() {
        render()
        composeRule.onNodeWithTag("session_picker_preset_1").assertIsDisplayed()
        composeRule.onNodeWithTag("session_picker_preset_5").assertIsDisplayed()
        composeRule.onNodeWithTag("session_picker_preset_10").assertIsDisplayed()
        composeRule.onNodeWithTag("session_picker_preset_15").assertIsDisplayed()
    }

    @Test
    fun presetButton_emitsOnConfirmPreset() {
        var captured: Int? = null
        render(onConfirmPreset = { captured = it })
        composeRule.onNodeWithTag("session_picker_preset_5").performClick()
        assertEquals(5, captured)
    }

    @Test
    fun expandArrow_emitsToggleEvent() {
        var fired = false
        render(onToggleExpanded = { fired = true })
        composeRule.onNodeWithTag("session_picker_expand_arrow").performClick()
        assertTrue(fired)
    }

    @Test
    fun expandedPanel_showsCustomFieldAndRadios() {
        render(
            state = SessionDurationPickerUiState(
                packageName = "com.alpha",
                appLabel = "Alpha App",
                expanded = true,
            ),
        )
        composeRule.onNodeWithTag("session_picker_custom_field").assertIsDisplayed()
        composeRule.onNodeWithTag("session_picker_custom_confirm").assertIsDisplayed()
        SessionExpiryMode.entries.forEach { mode ->
            composeRule.onNodeWithTag("session_picker_mode_${mode.name}").assertIsDisplayed()
        }
    }

    @Test
    fun customConfirmButton_disabledWhenInvalid() {
        render(
            state = SessionDurationPickerUiState(
                packageName = "com.alpha",
                appLabel = "Alpha App",
                expanded = true,
                customMinutes = "0",
                customMinutesValid = false,
            ),
        )
        composeRule.onNodeWithTag("session_picker_custom_confirm").assertIsNotEnabled()
    }

    @Test
    fun customConfirmButton_enabledWhenValid() {
        render(
            state = SessionDurationPickerUiState(
                packageName = "com.alpha",
                appLabel = "Alpha App",
                expanded = true,
                customMinutes = "42",
                customMinutesValid = true,
            ),
        )
        composeRule.onNodeWithTag("session_picker_custom_confirm").assertIsEnabled()
    }

    @Test
    fun customConfirmButton_emitsConfirmCustom() {
        var fired = false
        render(
            state = SessionDurationPickerUiState(
                packageName = "com.alpha",
                appLabel = "Alpha App",
                expanded = true,
                customMinutes = "42",
                customMinutesValid = true,
            ),
            onConfirmCustom = { fired = true },
        )
        composeRule.onNodeWithTag("session_picker_custom_confirm").performClick()
        assertTrue(fired)
    }

    @Test
    fun customMinutesField_emitsTypedValue() {
        var captured: String? = null
        render(
            state = SessionDurationPickerUiState(
                packageName = "com.alpha",
                appLabel = "Alpha App",
                expanded = true,
            ),
            onCustomMinutesChange = { captured = it },
        )
        composeRule.onNodeWithTag("session_picker_custom_field").performTextInput("17")
        assertEquals("17", captured)
    }

    @Test
    fun cancelButton_emitsOnDismiss() {
        var fired = false
        render(onDismiss = { fired = true })
        composeRule.onNodeWithTag("session_picker_cancel_button").performClick()
        assertTrue(fired)
    }

    @Test
    fun modeRadio_emitsOnSelectMode() {
        var captured: SessionExpiryMode? = null
        render(
            state = SessionDurationPickerUiState(
                packageName = "com.alpha",
                appLabel = "Alpha App",
                expanded = true,
            ),
            onSelectMode = { captured = it },
        )
        composeRule.onNodeWithTag(
            "session_picker_mode_${SessionExpiryMode.MINDFUL_EXTENSION_DELAY.name}",
        ).performClick()
        assertEquals(SessionExpiryMode.MINDFUL_EXTENSION_DELAY, captured)
    }
}
