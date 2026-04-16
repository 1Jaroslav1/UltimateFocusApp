/**
 * Compose UI integration tests for the home-screen swipe-gesture modifier.
 * Verifies that:
 * - Swipe-up triggers onSwipeUp exactly once per gesture.
 * - Swipe-down triggers onSwipeDown exactly once per gesture.
 * - A drag smaller than the threshold is ignored (border case).
 * - A horizontal drag does not trigger either vertical callback (border case).
 *
 * Covers: ULT-16, ULT-17
 */
package com.ultimatefocus.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    private class Counts(var up: Int = 0, var down: Int = 0)

    private fun setContentWithGestures(counts: Counts) {
        composeRule.setContent {
            UltimateFocusTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .homeSwipeGestures(
                            onSwipeUp = { counts.up++ },
                            onSwipeDown = { counts.down++ },
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
}
