/**
 * Room entity representing an active block on a tag. One row per blocked
 * tag; a block is "active" iff blockedUntilEpochMs > now() at read time.
 * durationLabel preserves the user's original choice ("1h", "24h", "30d") so
 * the UI can show it next to the lock badge without reverse-engineering
 * from timestamps.
 * ON DELETE CASCADE on tagId means deleting a tag automatically prunes its
 * blocked_tags row — the FK is the safety net behind explicit cleanup.
 * Covers: ULT-75
 */
package com.ultimatefocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_tags",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class BlockedTagEntity(
    @PrimaryKey
    val tagId: Long,
    val blockedUntilEpochMs: Long,
    val blockedSinceEpochMs: Long,
    val durationLabel: String,
)
