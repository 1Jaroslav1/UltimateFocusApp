/**
 * Pure-logic mapping from a session's chosen expiry mode to the concrete
 * effect that should fire when the timer hits zero. Encoded as data so
 * the watcher (which holds the Android-side surface to actually run the
 * effect) is a thin shell and the rule stays unit-testable.
 *
 * Effect catalog:
 *  - GoHome — leave the user where they are if they want; default to
 *    notification only (matches AC: "no forced action").
 *  - LaunchOverlayCountdown — fullscreen "time's up" + countdown before
 *    the Extend button is tappable (Mode 1 — Mindful extension delay).
 *  - GoHomeOnly — close the app, return to home (Mode 2 — Exit app).
 *  - GoHomeAndBlock — close the app + block it for the same minutes the
 *    user just spent inside (Mode 3 — Exit + auto-block).
 *  - ShowReminderOnly — toast / notification, no eviction (Mode 4).
 *
 * Covers: ULT-81
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.ActiveSession
import com.ultimatefocus.app.domain.model.SessionExpiryMode

sealed interface SessionExpiryEffect {
    val packageName: String
    val appLabel: String

    data class LaunchOverlayCountdown(
        override val packageName: String,
        override val appLabel: String,
    ) : SessionExpiryEffect

    data class GoHomeOnly(
        override val packageName: String,
        override val appLabel: String,
    ) : SessionExpiryEffect

    data class GoHomeAndBlock(
        override val packageName: String,
        override val appLabel: String,
        val blockDurationMinutes: Int,
    ) : SessionExpiryEffect

    data class ShowReminderOnly(
        override val packageName: String,
        override val appLabel: String,
    ) : SessionExpiryEffect
}

object SessionExpiryDispatcher {
    fun effectFor(session: ActiveSession): SessionExpiryEffect = when (session.expiryMode) {
        SessionExpiryMode.MINDFUL_EXTENSION_DELAY -> SessionExpiryEffect.LaunchOverlayCountdown(
            packageName = session.packageName,
            appLabel = session.appLabel,
        )
        SessionExpiryMode.EXIT_APP -> SessionExpiryEffect.GoHomeOnly(
            packageName = session.packageName,
            appLabel = session.appLabel,
        )
        SessionExpiryMode.EXIT_AND_BLOCK -> SessionExpiryEffect.GoHomeAndBlock(
            packageName = session.packageName,
            appLabel = session.appLabel,
            blockDurationMinutes = session.durationMinutes,
        )
        SessionExpiryMode.ONLY_REMINDER -> SessionExpiryEffect.ShowReminderOnly(
            packageName = session.packageName,
            appLabel = session.appLabel,
        )
    }
}
