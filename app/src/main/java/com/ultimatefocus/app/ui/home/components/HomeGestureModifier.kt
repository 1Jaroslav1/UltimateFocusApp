/**
 * Gesture-detection Modifiers used by the home screen:
 * - homeSwipeGestures accumulates vertical drag and dispatches onSwipeUp /
 *   onSwipeDown once per gesture on release, when the accumulated movement
 *   crosses SWIPE_THRESHOLD_DP.
 * - homeTwoFingerDoubleTap fires onTwoFingerDoubleTap when the user performs
 *   two consecutive two-finger taps within DOUBLE_TAP_GAP_MS. Each tap is a
 *   brief press with at least two simultaneous fingers, released inside the
 *   system long-press timeout and without drifting past touchSlop.
 * - homeMultiTouchDebug is a temporary diagnostic that toasts "N fingers
 *   detected" while we iterate on ULT-18 on-device; not wired into the
 *   production home screen.
 *
 * All detectors await on PointerEventPass.Initial so events are observed
 * before the parent HorizontalPager's Main-pass arbitration can claim
 * multi-touch for a page drag.
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput

/** Minimum accumulated vertical drag (in dp) that counts as a swipe. */
const val SWIPE_THRESHOLD_DP: Float = 60f

/**
 * Max gap between two-finger-tap #1 release and two-finger-tap #2 first down.
 * 700 ms is deliberately generous — humans naturally double-tap at ~300-600 ms
 * and stock Android's ViewConfiguration.getDoubleTapTimeout() is 300 ms, but
 * two-finger gestures take a touch longer to re-plant than one-finger ones.
 */
const val DOUBLE_TAP_GAP_MS: Long = 700L

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
 * Adds two-finger double-tap detection to this node that only fires when
 * [enabled] is true. The pointer input is keyed on [enabled] so a preference
 * flip restarts the detector without requiring a full recomposition.
 *
 * One tap qualifies as "two-finger" when the gesture saw at least two
 * simultaneous pointers pressed, no finger drifted past touchSlop, and every
 * finger lifted within the system long-press timeout. Two such taps with a
 * gap of at most DOUBLE_TAP_GAP_MS between the first release and the
 * second's first-down fire the callback. Any non-qualifying gesture in
 * between resets the pairing.
 */
fun Modifier.homeTwoFingerDoubleTap(
    enabled: Boolean,
    onTwoFingerDoubleTap: () -> Unit,
    onSingleTapForDebug: (() -> Unit)? = null,
    onDebugEvent: ((String) -> Unit)? = null,
): Modifier = this.pointerInput(enabled) {
    // Fires on every coroutine start so we can tell whether the modifier is
    // armed or whether the stored preference is OFF. Keyed on `enabled`, so
    // the coroutine restarts (and retoasts) whenever the pref flips.
    onDebugEvent?.invoke(
        if (enabled) "detector armed" else "detector OFF (toggle in Settings)",
    )
    if (!enabled) return@pointerInput
    val slop = viewConfiguration.touchSlop
    val slopSquared = slop * slop
    val tapTimeoutMs = viewConfiguration.longPressTimeoutMillis

    // `recognizeTwoFingerTap` is an AwaitPointerEventScope extension, so we
    // need to be inside awaitPointerEventScope — pointerInput itself only
    // gives us PointerInputScope, which doesn't allow calling awaitPointerEvent
    // directly.
    awaitPointerEventScope {
        var previousTapEndMs: Long? = null
        while (true) {
            val tap = recognizeTwoFingerTap(slopSquared, tapTimeoutMs, onDebugEvent)
            if (tap == null) {
                // Non-qualifying gesture — abandon any pending pairing.
                previousTapEndMs = null
                continue
            }
            onSingleTapForDebug?.invoke()
            val (tapStart, tapEnd) = tap
            val prev = previousTapEndMs
            if (prev != null) {
                val gap = tapStart - prev
                if (gap <= DOUBLE_TAP_GAP_MS) {
                    onDebugEvent?.invoke("gap ${gap}ms → pair")
                    previousTapEndMs = null
                    onTwoFingerDoubleTap()
                } else {
                    onDebugEvent?.invoke("gap ${gap}ms → too slow, restarting")
                    previousTapEndMs = tapEnd
                }
            } else {
                previousTapEndMs = tapEnd
            }
        }
    }
}

/**
 * Waits for the next gesture and decides whether it qualifies as a brief
 * two-finger tap. Returns (startTime, endTime) on success, null otherwise.
 *
 * Does NOT use awaitFirstDown() because that helper only exits when every
 * change in the event is a down-transition — if finger 2 comes down in the
 * same event as finger 1, awaitFirstDown skips the event entirely and the
 * whole gesture can be missed. We roll a tiny manual loop that exits on the
 * first event containing any down-transition, then seed `starts` from every
 * pointer that's pressed in that event.
 */
private suspend fun AwaitPointerEventScope.recognizeTwoFingerTap(
    slopSquared: Float,
    tapTimeoutMs: Long,
    onDebugEvent: ((String) -> Unit)? = null,
): Pair<Long, Long>? {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Initial)
    } while (event.changes.none { it.changedToDownIgnoreConsumed() })

    val starts: MutableMap<PointerId, Offset> = mutableMapOf()
    event.changes.forEach { ch ->
        if (ch.pressed) starts[ch.id] = ch.position
    }
    if (starts.isEmpty()) {
        onDebugEvent?.invoke("reject: first event had no pressed pointers")
        return null
    }
    val startTime = event.changes.filter { it.pressed }.minOf { it.uptimeMillis }
    var sawTwoFingers = starts.size >= 2

    while (true) {
        event = awaitPointerEvent(PointerEventPass.Initial)
        val now = event.changes.maxOfOrNull { it.uptimeMillis } ?: startTime

        if (now - startTime > tapTimeoutMs) {
            onDebugEvent?.invoke("reject: > ${tapTimeoutMs}ms (hold, not tap)")
            return null
        }

        event.changes.forEach { ch ->
            if (ch.id !in starts && ch.pressed) {
                starts[ch.id] = ch.position
                if (starts.size >= 2) sawTwoFingers = true
            }
        }

        val drifted = event.changes.any { ch ->
            val start = starts[ch.id] ?: return@any false
            val dx = ch.position.x - start.x
            val dy = ch.position.y - start.y
            dx * dx + dy * dy > slopSquared
        }
        if (drifted) {
            onDebugEvent?.invoke("reject: finger drifted past touchSlop")
            return null
        }

        if (event.changes.none { it.pressed }) {
            return if (sawTwoFingers) {
                startTime to now
            } else {
                onDebugEvent?.invoke("reject: released with only 1 finger seen")
                null
            }
        }
    }
    @Suppress("UNREACHABLE_CODE") return null
}

/**
 * Diagnostic-only Modifier that toasts "N fingers detected" the first time
 * each pointer count (≥ 2) is observed within a single gesture cycle.
 * Not applied by the production HomeScreen any more now that Initial-pass
 * multi-touch delivery has been confirmed on device, but kept here so it
 * can be re-attached when a future gesture bug needs investigation.
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
