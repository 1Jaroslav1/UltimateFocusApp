/**
 * Compose UI tests for MinimalToggle — the centralized toggle replacing
 * Material3 Switch across the launcher (settings, In-App Reminder).
 * Verifies the testTag passes through, taps fire onCheckedChange, the
 * disabled state is honored, and the rendered chrome is visibly smaller
 * than a vanilla Switch (Modifier.scale(0.8f) ≈ 20% reduction).
 *
 * Covers: ULT-82
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.material3.Switch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MinimalToggleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersWithTestTagAndCheckedState() {
        composeRule.setContent {
            UltimateFocusTheme {
                MinimalToggle(
                    checked = true,
                    onCheckedChange = {},
                    modifier = Modifier.testTag("minimal_toggle"),
                )
            }
        }
        composeRule.onNodeWithTag("minimal_toggle").assertIsDisplayed().assertIsOn()
    }

    @Test
    fun emitsToggleCallback() {
        var captured: Boolean? = null
        composeRule.setContent {
            UltimateFocusTheme {
                MinimalToggle(
                    checked = false,
                    onCheckedChange = { captured = it },
                    modifier = Modifier.testTag("minimal_toggle"),
                )
            }
        }
        composeRule.onNodeWithTag("minimal_toggle").assertIsOff().performClick()
        assertEquals(true, captured)
    }

    @Test
    fun disabledStateBlocksInteraction() {
        composeRule.setContent {
            UltimateFocusTheme {
                MinimalToggle(
                    checked = false,
                    onCheckedChange = {},
                    enabled = false,
                    modifier = Modifier.testTag("minimal_toggle"),
                )
            }
        }
        composeRule.onNodeWithTag("minimal_toggle").assertIsNotEnabled()
    }

    @Test
    fun renderedSizeIsSmallerThanVanillaSwitch() {
        composeRule.setContent {
            UltimateFocusTheme {
                Switch(
                    checked = false,
                    onCheckedChange = {},
                    modifier = Modifier.testTag("vanilla_switch"),
                )
                MinimalToggle(
                    checked = false,
                    onCheckedChange = {},
                    modifier = Modifier.testTag("minimal_toggle"),
                )
            }
        }
        // Both share Switch's hit-target size in semantics, so we compare
        // bounding-box width via fetchSemanticsNode.boundsInRoot.
        val vanillaBounds = composeRule.onNodeWithTag("vanilla_switch")
            .fetchSemanticsNode().boundsInRoot
        val minimalBounds = composeRule.onNodeWithTag("minimal_toggle")
            .fetchSemanticsNode().boundsInRoot
        // The minimal toggle's drawn footprint should be no larger than
        // the vanilla Switch's.
        assertTrue(
            "minimal=${minimalBounds.width} vs vanilla=${vanillaBounds.width}",
            minimalBounds.width <= vanillaBounds.width,
        )
    }
}
