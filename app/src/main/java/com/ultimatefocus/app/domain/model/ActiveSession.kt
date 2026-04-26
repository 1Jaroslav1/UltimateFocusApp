/**
 * Snapshot of an in-flight session. Lives in memory only (held by
 * SessionTimerCoordinator) — sessions are intrinsically a foreground
 * concept and there is no value to surviving a process death since the
 * user will be back on the home screen anyway. Persisting a session would
 * also conflict with the "user must commit each time" friction that the
 * feature is built around.
 *
 * Covers: ULT-78
 */
package com.ultimatefocus.app.domain.model

data class ActiveSession(
    val packageName: String,
    val appLabel: String,
    val startedEpochMs: Long,
    val durationMinutes: Int,
    val expiryMode: SessionExpiryMode,
) {
    val endsEpochMs: Long get() = startedEpochMs + durationMinutes * 60_000L
}
