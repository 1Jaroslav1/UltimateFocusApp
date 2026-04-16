/**
 * Compose UI integration tests for the home-screen gesture modifiers.
 * Verifies swipe-up, swipe-down, double-tap dispatch and that below-threshold
 * or disabled-feature interactions are ignored.
 *
 * Covers: ULT-16, ULT-17, ULT-18
 */
package com.ultimatefocus.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.ui.home.components.homeDoubleTap
import com.ultimatefocus.app.ui.home.components.homeSwipeGestures
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenGestureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class Counts(var up: Int = 0, var down: Int = 0, var doubleTap: Int = 0)

    private fun setContentWithGestures(counts: Counts, doubleTapEnabled: Boolean = false) {
        composeRule.setContent {
            UltimateFocusTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .homeSwipeGestures(
                            onSwipeUp = { counts.up++ },
                            onSwipeDown = { counts.down++ },
                        )
                        .homeDoubleTap(
                            enabled = doubleTapEnabled,
                            onDoubleTap = { counts.doubleTap++ },
                        )
                        .testTag("gesture_box"),
                )
            }
        }
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun swipeUp_dispatchesOnSwipeUpExactlyOnce() {
        val counts = Counts()
        setContentWithGestures(counts)

        composeRule.onNodeWithTag("gesture_box").performTouchInput { swipeUp() }

        composeRule.runOnIdle {
            assertEquals(1, counts.up)
            assertEquals(0, counts.down)
        }
    }

    @Test
    fun swipeDown_dispatchesOnSwipeDownExactlyOnce() {
        val counts = Counts()
        setContentWithGestures(counts)

        composeRule.onNodeWithTag("gesture_box").performTouchInput { swipeDown() }

        composeRule.runOnIdle {
            assertEquals(0, counts.up)
            assertEquals(1, counts.down)
        }
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun smallVerticalDrag_belowThreshold_isIgnored() {
        val counts = Counts()
        setContentWithGestures(counts)

        composeRule.onNodeWithTag("gesture_box").performTouchInput {
            // 5-pixel drag — well under the 60 dp threshold
            down(center)
            moveBy(Offset(0f, -5f))
            up()
        }

        composeRule.runOnIdle {
            assertEquals(0, counts.up)
            assertEquals(0, counts.down)
        }
    }

    @Test
    fun horizontalSwipe_doesNotTriggerVerticalCallbacks() {
        val counts = Counts()
        setContentWithGestures(counts)

        composeRule.onNodeWithTag("gesture_box").performTouchInput { swipeLeft() }

        composeRule.runOnIdle {
            assertEquals(0, counts.up)
            assertEquals(0, counts.down)
        }
    }

    // ── ULT-18: double-tap ───────────────────────────────────────────────────

    @Test
    fun doubleTap_whenEnabled_dispatchesOnce() {
        val counts = Counts()
        setContentWithGestures(counts, doubleTapEnabled = true)

        composeRule.onNodeWithTag("gesture_box").performTouchInput { doubleClick() }

        composeRule.runOnIdle {
            assertEquals(1, counts.doubleTap)
        }
    }

    @Test
    fun doubleTap_whenDisabled_isIgnored() {
        val counts = Counts()
        setContentWithGestures(counts, doubleTapEnabled = false)

        composeRule.onNodeWithTag("gesture_box").performTouchInput { doubleClick() }

        composeRule.runOnIdle {
            assertEquals(0, counts.doubleTap)
        }
    }
}
