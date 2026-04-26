/**
 * Compose UI tests for SessionExpiredOverlayContent (ULT-81 / Mode 1).
 * Drives the stateless variant directly so the test does not need a
 * ViewModel or actual countdown coroutine. Verifies:
 *  - the headline + countdown render;
 *  - the Extend button is disabled while the countdown is active;
 *  - the Extend button enables after canExtend = true and emits onExtend;
 *  - cancel button emits onCancel.
 *
 * Covers: ULT-81
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionExpiredOverlayScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        state: SessionExpiredOverlayUiState =
            SessionExpiredOverlayUiState(
                packageName = "com.alpha",
                appLabel = "Alpha App",
                secondsRemaining = 20,
                canExtend = false,
            ),
        onExtend: () -> Unit = {},
        onCancel: () -> Unit = {},
    ) {
        composeRule.setContent {
            UltimateFocusTheme {
                SessionExpiredOverlayContent(
                    state = state,
                    onExtend = onExtend,
                    onCancel = onCancel,
                )
            }
        }
    }

    @Test
    fun overlay_rendersHeadlineAndCountdown() {
        render()
        composeRule.onNodeWithTag("session_expired_overlay").assertIsDisplayed()
        composeRule.onNodeWithText("Time's up").assertIsDisplayed()
        composeRule.onNodeWithTag("session_expired_countdown_indicator").assertIsDisplayed()
        composeRule.onNodeWithTag("session_expired_countdown_label").assertIsDisplayed()
    }

    @Test
    fun extendButton_isDisabledWhenCannotExtend() {
        render()
        composeRule.onNodeWithTag("session_expired_extend_button").assertIsNotEnabled()
    }

    @Test
    fun extendButton_isEnabledWhenCanExtend() {
        render(
            state = SessionExpiredOverlayUiState(
                packageName = "com.alpha",
                appLabel = "Alpha App",
                secondsRemaining = 0,
                canExtend = true,
            ),
        )
        composeRule.onNodeWithTag("session_expired_extend_button").assertIsEnabled()
    }

    @Test
    fun extendButton_emitsOnExtend() {
        var fired = false
        render(
            state = SessionExpiredOverlayUiState(
                packageName = "com.alpha",
                appLabel = "Alpha App",
                secondsRemaining = 0,
                canExtend = true,
            ),
            onExtend = { fired = true },
        )
        composeRule.onNodeWithTag("session_expired_extend_button").performClick()
        assertTrue(fired)
    }

    @Test
    fun cancelButton_emitsOnCancel() {
        var fired = false
        render(onCancel = { fired = true })
        composeRule.onNodeWithTag("session_expired_cancel_button").performClick()
        assertTrue(fired)
    }
}
