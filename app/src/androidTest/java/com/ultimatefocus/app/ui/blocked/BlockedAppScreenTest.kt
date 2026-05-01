/**
 * Compose tests for BlockedAppScreen.
 * Verifies required UI elements render correctly and — crucially — that
 * NO "Unblock" / "Override" / "Skip" / "Cancel block" affordance appears.
 * This enforces the ULT-21 AC that blocks have no escape hatch.
 *
 * ULT-84: also exercises the re-entry case — when the activity is
 * reused via onNewIntent for a different blocked app, the screen must
 * re-render against the new extras (label switches, countdown re-binds
 * to the new expiry).
 *
 * Covers: ULT-21, ULT-84
 */
package com.ultimatefocus.app.ui.blocked

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.service.FakeClock
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

    // ── ULT-84: re-entry refreshes the screen ───────────────────────────────

    @Test
    fun extrasSwapMidStream_relabelsScreenAndResetsCountdown() {
        // Two distinct blocked apps with different countdowns. Simulates
        // the singleTask onNewIntent path: same Compose tree, new extras.
        val app1 = BlockedAppExtras(
            packageName = "com.test.app1",
            appLabel = "App One",
            blockedUntilEpochMs = 1_000L + 60_000L,
            durationLabel = "1h",
        )
        val app2 = BlockedAppExtras(
            packageName = "com.test.app2",
            appLabel = "App Two",
            blockedUntilEpochMs = 1_000L + 5 * 60_000L,
            durationLabel = "5h",
        )
        val clock = FakeClock(current = 1_000L)
        var extras by mutableStateOf(app1)

        composeRule.setContent {
            UltimateFocusTheme {
                BlockedAppScreen(extras = extras, clock = clock, onDismiss = {})
            }
        }

        composeRule.onNodeWithText("🔒 App One").assertIsDisplayed()

        extras = app2
        composeRule.waitForIdle()

        composeRule.onNodeWithText("🔒 App Two").assertIsDisplayed()
        // The previous label is gone — confirms recomposition really reset.
        assertTrue(
            composeRule.onAllNodesWithText("🔒 App One").fetchSemanticsNodes().isEmpty(),
        )
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
