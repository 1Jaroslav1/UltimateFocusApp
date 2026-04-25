/**
 * Room DAO for blocking schedules + their assigned apps. Reads return the
 * relation projection (BlockingScheduleWithApps) under @Transaction so the
 * parent row and its junction rows are observed atomically. Writes go
 * through replaceApps() which clears the junction for a schedule and
 * re-inserts in a single transaction so the editor never observes a half-
 * applied state.
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.ultimatefocus.app.data.local.entity.BlockingScheduleAppEntity
import com.ultimatefocus.app.data.local.entity.BlockingScheduleEntity
import com.ultimatefocus.app.data.local.relation.BlockingScheduleWithApps
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockingScheduleDao {

    @Transaction
    @Query("SELECT * FROM blocking_schedules ORDER BY createdEpochMs ASC")
    fun observeAll(): Flow<List<BlockingScheduleWithApps>>

    @Transaction
    @Query("SELECT * FROM blocking_schedules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BlockingScheduleWithApps?

    @Insert
    suspend fun insertSchedule(entity: BlockingScheduleEntity): Long

    @Update
    suspend fun updateSchedule(entity: BlockingScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApps(rows: List<BlockingScheduleAppEntity>)

    @Query("DELETE FROM blocking_schedule_apps WHERE scheduleId = :scheduleId")
    suspend fun deleteAppsForSchedule(scheduleId: Long)

    @Transaction
    suspend fun replaceApps(scheduleId: Long, packageNames: Collection<String>) {
        deleteAppsForSchedule(scheduleId)
        if (packageNames.isNotEmpty()) {
            insertApps(packageNames.map { BlockingScheduleAppEntity(scheduleId, it) })
        }
    }

    @Query("DELETE FROM blocking_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blocking_schedule_apps WHERE packageName = :packageName")
    suspend fun deleteAssignmentsForPackage(packageName: String)
}
