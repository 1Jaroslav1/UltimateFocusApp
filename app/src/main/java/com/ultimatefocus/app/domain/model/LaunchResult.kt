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
 * Covers: ULT-21, ULT-24
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
}
