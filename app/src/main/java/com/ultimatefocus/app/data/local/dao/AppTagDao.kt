/**
 * Room DAO for the many-to-many app↔tag relationship. Read paths return
 * Flow so observers re-emit when assignments change. Counts (countForApp)
 * power the 5-tags-per-app cap check at the repository layer.
 * Covers: ULT-72, ULT-73
 */
package com.ultimatefocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ultimatefocus.app.data.local.entity.AppTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface AppTagDao {

    @Query("SELECT * FROM app_tags")
    fun observeAll(): Flow<List<AppTagCrossRef>>

    @Query("SELECT tagId FROM app_tags WHERE packageName = :packageName")
    fun observeTagIdsForApp(packageName: String): Flow<List<Long>>

    @Query("SELECT packageName FROM app_tags WHERE tagId = :tagId ORDER BY packageName ASC")
    fun observePackagesForTag(tagId: Long): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM app_tags WHERE packageName = :packageName")
    suspend fun countForApp(packageName: String): Int

    @Query("SELECT 1 FROM app_tags WHERE packageName = :packageName AND tagId = :tagId LIMIT 1")
    suspend fun isAssigned(packageName: String, tagId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: AppTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(refs: List<AppTagCrossRef>)

    @Query("DELETE FROM app_tags WHERE packageName = :packageName AND tagId = :tagId")
    suspend fun delete(packageName: String, tagId: Long)

    @Query("DELETE FROM app_tags WHERE tagId = :tagId AND packageName IN (:packageNames)")
    suspend fun deleteByPackagesForTag(packageNames: List<String>, tagId: Long)

    @Query("DELETE FROM app_tags WHERE tagId = :tagId")
    suspend fun deleteAllForTag(tagId: Long)
}
