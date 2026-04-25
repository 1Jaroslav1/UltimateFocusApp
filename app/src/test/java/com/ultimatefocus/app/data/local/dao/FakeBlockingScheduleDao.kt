/**
 * In-memory stand-in for BlockingScheduleDao used by JVM unit tests so Room
 * doesn't need to boot. Maintains the same coupling between the schedule
 * row and its junction rows that the real DAO's @Transaction annotation
 * provides — replaceApps deletes-then-inserts atomically from the test's
 * point of view.
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.data.local.dao

import com.ultimatefocus.app.data.local.entity.BlockingScheduleAppEntity
import com.ultimatefocus.app.data.local.entity.BlockingScheduleEntity
import com.ultimatefocus.app.data.local.relation.BlockingScheduleWithApps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBlockingScheduleDao : BlockingScheduleDao {

    private val schedules = MutableStateFlow<Map<Long, BlockingScheduleEntity>>(emptyMap())
    private val junction = MutableStateFlow<List<BlockingScheduleAppEntity>>(emptyList())

    private var nextId: Long = 1L

    val scheduleSize: Int get() = schedules.value.size
    val junctionSize: Int get() = junction.value.size

    override fun observeAll(): Flow<List<BlockingScheduleWithApps>> =
        schedules.map { snapshot ->
            val byScheduleId = junction.value.groupBy { it.scheduleId }
            snapshot.values
                .sortedBy { it.createdEpochMs }
                .map { entity ->
                    BlockingScheduleWithApps(
                        schedule = entity,
                        apps = byScheduleId[entity.id].orEmpty(),
                    )
                }
        }

    override suspend fun getById(id: Long): BlockingScheduleWithApps? {
        val entity = schedules.value[id] ?: return null
        return BlockingScheduleWithApps(
            schedule = entity,
            apps = junction.value.filter { it.scheduleId == id },
        )
    }

    override suspend fun insertSchedule(entity: BlockingScheduleEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        val withId = entity.copy(id = id)
        schedules.value = schedules.value + (id to withId)
        return id
    }

    override suspend fun updateSchedule(entity: BlockingScheduleEntity) {
        schedules.value = schedules.value + (entity.id to entity)
    }

    override suspend fun insertApps(rows: List<BlockingScheduleAppEntity>) {
        val existing = junction.value.toMutableList()
        rows.forEach { row ->
            if (existing.none { it.scheduleId == row.scheduleId && it.packageName == row.packageName }) {
                existing.add(row)
            }
        }
        junction.value = existing
    }

    override suspend fun deleteAppsForSchedule(scheduleId: Long) {
        junction.value = junction.value.filterNot { it.scheduleId == scheduleId }
    }

    override suspend fun deleteById(id: Long) {
        schedules.value = schedules.value - id
        // Mirrors Room's onDelete=CASCADE on the junction table.
        junction.value = junction.value.filterNot { it.scheduleId == id }
    }

    override suspend fun deleteAssignmentsForPackage(packageName: String) {
        junction.value = junction.value.filterNot { it.packageName == packageName }
    }
}
