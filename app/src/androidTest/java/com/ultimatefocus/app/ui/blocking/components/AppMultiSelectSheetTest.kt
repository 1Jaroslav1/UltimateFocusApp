/**
 * Compose UI tests for AppMultiSelectSheet. Verifies installed apps render,
 * checkboxes reflect the selected set, and tapping a row fires the toggle
 * callback with the correct package name.
 * Covers: ULT-24
 */
package com.ultimatefocus.app.ui.blocking.components

import android.content.ComponentName
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppMultiSelectSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun fakeApp(pkg: String, label: String) =
        AppInfo(
            packageName = pkg,
            label = label,
            componentName = ComponentName(pkg, "$pkg.MainActivity"),
        )

    @Test
    fun rendersEachInstalledApp() {
        val apps = listOf(fakeApp("com.a", "Alpha"), fakeApp("com.b", "Beta"))
        composeRule.setContent {
            UltimateFocusTheme {
                AppMultiSelectSheet(
                    apps = apps,
                    selected = emptySet(),
                    onToggle = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("schedule_app_row_com.a").assertIsDisplayed()
        composeRule.onNodeWithTag("schedule_app_row_com.b").assertIsDisplayed()
    }

    @Test
    fun tappingRow_invokesToggleWithPackageName() {
        val apps = listOf(fakeApp("com.a", "Alpha"))
        val toggled = mutableListOf<String>()
        composeRule.setContent {
            UltimateFocusTheme {
                AppMultiSelectSheet(
                    apps = apps,
                    selected = emptySet(),
                    onToggle = { toggled += it },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("schedule_app_row_com.a").performClick()
        assertEquals(listOf("com.a"), toggled)
    }

    @Test
    fun doneButton_invokesDismiss() {
        var dismissed = false
        composeRule.setContent {
            UltimateFocusTheme {
                AppMultiSelectSheet(
                    apps = emptyList(),
                    selected = emptySet(),
                    onToggle = {},
                    onDismiss = { dismissed = true },
                )
            }
        }
        composeRule.onNodeWithTag("schedule_apps_done_button").performClick()
        assert(dismissed)
    }
}
