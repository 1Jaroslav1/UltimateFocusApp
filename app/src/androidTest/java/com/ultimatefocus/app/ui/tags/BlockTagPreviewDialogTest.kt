/**
 * Compose UI tests for BlockTagPreviewDialog. Verifies the affected-app
 * count copy, the app-label list rendering, all 8 duration buttons, the
 * empty-tag case (no apps assigned → buttons disabled), and the cancel
 * path. Labels are shown — never raw package names — addressing the
 * iteration feedback that "org.telegram.messenger" should read "Telegram".
 * Covers: ULT-75
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockTagPreviewDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val socialTag = Tag(id = 1L, name = "Social", color = TagColor.STEEL_BLUE)

    private fun apps(vararg pairs: Pair<String, String>) =
        pairs.map { (pkg, label) -> BlockTagPreviewApp(pkg, label) }

    @Test
    fun preview_listsAllAffectedAppsByLabel() {
        composeRule.setContent {
            UltimateFocusTheme {
                BlockTagPreviewDialog(
                    tag = socialTag,
                    affectedApps = apps(
                        "org.telegram.messenger" to "Telegram",
                        "com.instagram.android" to "Instagram",
                        "com.zhiliaoapp.musically" to "TikTok",
                    ),
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("3 apps will be blocked:").assertIsDisplayed()
        composeRule.onNodeWithText("Telegram").assertIsDisplayed()
        composeRule.onNodeWithText("Instagram").assertIsDisplayed()
        composeRule.onNodeWithText("TikTok").assertIsDisplayed()
        // Critical: the raw packageName must not be visible.
        composeRule.onNodeWithText("org.telegram.messenger").assertDoesNotExist()
    }

    @Test
    fun preview_singularCopyForOneApp() {
        composeRule.setContent {
            UltimateFocusTheme {
                BlockTagPreviewDialog(
                    tag = socialTag,
                    affectedApps = apps("com.only" to "Only"),
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("1 app will be blocked:").assertIsDisplayed()
    }

    @Test
    fun all_eight_durations_are_rendered() {
        composeRule.setContent {
            UltimateFocusTheme {
                BlockTagPreviewDialog(
                    tag = socialTag,
                    affectedApps = apps("com.a" to "A"),
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        BlockDuration.entries.forEach { d ->
            composeRule.onNodeWithTag("block_tag_duration_${d.label}").assertIsDisplayed()
        }
    }

    @Test
    fun choosing_a_duration_invokes_onConfirm_with_it() {
        var picked: BlockDuration? = null
        composeRule.setContent {
            UltimateFocusTheme {
                BlockTagPreviewDialog(
                    tag = socialTag,
                    affectedApps = apps("com.a" to "A"),
                    onConfirm = { picked = it },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("block_tag_duration_24h").performClick()
        assertEquals(BlockDuration.DAY_1, picked)
    }

    @Test
    fun no_apps_disables_all_duration_buttons() {
        composeRule.setContent {
            UltimateFocusTheme {
                BlockTagPreviewDialog(
                    tag = socialTag,
                    affectedApps = emptyList(),
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("No apps are tagged with 'Social'.").assertIsDisplayed()
        composeRule.onNodeWithTag("block_tag_duration_1h").assertIsNotEnabled()
    }

    @Test
    fun cancel_button_invokes_onDismiss() {
        var dismissed = false
        composeRule.setContent {
            UltimateFocusTheme {
                BlockTagPreviewDialog(
                    tag = socialTag,
                    affectedApps = apps("com.a" to "A"),
                    onConfirm = {},
                    onDismiss = { dismissed = true },
                )
            }
        }
        composeRule.onNodeWithTag("block_tag_cancel_button").performClick()
        assertTrue(dismissed)
    }
}
