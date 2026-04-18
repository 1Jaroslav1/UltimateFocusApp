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

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
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
        // All awaits on the INITIAL pass so we see pointer events before the
        // parent HorizontalPager's Main-pass arbitration decides to consume
        // them. Critical on devices where the pager greedily claims the
        // second finger for a page drag.
        val first = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        val firstStart = first.position

        // Phase 1 — wait for a second finger to land.
        val second = withTimeoutOrNull(TWO_FINGER_WINDOW_MS) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
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
                val event = awaitPointerEvent(PointerEventPass.Initial)
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

/**
 * Diagnostic-only Modifier that toasts "N fingers detected" the first time
 * each pointer count (≥ 2) is observed within a single gesture cycle.
 * Added while debugging ULT-18 on device — if the user sees the toast we
 * know multi-touch is reaching the Box and the real detector is the
 * suspect. If they don't, a parent node is swallowing the second finger.
 */
fun Modifier.homeMultiTouchDebug(context: Context): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val seenIds = mutableSetOf<PointerId>()
        var lastToastedCount = 0
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.forEach { if (it.pressed) seenIds.add(it.id) }
            val concurrent = event.changes.count { it.pressed }
            if (concurrent >= 2 && concurrent > lastToastedCount) {
                lastToastedCount = concurrent
                Toast.makeText(context, "$concurrent fingers detected", Toast.LENGTH_SHORT).show()
            }
            if (event.changes.none { it.pressed }) return@awaitEachGesture
        }
    }
}
