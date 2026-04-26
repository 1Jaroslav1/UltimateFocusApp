/**
 * Pure-logic decision rule behind SessionWarningController. Given the
 * current ActiveSession (or null) and the current wall-clock instant,
 * decides whether the 1-minute pre-end warning popup should fire RIGHT
 * NOW. Returns true at most once per session — repeated calls within
 * the same session keep returning false until either the session ends
 * (cleared via reset()) or a new session begins.
 *
 * Encoded outside the controller so the timing rule is unit-testable
 * without driving a real ticker Flow or Toast surface.
 *
 * Threshold is parameterized for tests (production uses 60_000ms per AC).
 *
 * Covers: ULT-80
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.ActiveSession

class SessionWarningStateMachine {

    private var lastWarnedKey: WarningKey? = null

    private data class WarningKey(val packageName: String, val startedEpochMs: Long)

    /**
     * Returns true exactly once per session when the session has 0 < remaining
     * <= [thresholdMs] left. Returns false outside that window, when no
     * session is active, when the session has already expired, and on every
     * call after the first warning for the same session.
     *
     * Clamping `remaining > 0` matters: at expiry the session-end behavior
     * mode (ULT-81) takes over, so we don't double-fire a "1 minute left"
     * popup against a zero/negative remaining.
     */
    fun shouldWarn(
        session: ActiveSession?,
        nowMs: Long,
        thresholdMs: Long = DEFAULT_THRESHOLD_MS,
    ): Boolean {
        if (session == null) {
            // Session ended — release the latch so the NEXT session can warn.
            lastWarnedKey = null
            return false
        }
        val remaining = session.endsEpochMs - nowMs
        if (remaining <= 0L || remaining > thresholdMs) return false
        val key = WarningKey(session.packageName, session.startedEpochMs)
        if (key == lastWarnedKey) return false
        lastWarnedKey = key
        return true
    }

    fun reset() { lastWarnedKey = null }

    companion object {
        const val DEFAULT_THRESHOLD_MS: Long = 60_000L
    }
}
