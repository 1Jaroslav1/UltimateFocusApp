/**
 * Gesture-detection Modifier used by the home screen. Accumulates vertical
 * drag deltas and dispatches a single onSwipeUp / onSwipeDown callback on
 * gesture release, once the accumulated movement crosses SWIPE_THRESHOLD_DP.
 *
 * Extracted so that both the production HomeScreen and instrumented tests
 * apply the exact same gesture logic.
 *
 * Covers: ULT-16, ULT-17
 */
package com.ultimatefocus.app.ui.home.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Minimum accumulated vertical drag (in dp) that counts as a swipe. */
const val SWIPE_THRESHOLD_DP: Float = 60f

/**
 * Adds vertical-swipe detection to this node. The callbacks fire at most once
 * per gesture, on release, only if the accumulated drag crosses the threshold.
 */
fun Modifier.homeSwipeGestures(
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
): Modifier = this.pointerInput(Unit) {
    val thresholdPx = SWIPE_THRESHOLD_DP * density
    var totalDy = 0f
    detectVerticalDragGestures(
        onDragStart = { totalDy = 0f },
        onDragEnd = {
            when {
                totalDy <= -thresholdPx -> onSwipeUp()
                totalDy >= thresholdPx -> onSwipeDown()
            }
        },
    ) { _, dy -> totalDy += dy }
}
