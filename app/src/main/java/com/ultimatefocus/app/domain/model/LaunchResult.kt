/**
 * Outcome of AppRepository.launchApp(). A sealed result instead of a side-
 * channel flow so callers can unit-test routing to the blocked reminder
 * without pulling in a Context.
 *
 * `Blocked` carries an optional [scheduleName] populated when the block
 * came from a recurring blocking schedule (ULT-24) — the reminder screen
 * uses it to render "Blocked by Work Focus until 17:00" instead of the
 * manual-block "Blocked for 1h" copy.
 *
 * `SessionDurationRequired` is emitted when the user attempts to open a
 * package with the In-App Reminder enabled and no active session yet
 * (ULT-78). The caller routes the user to the duration picker overlay
 * instead of completing the launch.
 *
 * Covers: ULT-21, ULT-24, ULT-78
 */
package com.ultimatefocus.app.domain.model

sealed interface LaunchResult {
    data object Launched : LaunchResult
    data class Blocked(
        val packageName: String,
        val appLabel: String,
        val remainingMs: Long,
        val durationLabel: String,
        val scheduleName: String? = null,
    ) : LaunchResult

    data class SessionDurationRequired(
        val packageName: String,
        val appLabel: String,
        /**
         * The user's globally-configured default expiry mode at the moment
         * launchApp() was called. Carried with the result so the picker
         * activity always sees a fresh value — relying on the launcher
         * Compose state would otherwise risk a stale read during the brief
         * window between process start and the DataStore Flow's first
         * emission.
         */
        val defaultMode: SessionExpiryMode,
    ) : LaunchResult
}
