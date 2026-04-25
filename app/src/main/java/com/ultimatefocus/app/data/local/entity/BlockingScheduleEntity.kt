/**
 * Room entity representing a recurring blocking schedule. One row per
 * user-defined window; the assigned packages live in a separate junction
 * table (BlockingScheduleAppEntity) so a single schedule can target many
 * apps and a single app can belong to many schedules.
 *
 * Time is stored as minute-of-day (0..1439) in the device's local zone so
 * the schedule survives DST and travel without the user re-editing it. If
 * endMinuteOfDay <= startMinuteOfDay the window crosses midnight.
 *
 * daysOfWeekMask is a 7-bit ISO bitmask (bit (DayOfWeek.value - 1):
 * MONDAY=bit0, SUNDAY=bit6).
 *
 * Covers: ULT-24
 */
package com.ultimatefocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocking_schedules")
data class BlockingScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val daysOfWeekMask: Int,
    val createdEpochMs: Long,
)
