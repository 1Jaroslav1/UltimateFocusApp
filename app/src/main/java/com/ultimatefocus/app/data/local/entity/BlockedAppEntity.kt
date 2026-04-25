/**
 * Room entity representing a manual app block. One row per blocked package;
 * a block is "active" iff blockedUntilEpochMs > now() at read time.
 * durationLabel preserves the user's original choice ("1h", "24h", "30d") so
 * the reminder screen can show it without reverse-engineering from timestamps.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey
    val packageName: String,
    val blockedUntilEpochMs: Long,
    val blockedSinceEpochMs: Long,
    val durationLabel: String,
)
