/**
 * Gesture-detection Modifiers used by the home screen:
 * - homeSwipeGestures accumulates vertical drag and dispatches onSwipeUp /
 *   onSwipeDown once per gesture on release, when the accumulated movement
 *   crosses SWIPE_THRESHOLD_DP.
 * - homeTwoFingerTap fires onTwoFingerTap when two fingers land on the screen
 *   during the same short gesture and both lift without significant drag or
 *   exceeding the long-press timeout. The previous single-finger double-tap
 *   handler did not fire reliably in the launcher context, and a strict
 *   "must arrive within 200 ms" window made the replacement too picky in
 *   practice — all we require now is that both fingers come down before
 *   either lifts, and the whole gesture completes within the tap timeout
 *   (ULT-18).
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
 * Adds two-finger-tap detection to this node that only fires when [enabled]
 * is true. The gesture detector is keyed on [enabled] so a preference change
 * toggles the listener on/off without restarting the composition.
 *
 * Recognition rules (all must hold for the callback to fire):
 * - At least two distinct fingers are pressed during the gesture. The second
 *   finger may arrive after the first — the only bound is the tap timeout.
 * - No finger drifts more than `viewConfiguration.touchSlop` from its down
 *   position (i.e. the user tapped, did not swipe).
 * - All fingers lift within `viewConfiguration.longPressTimeoutMillis` of the
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
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        val startTime = firstDown.uptimeMillis
        val starts: MutableMap<PointerId, Offset> = mutableMapOf(firstDown.id to firstDown.position)

        while (true) {
            val event = awaitPointerEvent()
            val now = event.changes.maxOfOrNull { it.uptimeMillis } ?: startTime

            // Bail if the user is clearly doing a long-press instead of a tap.
            if (now - startTime > tapTimeoutMs) return@awaitEachGesture

            // Track every new finger that comes down during the window.
            event.changes.forEach { change ->
                if (change.id !in starts && change.pressed) {
                    starts[change.id] = change.position
                }
            }

            // Abort on drift so a swipe doesn't count as a two-finger tap.
            val drifted = event.changes.any { change ->
                val start = starts[change.id] ?: return@any false
                val dx = change.position.x - start.x
                val dy = change.position.y - start.y
                dx * dx + dy * dy > slopSquared
            }
            if (drifted) return@awaitEachGesture

            // When every tracked finger has lifted, fire only if at least two
            // distinct fingers were ever seen.
            if (event.changes.none { it.pressed }) {
                if (starts.size >= 2) onTwoFingerTap()
                return@awaitEachGesture
            }
        }
    }
}
