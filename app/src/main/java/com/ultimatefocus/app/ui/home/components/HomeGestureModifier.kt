/**
 * Gesture-detection Modifiers used by the home screen:
 * - homeSwipeGestures accumulates vertical drag and dispatches onSwipeUp /
 *   onSwipeDown once per gesture on release, when the accumulated movement
 *   crosses SWIPE_THRESHOLD_DP.
 * - homeTwoFingerTap fires onTwoFingerTap when the user presses two fingers
 *   simultaneously (within TWO_FINGER_WINDOW_MS) and releases both without
 *   significant drag or exceeding the long-press timeout. Replaces an earlier
 *   single-finger double-tap handler that did not fire reliably in the
 *   launcher context (ULT-18).
 *
 * Extracted so production HomeScreen and instrumented tests apply identical
 * gesture logic.
 *
 * Covers: ULT-16, ULT-17, ULT-18
 */
package com.ultimatefocus.app.ui.home.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput

/** Minimum accumulated vertical drag (in dp) that counts as a swipe. */
const val SWIPE_THRESHOLD_DP: Float = 60f

/** Max delay between the first and second finger arriving to be treated as simultaneous. */
const val TWO_FINGER_WINDOW_MS: Long = 200L

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
 * Adds two-finger-tap detection to this node that only fires when [enabled]
 * is true. The gesture detector is keyed on [enabled] so a preference change
 * toggles the listener on/off without restarting the composition.
 *
 * Recognition rules (all must hold for the callback to fire):
 * - Two distinct fingers are pressed within TWO_FINGER_WINDOW_MS of each other.
 * - Neither finger moves more than `viewConfiguration.touchSlop` from its
 *   down position (i.e. the user tapped, did not swipe).
 * - Both fingers lift within `viewConfiguration.longPressTimeoutMillis` of the
 *   first down (i.e. the user tapped, did not long-press).
 */
fun Modifier.homeTwoFingerTap(
    enabled: Boolean,
    onTwoFingerTap: () -> Unit,
): Modifier = this.pointerInput(enabled) {
    if (!enabled) return@pointerInput
    val slop = viewConfiguration.touchSlop
    val slopSquared = slop * slop
    val tapTimeoutMs = viewConfiguration.longPressTimeoutMillis

    awaitEachGesture {
        // Phase 1 — first finger down.
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        val startTime = firstDown.uptimeMillis
        val starts: MutableMap<PointerId, Offset> = mutableMapOf(firstDown.id to firstDown.position)

        // Phase 2 — wait for a second finger within the window.
        var secondFingerSeen = false
        while (!secondFingerSeen) {
            val event = awaitPointerEvent()
            val latest = event.changes.maxOfOrNull { it.uptimeMillis } ?: startTime
            if (latest - startTime > TWO_FINGER_WINDOW_MS) return@awaitEachGesture
            event.changes.forEach { change ->
                if (change.id !in starts && change.pressed && change.changedToDownIgnoreConsumed()) {
                    starts[change.id] = change.position
                    secondFingerSeen = true
                }
            }
            // If the (only) first finger is released before a second arrives, abort.
            if (!secondFingerSeen && event.changes.none { it.pressed }) return@awaitEachGesture
        }

        // Phase 3 — wait for release. Abort on drift or long-press timeout.
        while (true) {
            val event = awaitPointerEvent()
            val now = event.changes.maxOf { it.uptimeMillis }
            if (now - startTime > tapTimeoutMs) return@awaitEachGesture

            val drifted = event.changes.any { change ->
                val start = starts[change.id] ?: return@any false
                val dx = change.position.x - start.x
                val dy = change.position.y - start.y
                dx * dx + dy * dy > slopSquared
            }
            if (drifted) return@awaitEachGesture

            if (event.changes.none { it.pressed }) {
                onTwoFingerTap()
                return@awaitEachGesture
            }
        }
    }
}
