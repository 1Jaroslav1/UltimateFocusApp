/**
 * In-memory holder for the at-most-one ActiveSession. Owns the lifecycle
 * of a session committed via the duration picker (ULT-78), and is the
 * single source of truth that downstream consumers use to:
 *  - skip the launch interceptor for the package whose session is in
 *    progress (don't ask again until the session ends).
 *  - drive the 1-minute warning popup (ULT-80).
 *  - dispatch the session-end behavior mode (ULT-81).
 *
 * `endSession` and the auto-derived expiry are the only ways the session
 * exits — there is no "resume" semantic. Process death implicitly clears
 * the session since the holder is process-scoped.
 *
 * Covers: ULT-78
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.ActiveSession
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionTimerCoordinator @Inject constructor(
    private val clock: Clock,
) {
    private val _active = MutableStateFlow<ActiveSession?>(null)
    val active: StateFlow<ActiveSession?> = _active.asStateFlow()

    fun startSession(
        packageName: String,
        appLabel: String,
        durationMinutes: Int,
        expiryMode: SessionExpiryMode,
    ): ActiveSession {
        val session = ActiveSession(
            packageName = packageName,
            appLabel = appLabel,
            startedEpochMs = clock.now(),
            durationMinutes = durationMinutes,
            expiryMode = expiryMode,
        )
        _active.value = session
        return session
    }

    /** Returns true when [packageName] currently has an in-progress session. */
    fun isSessionActiveFor(packageName: String): Boolean {
        val s = _active.value ?: return false
        return s.packageName == packageName && clock.now() < s.endsEpochMs
    }

    /** Milliseconds left in the active session for [packageName], or null. */
    fun remainingMsFor(packageName: String): Long? {
        val s = _active.value ?: return null
        if (s.packageName != packageName) return null
        return (s.endsEpochMs - clock.now()).coerceAtLeast(0L)
    }

    fun endSession() { _active.value = null }
}
