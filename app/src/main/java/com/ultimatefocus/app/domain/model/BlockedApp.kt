/**
 * Domain model representing an active or historical block on a given app.
 * Kept in the domain layer so ViewModels never touch the Room entity.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.domain.model

data class BlockedApp(
    val packageName: String,
    val blockedUntilEpochMs: Long,
    val blockedSinceEpochMs: Long,
    val durationLabel: String,
)
