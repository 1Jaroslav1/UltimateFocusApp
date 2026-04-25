/**
 * Outcome of AppRepository.launchApp(). A sealed result instead of a side-
 * channel flow so callers can unit-test routing to the blocked reminder
 * without pulling in a Context.
 * Covers: ULT-21
 */
package com.ultimatefocus.app.domain.model

sealed interface LaunchResult {
    data object Launched : LaunchResult
    data class Blocked(
        val packageName: String,
        val appLabel: String,
        val remainingMs: Long,
        val durationLabel: String,
    ) : LaunchResult
}
