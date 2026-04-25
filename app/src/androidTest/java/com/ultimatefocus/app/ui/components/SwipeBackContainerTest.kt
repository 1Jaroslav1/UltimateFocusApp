/**
 * Compose UI tests for SwipeBackContainer. A drag from inside the left-edge
 * band that exceeds the threshold triggers onBack; drags that start in the
 * middle of the screen do not (so horizontally-scrollable children remain
 * usable). Vertical drags never fire onBack.
 * Covers: ULT-23 (iteration)
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SwipeBackContainerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun edgeSwipeRight_pastThreshold_invokesOnBack() {
        var backInvoked = false
        composeRule.setContent {
            SwipeBackContainer(onBack = { backInvoked = true }) {
                Box(modifier = Modifier.fillMaxSize().testTag("content"))
            }
        }
        composeRule.onNodeWithTag("content").performTouchInput {
            // Start near the left edge and swipe most of the way across.
            swipeRight(startX = 0f, endX = right * 0.8f)
        }
        composeRule.runOnIdle { assertTrue(backInvoked) }
    }

    @Test
    fun midScreenSwipeRight_doesNotInvokeOnBack() {
        var backInvoked = false
        composeRule.setContent {
            SwipeBackContainer(onBack = { backInvoked = true }) {
                Box(modifier = Modifier.fillMaxSize().testTag("content"))
            }
        }
        composeRule.onNodeWithTag("content").performTouchInput {
            // Start in the middle of the screen — outside the edge band.
            swipeRight(startX = centerX, endX = right * 0.95f)
        }
        composeRule.runOnIdle { assertFalse(backInvoked) }
    }

    @Test
    fun verticalSwipe_doesNotInvokeOnBack() {
        var backInvoked = false
        composeRule.setContent {
            SwipeBackContainer(onBack = { backInvoked = true }) {
                Box(modifier = Modifier.fillMaxSize().testTag("content"))
            }
        }
        composeRule.onNodeWithTag("content").performTouchInput { swipeDown() }
        composeRule.onNodeWithTag("content").performTouchInput { swipeUp() }
        composeRule.runOnIdle { assertFalse(backInvoked) }
    }

    @Test
    fun disabled_swipeIsIgnored() {
        var backInvoked = false
        composeRule.setContent {
            SwipeBackContainer(onBack = { backInvoked = true }, enabled = false) {
                Box(modifier = Modifier.fillMaxSize().testTag("content"))
            }
        }
        composeRule.onNodeWithTag("content").performTouchInput {
            swipeRight(startX = 0f, endX = right * 0.8f)
        }
        composeRule.runOnIdle { assertFalse(backInvoked) }
    }
}
