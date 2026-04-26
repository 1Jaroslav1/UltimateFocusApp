/**
 * Room entity representing one app's opt-in to the In-App Time Reminder.
 * One row per package; presence means "the duration picker should appear
 * when this app comes to the foreground" — provided the global master
 * switch in UserPreferencesSource is also on. addedEpochMs lets us sort
 * the per-app rules by recency in any future audit UI without forcing a
 * separate metadata table.
 *
 * Covers: ULT-77
 */
package com.ultimatefocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_timer_apps")
data class SessionTimerAppEntity(
    @PrimaryKey
    val packageName: String,
    val addedEpochMs: Long,
)
