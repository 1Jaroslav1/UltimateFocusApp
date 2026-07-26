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
 * fully exits. The countdown itself pauses/resumes via
 * [onForegroundPackageChanged] as the foreground package changes, so it
 * only ticks down while the session's own package is actually in use
 * (ULT-86). Two independent producers call this: BlockedAppAccessibility-
 * Service (immediate, but silent whenever the OS suspends/kills the
 * accessibility service) and ForegroundPoller (a ~1s UsageStatsManager
 * poll, accessibility-free) — the poller exists specifically so pause/resume
 * still works when the accessibility service is dead, matching
 * SessionExpiryRunner's own "process-lifetime, accessibility-independent"
 * dispatch guarantee. Process death implicitly clears the session since the
 * holder is process-scoped.
 *
 * Covers: ULT-78, ULT-86
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
        return s.packageName == packageName && s.remainingMs(clock.now()) > 0L
    }

    /** Milliseconds left in the active session for [packageName], or null. */
    fun remainingMsFor(packageName: String): Long? {
        val s = _active.value ?: return null
        if (s.packageName != packageName) return null
        return s.remainingMs(clock.now())
    }

    /**
     * Pauses the active session's countdown when the foreground package
     * moves away from it, and resumes it when the foreground package
     * comes back — so the countdown only ever runs while its own package
     * is actually in use. System surfaces (keyboard, System UI, etc.) are
     * ignored so they don't pause an in-progress session. Covers ULT-86.
     */
    fun onForegroundPackageChanged(packageName: String?, nowMs: Long) {
        if (packageName.isNullOrBlank()) return
        if (packageName in BlockedAppForegroundHandler.SYSTEM_PACKAGES) return
        val session = _active.value ?: return
        if (packageName == session.packageName) {
            if (session.isPaused) {
                _active.value = session.copy(resumedAtEpochMs = nowMs)
            }
        } else {
            val resumedAt = session.resumedAtEpochMs
            if (resumedAt != null) {
                _active.value = session.copy(
                    consumedMsBeforePause = session.consumedMsBeforePause + (nowMs - resumedAt),
                    resumedAtEpochMs = null,
                )
            }
        }
    }

    fun endSession() { _active.value = null }
}
