/**
 * Compose UI test for MindfulLaunchDelayContent. Drives the stateless
 * variant directly so the test does not need a ViewModel or actual
 * delay coroutine. Verifies the overlay renders the indicator + subtitle
 * and includes the resolved app label.
 *
 * Covers: ULT-79
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
class MindfulLaunchDelayScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun overlay_rendersIndicatorAndSubtitle() {
        composeRule.setContent {
            UltimateFocusTheme {
                MindfulLaunchDelayContent(
                    state = MindfulLaunchDelayUiState(
                        packageName = "com.alpha",
                        appLabel = "Alpha App",
                        started = true,
                    ),
                )
            }
        }
        composeRule.onNodeWithTag("mindful_launch_delay_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("mindful_launch_delay_indicator").assertIsDisplayed()
        composeRule.onNodeWithTag("mindful_launch_delay_subtitle").assertIsDisplayed()
        composeRule.onNodeWithText("Take a breath").assertIsDisplayed()
        composeRule.onNodeWithText("Alpha App will open in a moment.").assertIsDisplayed()
    }

    @Test
    fun overlay_fallsBackToGenericLabelWhenAppLabelIsBlank() {
        composeRule.setContent {
            UltimateFocusTheme {
                MindfulLaunchDelayContent(
                    state = MindfulLaunchDelayUiState(
                        packageName = "com.alpha",
                        appLabel = "",
                        started = true,
                    ),
                )
            }
        }
        composeRule.onNodeWithText("this app will open in a moment.").assertIsDisplayed()
    }
}
