/**
 * Compose UI tests for ClockWidget — covers the ULT-82 #2 size reduction.
 * Asserts the rendered Canvas (the battery arc) measures 160 dp (down
 * from the previous 200 dp) and that the time/date/battery text nodes
 * are still displayed at the new sizes.
 *
 * Covers: ULT-82
 */
package com.ultimatefocus.app.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.home.components.ClockWidget
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ClockWidgetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sample: LocalDateTime = LocalDateTime.of(2026, 5, 1, 9, 41)

    @Test
    fun rendersTimeDateBattery() {
        composeRule.setContent {
            UltimateFocusTheme {
                ClockWidget(currentTime = sample, batteryLevel = 75, isCharging = false)
            }
        }
        composeRule.onNodeWithTag("clock_time").assertIsDisplayed()
        composeRule.onNodeWithTag("clock_date").assertIsDisplayed()
        composeRule.onNodeWithTag("clock_battery").assertIsDisplayed()
    }

    @Test
    fun batteryArcCanvasIsSixtyDpSmaller() {
        composeRule.setContent {
            UltimateFocusTheme {
                ClockWidget(currentTime = sample, batteryLevel = 50, isCharging = true)
            }
        }
        val arc = composeRule.onNodeWithTag("clock_battery_arc").fetchSemanticsNode().size
        val widthDp = with(composeRule.density) { arc.width.toDp() }
        val heightDp = with(composeRule.density) { arc.height.toDp() }
        // Allow ±1 dp of measurement noise.
        assertTrue(
            "expected width≈160.dp, was $widthDp",
            abs((widthDp - 160.dp).value) <= 1f,
        )
        assertTrue(
            "expected height≈160.dp, was $heightDp",
            abs((heightDp - 160.dp).value) <= 1f,
        )
    }
}
