/**
 * Snapshot of an in-flight session. Lives in memory only (held by
 * SessionTimerCoordinator) — sessions are intrinsically a foreground
 * concept and there is no value to surviving a process death since the
 * user will be back on the home screen anyway. Persisting a session would
 * also conflict with the "user must commit each time" friction that the
 * feature is built around.
 *
 * The countdown only runs while [packageName] is actually foreground:
 * [resumedAtEpochMs] is the timestamp the session most recently became
 * foreground-active (null means currently paused because the user
 * switched to a different app), and [consumedMsBeforePause] banks the
 * active time accumulated across any earlier pause/resume cycles. This
 * ensures "N minutes" means N minutes of use, and that expiry can only
 * ever be observed while this exact package is foreground (ULT-86).
 *
 * Covers: ULT-78, ULT-86
 */
package com.ultimatefocus.app.domain.model

data class ActiveSession(
    val packageName: String,
    val appLabel: String,
    val startedEpochMs: Long,
    val durationMinutes: Int,
    val expiryMode: SessionExpiryMode,
    val consumedMsBeforePause: Long = 0L,
    val resumedAtEpochMs: Long? = startedEpochMs,
) {
    val totalBudgetMs: Long get() = durationMinutes * 60_000L

    val isPaused: Boolean get() = resumedAtEpochMs == null

    fun remainingMs(nowMs: Long): Long {
        val activeMs = consumedMsBeforePause + (resumedAtEpochMs?.let { nowMs - it } ?: 0L)
        return (totalBudgetMs - activeMs).coerceAtLeast(0L)
    }
}
