/**
 * Room DAO for reading and writing app blocks. Observers filter on the
 * caller-supplied nowMs so expired rows are invisible even when pruning
 * hasn't yet happened — correctness does not depend on prune scheduling.
 * Expiry uses strict > semantics (blockedUntilEpochMs > nowMs).
 * Covers: ULT-20
 */
package com.ultimatefocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ultimatefocus.app.data.local.entity.BlockedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {

    @Query("SELECT * FROM blocked_apps WHERE blockedUntilEpochMs > :nowMs")
    fun observeActiveBlocks(nowMs: Long): Flow<List<BlockedAppEntity>>

    @Query("SELECT packageName FROM blocked_apps WHERE blockedUntilEpochMs > :nowMs")
    fun observeActivePackageNames(nowMs: Long): Flow<List<String>>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): BlockedAppEntity?

    @Upsert
    suspend fun upsert(entity: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM blocked_apps WHERE blockedUntilEpochMs <= :nowMs")
    suspend fun pruneExpired(nowMs: Long)
}
