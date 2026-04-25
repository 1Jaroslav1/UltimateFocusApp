/**
 * Room entity representing a user-created tag. colorHex is constrained to
 * unique values via a unique index — second-line defense behind the
 * repository's pre-write check; serializes to one of the TagColor.hex values.
 * Covers: ULT-72
 */
package com.ultimatefocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [Index(value = ["colorHex"], unique = true)],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val colorHex: String,
    val createdAtEpochMs: Long,
)
