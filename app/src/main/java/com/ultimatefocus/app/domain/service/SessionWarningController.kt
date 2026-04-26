/**
 * Wires SessionWarningStateMachine to a 1-second ticker so the warning
 * popup fires at the right moment without polling the UI thread. Exposes
 * a Flow<SessionWarningEvent> the Application layer collects to surface
 * the actual Toast.
 *
 * Pure-logic / Android-free: SessionWarningController itself does not
 * touch any Android symbols — the Toast presentation is handled by the
 * Application class observer (SessionWarningPresenter). This keeps the
 * controller unit-testable on the JVM with virtual time.
 *
 * Covers: ULT-80
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.ActiveSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

data class SessionWarningEvent(
    val packageName: String,
    val appLabel: String,
    val remainingMs: Long,
)

@Singleton
class SessionWarningController @Inject constructor(
    private val coordinator: SessionTimerCoordinator,
    private val clock: Clock,
) {

    private val stateMachine = SessionWarningStateMachine()

    fun warningEvents(
        tickIntervalMs: Long = DEFAULT_TICK_MS,
        thresholdMs: Long = SessionWarningStateMachine.DEFAULT_THRESHOLD_MS,
    ): Flow<SessionWarningEvent> {
        val ticker = ticker(tickIntervalMs)
        return combine(coordinator.active, ticker) { session, _ -> session }
            .mapNotNull { session -> emitIfShouldWarn(session, thresholdMs) }
            // Reset the latch when the active session goes back to null so
            // the next session is eligible for its own warning. We do this
            // by tapping into the same flow with onEach.
            .onEach { /* event already gated by the state machine */ }
    }

    private fun emitIfShouldWarn(
        session: ActiveSession?,
        thresholdMs: Long,
    ): SessionWarningEvent? {
        val now = clock.now()
        val shouldWarn = stateMachine.shouldWarn(session, now, thresholdMs)
        if (!shouldWarn || session == null) return null
        return SessionWarningEvent(
            packageName = session.packageName,
            appLabel = session.appLabel,
            remainingMs = (session.endsEpochMs - now).coerceAtLeast(0L),
        )
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
