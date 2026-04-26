/**
 * Compose UI tests for SessionExpiredOverlayContent (ULT-81 / Mode 1).
 * The overlay is loader-only — no buttons. Verifies the headline +
 * countdown indicator + subtitle render with the supplied state.
 *
 * Covers: ULT-81
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
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
                progress = 0f,
            ),
    ) {
        composeRule.setContent {
            UltimateFocusTheme {
                SessionExpiredOverlayContent(state = state)
            }
        }
    }

    @Test
    fun overlay_rendersHeadlineAndCountdown() {
        render()
        composeRule.onNodeWithTag("session_expired_overlay").assertIsDisplayed()
        composeRule.onNodeWithText("Take a moment").assertIsDisplayed()
        composeRule.onNodeWithTag("session_expired_countdown_indicator").assertIsDisplayed()
        composeRule.onNodeWithTag("session_expired_subtitle").assertIsDisplayed()
    }

    @Test
    fun overlay_rendersWithFullProgress() {
        render(
            state = SessionExpiredOverlayUiState(
                packageName = "com.alpha",
                appLabel = "Alpha App",
                progress = 1f,
            ),
        )
        composeRule.onNodeWithTag("session_expired_countdown_indicator").assertIsDisplayed()
    }
}
