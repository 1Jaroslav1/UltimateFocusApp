/**
 * Compose UI tests for AppListItem's tag-related rendering (ULT-74).
 * Verifies: dots appear when the app has tags; no dots when there are
 * none (and no placeholder gap); the row's combined click action still
 * works on the labeled text (semantics merging preserved).
 * Covers: ULT-74
 */
package com.ultimatefocus.app.ui.drawer

import android.content.ComponentName
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.Tag
import com.ultimatefocus.app.domain.model.TagColor
import com.ultimatefocus.app.ui.drawer.components.AppListItem
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppListItemTagsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun makeApp(
        label: String = "Calendar",
        tags: List<Tag> = emptyList(),
    ) = AppInfo(
        packageName = "com.test.${label.lowercase().replace(" ", "")}",
        label = label,
        componentName = ComponentName("com.test", "Main"),
        tags = tags,
    )

    @Test
    fun appWithTags_rendersDotsRow() {
        val app = makeApp(
            tags = listOf(
                Tag(1L, "Social", TagColor.STEEL_BLUE),
                Tag(2L, "Games", TagColor.AMBER_GOLD),
            ),
        )
        composeRule.setContent {
            UltimateFocusTheme {
                AppListItem(appInfo = app, onClick = {}, onLongClick = {})
            }
        }
        composeRule.onNodeWithTag("tag_dots_row").assertIsDisplayed()
        composeRule.onNodeWithTag("tag_dot_1").assertIsDisplayed()
        composeRule.onNodeWithTag("tag_dot_2").assertIsDisplayed()
    }

    @Test
    fun appWithoutTags_doesNotRenderDotsRow() {
        composeRule.setContent {
            UltimateFocusTheme {
                AppListItem(appInfo = makeApp(), onClick = {}, onLongClick = {})
            }
        }
        composeRule.onNodeWithTag("tag_dots_row").assertDoesNotExist()
    }

    @Test
    fun rowClickStillWorksWhenTagsPresent() {
        val app = makeApp(
            label = "Maps",
            tags = listOf(Tag(99L, "Travel", TagColor.TEAL)),
        )
        var clicked = false
        composeRule.setContent {
            UltimateFocusTheme {
                AppListItem(
                    appInfo = app,
                    onClick = { clicked = true },
                    onLongClick = {},
                )
            }
        }
        composeRule.onNodeWithText("Maps").performClick()
        assertEquals(true, clicked)
    }
}
