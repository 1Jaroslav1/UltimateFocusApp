/**
 * Gesture-detection Modifiers used by the home screen:
 * - homeSwipeGestures accumulates vertical drag and dispatches onSwipeUp /
 *   onSwipeDown once per gesture on release, when the accumulated movement
 *   crosses SWIPE_THRESHOLD_DP.
 * - homeTwoFingerLongPress fires onTwoFingerLongPress after two fingers have
 *   been held still (within touchSlop) on the screen for
 *   `viewConfiguration.longPressTimeoutMillis`. The previous single-finger
 *   double-tap and quick two-finger tap variants collided with the parent
 *   HorizontalPager's multi-touch arbitration on some devices; a sustained
 *   hold sidesteps that because the pager only reacts to horizontal drag.
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
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/** Minimum accumulated vertical drag (in dp) that counts as a swipe. */
const val SWIPE_THRESHOLD_DP: Float = 60f

/** Max delay between the first and second finger landing to be treated as the same gesture. */
const val TWO_FINGER_WINDOW_MS: Long = 400L

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
 * Adds two-finger long-press detection to this node that only fires when
 * [enabled] is true. Keyed on [enabled] so a preference flip swaps the
 * listener on/off without a full recomposition.
 *
 * Recognition rules (all must hold for the callback to fire):
 * - Two distinct fingers land on the node, with the second arriving within
 *   TWO_FINGER_WINDOW_MS of the first.
 * - Both fingers stay pressed for the system long-press timeout
 *   (`viewConfiguration.longPressTimeoutMillis`, usually ~500 ms).
 * - Neither finger drifts more than `viewConfiguration.touchSlop` from its
 *   down position during the hold.
 *
 * The callback fires exactly once per qualifying gesture, the moment the
 * long-press timer elapses with both fingers still held.
 */
fun Modifier.homeTwoFingerLongPress(
    enabled: Boolean,
    onTwoFingerLongPress: () -> Unit,
): Modifier = this.pointerInput(enabled) {
    if (!enabled) return@pointerInput
    val slop = viewConfiguration.touchSlop
    val slopSquared = slop * slop
    val holdTimeoutMs = viewConfiguration.longPressTimeoutMillis

    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false)
        val firstStart = first.position

        // Phase 1 — wait for a second finger to land.
        val second = withTimeoutOrNull(TWO_FINGER_WINDOW_MS) {
            while (true) {
                val event = awaitPointerEvent()
                val other = event.changes.firstOrNull { it.id != first.id && it.pressed }
                if (other != null) return@withTimeoutOrNull other
                if (event.changes.all { !it.pressed }) return@withTimeoutOrNull null
            }
            @Suppress("UNREACHABLE_CODE") null
        } ?: return@awaitEachGesture

        val secondStart = second.position

        // Phase 2 — require both fingers to stay held still for the long-press
        // duration. If the inner block finishes (early return) we do NOT fire;
        // if withTimeoutOrNull times out (null) both fingers held, so we fire.
        val heldLongEnough = withTimeoutOrNull(holdTimeoutMs) {
            while (true) {
                val event = awaitPointerEvent()
                val trackedLifted = event.changes.any { ch ->
                    (ch.id == first.id || ch.id == second.id) && !ch.pressed
                }
                if (trackedLifted) return@withTimeoutOrNull false
                val drifted = event.changes.any { ch ->
                    val start = when (ch.id) {
                        first.id -> firstStart
                        second.id -> secondStart
                        else -> return@any false
                    }
                    val dx = ch.position.x - start.x
                    val dy = ch.position.y - start.y
                    dx * dx + dy * dy > slopSquared
                }
                if (drifted) return@withTimeoutOrNull false
            }
            @Suppress("UNREACHABLE_CODE") true
        }
        if (heldLongEnough == null) onTwoFingerLongPress()
    }
}
