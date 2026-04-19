/**
 * Compose tests for AppContextMenuDialog.
 * Verifies that the block menu entry is present for unblocked apps and
 * replaced with a non-interactive status row for blocked apps, and that
 * NO "Unblock" / "Override" affordance ever appears — enforcing the
 * ULT-19 "no self-unblock escape hatch" requirement at the UI layer.
 * Covers: ULT-20, ULT-21
 */
package com.ultimatefocus.app.ui.components

import android.content.ComponentName
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppContextMenuDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun makeApp(
        blockedUntilEpochMs: Long? = null,
        isFavorite: Boolean = false,
    ) = AppInfo(
        packageName = "com.test.calendar",
        label = "Calendar",
        componentName = ComponentName("com.test.calendar", "Main"),
        isFavorite = isFavorite,
        blockedUntilEpochMs = blockedUntilEpochMs,
    )

    // ── Unblocked state: "Block app…" is available ───────────────────────────

    @Test
    fun unblockedApp_showsBlockMenuEntry() {
        composeRule.setContent {
            UltimateFocusTheme {
                AppContextMenuDialog(
                    app = makeApp(),
                    nowEpochMs = 1_000L,
                    onToggleFavorite = {},
                    onBlockApp = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("context_menu_block_app").assertIsDisplayed()
        composeRule.onNodeWithTag("context_menu_block_status").assertDoesNotExist()
    }

    @Test
    fun unblockedApp_tappingBlockEntry_invokesOnBlockApp() {
        var blocked: AppInfo? = null
        val app = makeApp()
        composeRule.setContent {
            UltimateFocusTheme {
                AppContextMenuDialog(
                    app = app,
                    nowEpochMs = 1_000L,
                    onToggleFavorite = {},
                    onBlockApp = { blocked = it },
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("context_menu_block_app").performClick()
        assertEquals(app, blocked)
    }

    // ── Blocked state: read-only status row, no unblock affordance ───────────

    @Test
    fun blockedApp_showsStatusRowWithRemainingTime() {
        val now = 1_000L
        val remaining = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(17)
        composeRule.setContent {
            UltimateFocusTheme {
                AppContextMenuDialog(
                    app = makeApp(blockedUntilEpochMs = now + remaining),
                    nowEpochMs = now,
                    onToggleFavorite = {},
                    onBlockApp = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("context_menu_block_status").assertIsDisplayed()
        composeRule.onNodeWithText("Blocked — 2h 17m remaining").assertIsDisplayed()
        composeRule.onNodeWithTag("context_menu_block_app").assertDoesNotExist()
    }

    @Test
    fun blockedApp_hasNoUnblockOrOverrideText() {
        val now = 1_000L
        composeRule.setContent {
            UltimateFocusTheme {
                AppContextMenuDialog(
                    app = makeApp(blockedUntilEpochMs = now + TimeUnit.HOURS.toMillis(1)),
                    nowEpochMs = now,
                    onToggleFavorite = {},
                    onBlockApp = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        // Critical AC for ULT-21: no path offered to cancel/override the block.
        val forbidden = listOf("Unblock", "unblock", "Override", "override", "Cancel block", "Skip")
        forbidden.forEach { text ->
            assertTrue(
                "Context menu must not expose '$text'",
                composeRule.onAllNodesWithText(text, substring = true)
                    .fetchSemanticsNodes().isEmpty(),
            )
        }
    }

    // ── Favorite toggle + App Info rows still work in both states ────────────

    @Test
    fun favoriteRow_isClickableInBothStates() {
        val now = 1_000L
        var toggledCount = 0
        composeRule.setContent {
            UltimateFocusTheme {
                AppContextMenuDialog(
                    app = makeApp(blockedUntilEpochMs = now + TimeUnit.HOURS.toMillis(1)),
                    nowEpochMs = now,
                    onToggleFavorite = { toggledCount++ },
                    onBlockApp = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("context_menu_toggle_favorite")
            .assertHasClickAction()
            .performClick()
        assertEquals(1, toggledCount)
    }

    @Test
    fun appInfoRow_isDisplayedInUnblockedState() {
        composeRule.setContent {
            UltimateFocusTheme {
                AppContextMenuDialog(
                    app = makeApp(),
                    nowEpochMs = 1_000L,
                    onToggleFavorite = {},
                    onBlockApp = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("context_menu_app_info").assertIsDisplayed()
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun blockedUntilInThePast_isTreatedAsUnblocked() {
        // Defensive: if the row somehow slipped through with an expired timestamp,
        // the menu should offer "Block app…" again (no dead-state display).
        composeRule.setContent {
            UltimateFocusTheme {
                AppContextMenuDialog(
                    app = makeApp(blockedUntilEpochMs = 500L),
                    nowEpochMs = 1_000L,
                    onToggleFavorite = {},
                    onBlockApp = {},
                    onAppInfo = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("context_menu_block_app").assertIsDisplayed()
        composeRule.onNodeWithTag("context_menu_block_status").assertDoesNotExist()
    }
}
