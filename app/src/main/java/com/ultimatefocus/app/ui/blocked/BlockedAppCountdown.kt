/**
 * Pure helper + Composable that drives the live countdown on
 * BlockedAppScreen. Extracted from the deleted BlockedAppViewModel so
 * the math is unit-testable on the JVM and the composable can be re-keyed
 * by blockedUntilEpochMs — when a new BlockedAppActivity intent arrives
 * (a different app being opened during an active reminder), the new
 * blockedUntilEpochMs causes the LaunchedEffect to restart with a fresh
 * tick stream and the screen re-renders against the new expiry.
 *
 * Covers: ULT-21, ULT-84
 */
package com.ultimatefocus.app.ui.blocked

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ultimatefocus.app.domain.service.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Pure: clamped remaining-millis between [now] and [blockedUntilEpochMs]. */
fun computeRemainingMs(blockedUntilEpochMs: Long, nowMs: Long): Long =
    (blockedUntilEpochMs - nowMs).coerceAtLeast(0L)

/**
 * UI state surface for a single countdown. Re-keys whenever
 * [blockedUntilEpochMs] changes so a re-entry into BlockedAppActivity
 * (different app2) starts a fresh tick stream against the new expiry.
 */
data class BlockedCountdownState(
    val remainingMs: Long,
    val shouldFinish: Boolean,
)

@Composable
fun rememberBlockedCountdown(
    blockedUntilEpochMs: Long,
    clock: Clock,
): State<BlockedCountdownState> {
    var nowMs by remember(blockedUntilEpochMs) { mutableLongStateOf(clock.now()) }

    LaunchedEffect(blockedUntilEpochMs) {
        nowMs = clock.now()
        while (isActive) {
            val remaining = computeRemainingMs(blockedUntilEpochMs, nowMs)
            if (remaining <= 0L) break
            delay(1_000L)
            nowMs = clock.now()
        }
    }

    return remember(blockedUntilEpochMs, clock) {
        derivedStateOf {
            val remaining = computeRemainingMs(blockedUntilEpochMs, nowMs)
            BlockedCountdownState(
                remainingMs = remaining,
                shouldFinish = remaining <= 0L,
            )
        }
    }
}
