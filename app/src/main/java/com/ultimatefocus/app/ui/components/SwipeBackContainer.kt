/**
 * Wraps a NavHost-pushed screen so a horizontal swipe from the left edge
 * to the right pops the back stack — the standard iOS-style interactive-
 * back gesture, applied here so users can navigate away from Settings,
 * Blocking schedules, and the schedule editor without reaching for the
 * software back button.
 *
 * Implementation notes:
 *   - The detector lives at the screen's outermost edge (Modifier on the
 *     wrapping Box) so vertically-scrolling children continue to receive
 *     vertical drags. detectHorizontalDragGestures only consumes drags
 *     whose dominant axis is horizontal, so vertical scrolling is intact.
 *   - Only drags that begin within EDGE_START_FRACTION of the left edge
 *     count, so a horizontal swipe in the middle of the screen (e.g. on
 *     a chip row that owns horizontal gestures) is left alone.
 *   - The pop only fires when the cumulative horizontal drag exceeds
 *     SWIPE_THRESHOLD_DP — short flicks don't accidentally navigate away.
 *
 * Covers: ULT-23 (iteration)
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val SWIPE_THRESHOLD_DP = 80.dp
private const val EDGE_START_FRACTION = 0.15f

@Composable
fun SwipeBackContainer(
    onBack: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { SWIPE_THRESHOLD_DP.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!enabled) Modifier
                else Modifier.pointerInput(Unit) {
                    val edgeMaxX = size.width * EDGE_START_FRACTION
                    var totalDrag = 0f
                    var startedFromEdge = false
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            startedFromEdge = offset.x <= edgeMaxX
                            totalDrag = 0f
                        },
                        onDragEnd = {
                            if (startedFromEdge && totalDrag >= thresholdPx) onBack()
                            totalDrag = 0f
                            startedFromEdge = false
                        },
                        onDragCancel = {
                            totalDrag = 0f
                            startedFromEdge = false
                        },
                        onHorizontalDrag = { _, delta ->
                            if (startedFromEdge) totalDrag += delta
                        },
                    )
                },
            ),
    ) {
        content()
    }
}
