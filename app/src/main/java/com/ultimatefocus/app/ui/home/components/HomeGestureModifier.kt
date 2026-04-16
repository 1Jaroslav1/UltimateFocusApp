/**
 * Gesture-detection Modifiers used by the home screen:
 * - homeSwipeGestures accumulates vertical drag and dispatches onSwipeUp /
 *   onSwipeDown once per gesture on release, when the accumulated movement
 *   crosses SWIPE_THRESHOLD_DP.
 * - homeDoubleTap fires the provided callback only when the double-tap
 *   feature is enabled, relying on Compose's built-in double-tap window for
 *   accidental-tap mitigation.
 *
 * Extracted so production HomeScreen and instrumented tests apply identical
 * gesture logic.
 *
 * Covers: ULT-16, ULT-17, ULT-18
 */
package com.ultimatefocus.app.ui.home.components

import androidx.compose.foundation.gestures.detectTapGestures
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

/**
 * Adds double-tap detection to this node that only fires when [enabled] is
 * true. The gesture detector is keyed on [enabled] so a preference change
 * toggles the listener on/off without restarting the composition.
 * Single taps are ignored.
 */
fun Modifier.homeDoubleTap(
    enabled: Boolean,
    onDoubleTap: () -> Unit,
): Modifier = this.pointerInput(enabled) {
    if (!enabled) return@pointerInput
    detectTapGestures(onDoubleTap = { onDoubleTap() })
}
