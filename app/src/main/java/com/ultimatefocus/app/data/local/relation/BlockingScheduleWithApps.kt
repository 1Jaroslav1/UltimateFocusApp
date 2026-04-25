/**
 * Room @Relation projection that pulls a BlockingScheduleEntity together
 * with the package names assigned to it from the junction table in a single
 * @Transaction-bound query. Lets repository code map straight to the domain
 * BlockingSchedule without a manual JOIN.
 * Covers: ULT-24
 */
package com.ultimatefocus.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.ultimatefocus.app.data.local.entity.BlockingScheduleAppEntity
import com.ultimatefocus.app.data.local.entity.BlockingScheduleEntity

data class BlockingScheduleWithApps(
    @Embedded val schedule: BlockingScheduleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scheduleId",
    )
    val apps: List<BlockingScheduleAppEntity>,
)
