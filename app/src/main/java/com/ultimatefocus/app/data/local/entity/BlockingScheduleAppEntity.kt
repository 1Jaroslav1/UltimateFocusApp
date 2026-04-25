/**
 * Junction-table entity binding apps to a BlockingScheduleEntity. Composite
 * primary key (scheduleId, packageName) prevents duplicate assignments.
 * onDelete=CASCADE removes assignments when their parent schedule is deleted
 * so the editor doesn't have to manually clean junction rows.
 * Covers: ULT-24
 */
package com.ultimatefocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "blocking_schedule_apps",
    primaryKeys = ["scheduleId", "packageName"],
    foreignKeys = [
        ForeignKey(
            entity = BlockingScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("scheduleId"),
        Index("packageName"),
    ],
)
data class BlockingScheduleAppEntity(
    val scheduleId: Long,
    val packageName: String,
)
