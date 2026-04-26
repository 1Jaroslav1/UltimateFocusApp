/**
 * Room DAO for the per-app In-App Time Reminder opt-ins. The set is small
 * (bounded by installed-app count) and read on every foreground transition
 * by the AccessibilityService, so we expose both a Flow<Set<String>>
 * projection (cheap to combine) and a one-shot suspend lookup.
 *
 * Covers: ULT-77
 */
package com.ultimatefocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ultimatefocus.app.data.local.entity.SessionTimerAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionTimerAppDao {

    @Query("SELECT * FROM session_timer_apps ORDER BY packageName ASC")
    fun observeAll(): Flow<List<SessionTimerAppEntity>>

    @Query("SELECT packageName FROM session_timer_apps")
    fun observePackages(): Flow<List<String>>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM session_timer_apps WHERE packageName = :packageName)",
    )
    suspend fun isEnabled(packageName: String): Boolean

    @Upsert
    suspend fun upsert(entity: SessionTimerAppEntity)

    @Query("DELETE FROM session_timer_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
