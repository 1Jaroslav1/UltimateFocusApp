/**
 * Watches the active session's clock and emits exactly one
 * SessionExpiryEffect when the session reaches zero. BlockedAppAccessibility-
 * Service consumes the events and invokes the actual Android side effects
 * (goHome, block, start the overlay activity, post a notification) — the
 * AccessibilityService context is used so background activity launches
 * work on Android 10+ (app-context launches are forbidden there). This
 * class stays JVM-pure and unit-testable with virtual time.
 *
 * Latching by (packageName, startedEpochMs) ensures one effect per
 * session even if multiple ticks see remaining ≤ 0 before the
 * coordinator clears the active slot.
 *
 * Covers: ULT-81
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.ActiveSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class SessionExpiryWatcher @Inject constructor(
    private val coordinator: SessionTimerCoordinator,
    private val clock: Clock,
) {

    private var lastDispatchedKey: ExpiryKey? = null

    private data class ExpiryKey(val packageName: String, val startedEpochMs: Long)

    fun expiryEffects(tickIntervalMs: Long = DEFAULT_TICK_MS): Flow<SessionExpiryEffect> {
        val ticker = ticker(tickIntervalMs)
        return combine(coordinator.active, ticker) { session, _ -> session }
            .mapNotNull { session -> emitIfExpired(session) }
    }

    private fun emitIfExpired(session: ActiveSession?): SessionExpiryEffect? {
        if (session == null) {
            // Session ended (or watcher started before any session) — release
            // the latch so the next session is eligible.
            lastDispatchedKey = null
            return null
        }
        val remaining = session.endsEpochMs - clock.now()
        if (remaining > 0L) return null
        val key = ExpiryKey(session.packageName, session.startedEpochMs)
        if (key == lastDispatchedKey) return null
        lastDispatchedKey = key
        return SessionExpiryDispatcher.effectFor(session)
    }

    private fun ticker(intervalMs: Long): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(intervalMs.milliseconds)
        }
    }

    companion object {
        const val DEFAULT_TICK_MS: Long = 1_000L
    }
}
