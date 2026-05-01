/**
 * Gesture-detection Modifiers used by the home screen:
 * - homeSwipeGestures accumulates vertical drag and dispatches onSwipeUp /
 *   onSwipeDown once per gesture on release, when the accumulated movement
 *   crosses SWIPE_THRESHOLD_DP. Observes events on PointerEventPass.Initial
 *   so the gesture is detected even when the touch starts inside a child
 *   that consumes vertical drags (such as FavoritesList's
 *   `verticalScroll(...)` column). Once the swipe threshold has been
 *   crossed, the drag delta is consumed so the child stops scrolling and
 *   the home gesture claims the rest of the drag (ULT-82 #3).
 * - homeTwoFingerDoubleTap fires onTwoFingerDoubleTap when the user performs
 *   two consecutive two-finger taps within DOUBLE_TAP_GAP_MS. Each tap is a
 *   brief press with at least two simultaneous fingers, released inside the
 *   system long-press timeout and without drifting past touchSlop.
 *
 * Both detectors await on PointerEventPass.Initial so events are observed
 * before the parent HorizontalPager's / FavoritesList's Main-pass
 * arbitration can claim them.
 *
 * Covers: ULT-16, ULT-17, ULT-18, ULT-82
 */
package com.ultimatefocus.app.ui.home.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

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
 *
 * Observes on PointerEventPass.Initial so the detector sees pointer events
 * before child scroll containers (FavoritesList uses `verticalScroll`)
 * consume them — a swipe-up that begins on a favorite row still triggers
 * the home gesture. Per-event drag deltas are NOT consumed until the
 * vertical threshold has been crossed, so short flicks still let the
 * favorites list scroll normally; once the threshold trips, the drag is
 * consumed and the favorites stop receiving deltas (the home gesture has
 * claimed the rest of the drag).
 */
fun Modifier.homeSwipeGestures(
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
): Modifier = this.pointerInput(Unit) {
    val thresholdPx = SWIPE_THRESHOLD_DP * density
    awaitPointerEventScope {
        while (true) {
            // Wait for the first down event (any pointer).
            var event: PointerEvent
            do {
                event = awaitPointerEvent(PointerEventPass.Initial)
            } while (event.changes.none { it.changedToDownIgnoreConsumed() })

            val anchorByPointer: MutableMap<PointerId, Offset> = mutableMapOf()
            event.changes.forEach { ch ->
                if (ch.pressed) anchorByPointer[ch.id] = ch.position
            }
            var lastY = event.changes.firstOrNull { it.pressed }?.position?.y ?: 0f
            var totalDy = 0f
            var totalAbsDx = 0f
            var thresholdCrossed = false

            // Track the gesture until all pointers are up.
            while (true) {
                event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { ch ->
                    if (ch.id !in anchorByPointer && ch.pressed) {
                        anchorByPointer[ch.id] = ch.position
                    }
                }

                // Use the primary tracked pointer's position to derive deltas.
                val primary = event.changes.firstOrNull { it.id in anchorByPointer && it.pressed }
                if (primary != null) {
                    val dy = primary.position.y - lastY
                    val dx = primary.position.x - (anchorByPointer[primary.id]?.x ?: primary.position.x)
                    totalDy += dy
                    totalAbsDx += abs(dx)
                    lastY = primary.position.y

                    if (!thresholdCrossed && abs(totalDy) >= thresholdPx &&
                        abs(totalDy) > totalAbsDx
                    ) {
                        thresholdCrossed = true
                    }
                    if (thresholdCrossed) {
                        // Consume the position change so child scrollables stop
                        // moving — we've claimed this gesture as a home swipe.
                        event.changes.forEach { it.consume() }
                    }
                }

                if (event.changes.none { it.pressed }) {
                    if (thresholdCrossed) {
                        when {
                            totalDy <= -thresholdPx -> onSwipeUp()
                            totalDy >= thresholdPx -> onSwipeDown()
                        }
                    }
                    break
                }
            }
        }
    }
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
): Modifier = this.pointerInput(enabled) {
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
            val tap = recognizeTwoFingerTap(slopSquared, tapTimeoutMs)
            if (tap == null) {
                previousTapEndMs = null
                continue
            }
            val (tapStart, tapEnd) = tap
            val prev = previousTapEndMs
            if (prev != null && tapStart - prev <= DOUBLE_TAP_GAP_MS) {
                previousTapEndMs = null
                onTwoFingerDoubleTap()
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
): Pair<Long, Long>? {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Initial)
    } while (event.changes.none { it.changedToDownIgnoreConsumed() })

    val starts: MutableMap<PointerId, Offset> = mutableMapOf()
    event.changes.forEach { ch ->
        if (ch.pressed) starts[ch.id] = ch.position
    }
    if (starts.isEmpty()) return null
    val startTime = event.changes.filter { it.pressed }.minOf { it.uptimeMillis }
    var sawTwoFingers = starts.size >= 2

    while (true) {
        event = awaitPointerEvent(PointerEventPass.Initial)
        val now = event.changes.maxOfOrNull { it.uptimeMillis } ?: startTime

        if (now - startTime > tapTimeoutMs) return null

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
        if (drifted) return null

        if (event.changes.none { it.pressed }) {
            return if (sawTwoFingers) startTime to now else null
        }
    }
    @Suppress("UNREACHABLE_CODE") return null
}
