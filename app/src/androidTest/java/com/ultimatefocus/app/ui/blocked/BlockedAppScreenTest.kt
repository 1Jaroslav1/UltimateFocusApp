/**
 * Compose tests for BlockedAppScreen.
 * Verifies required UI elements render correctly and — crucially — that
 * NO "Unblock" / "Override" / "Skip" / "Cancel block" affordance appears.
 * This enforces the ULT-21 AC that blocks have no escape hatch.
 * Covers: ULT-21
 */
package com.ultimatefocus.app.ui.blocked

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockedAppScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screen_rendersLabelMotivationAndRemaining() {
        composeRule.setContent {
            UltimateFocusTheme {
                BlockedAppContent(
                    appLabel = "Instagram",
                    remainingLabel = "2h 17m",
                    unblockDateTimeLabel = "Apr 19, 20:45",
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("blocked_app_label").assertIsDisplayed()
        composeRule.onNodeWithText("\uD83D\uDD12 Instagram").assertIsDisplayed()
        composeRule.onNodeWithText("This app is blocked.").assertIsDisplayed()
        composeRule.onNodeWithText("Time remaining: 2h 17m").assertIsDisplayed()
        composeRule.onNodeWithText(
            "You committed to staying focused. Return to what matters.",
        ).assertIsDisplayed()
    }

    @Test
    fun returnHomeButton_invokesOnDismiss() {
        var dismissed = 0
        composeRule.setContent {
            UltimateFocusTheme {
                BlockedAppContent(
                    appLabel = "Instagram",
                    remainingLabel = "1h 0m",
                    unblockDateTimeLabel = "Apr 19, 20:45",
                    onDismiss = { dismissed++ },
                )
            }
        }
        composeRule.onNodeWithTag("blocked_return_home").performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun screen_hasNoOverrideOrUnblockAffordance() {
        composeRule.setContent {
            UltimateFocusTheme {
                BlockedAppContent(
                    appLabel = "Instagram",
                    remainingLabel = "1h 0m",
                    unblockDateTimeLabel = "Apr 19, 20:45",
                    onDismiss = {},
                )
            }
        }
        val forbidden = listOf(
            "Unblock",
            "unblock",
            "Override",
            "override",
            "Cancel block",
            "Skip",
        )
        forbidden.forEach { text ->
            assertTrue(
                "BlockedAppScreen must not expose '$text'",
                composeRule.onAllNodesWithText(text, substring = true)
                    .fetchSemanticsNodes().isEmpty(),
            )
        }
    }

    @Test
    fun unblockDateTime_isDisplayedWhenProvided() {
        composeRule.setContent {
            UltimateFocusTheme {
                BlockedAppContent(
                    appLabel = "Instagram",
                    remainingLabel = "1h 0m",
                    unblockDateTimeLabel = "Apr 19, 20:45",
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("blocked_app_unblock_at").assertIsDisplayed()
        composeRule.onNodeWithText("Blocked until Apr 19, 20:45").assertIsDisplayed()
    }

    @Test
    fun unblockDateTime_isOmittedWhenEmpty() {
        composeRule.setContent {
            UltimateFocusTheme {
                BlockedAppContent(
                    appLabel = "Instagram",
                    remainingLabel = "1h 0m",
                    unblockDateTimeLabel = "",
                    onDismiss = {},
                )
            }
        }
        // No crash and the node is not rendered.
        composeRule.onNodeWithTag("blocked_return_home").assertIsDisplayed()
    }

    @Test
    fun remainingZero_rendersLessThan1m() {
        composeRule.setContent {
            UltimateFocusTheme {
                BlockedAppContent(
                    appLabel = "Calendar",
                    remainingLabel = "< 1m",
                    unblockDateTimeLabel = "Apr 19, 20:45",
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("Time remaining: < 1m").assertIsDisplayed()
        composeRule.onNodeWithTag("blocked_return_home").assertIsDisplayed()
    }
}
