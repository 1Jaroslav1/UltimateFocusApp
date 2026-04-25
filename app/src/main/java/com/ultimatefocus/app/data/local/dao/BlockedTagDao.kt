/**
 * Room DAO for tag-level blocks. Mirrors BlockedAppDao in spirit: observers
 * filter on caller-supplied nowMs so expired rows are invisible even when
 * pruning hasn't happened, expiry uses strict > semantics, and re-blocking
 * upserts (extending the duration if the user picks a longer one).
 * The activePackagesForBlockedTagsAt query joins blocked_tags ⋈ app_tags so
 * the repository can hand the AppRepository a flat package→expiry map for
 * the "is this app blocked through any of its tags?" check.
 * Covers: ULT-75
 */
package com.ultimatefocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ultimatefocus.app.data.local.entity.BlockedTagEntity
import kotlinx.coroutines.flow.Flow

/** Projection joining a package name with the latest expiry across its blocked tags. */
data class PackageBlockExpiry(
    val packageName: String,
    val blockedUntilEpochMs: Long,
)

@Dao
interface BlockedTagDao {

    @Query("SELECT * FROM blocked_tags WHERE blockedUntilEpochMs > :nowMs")
    fun observeActiveBlocks(nowMs: Long): Flow<List<BlockedTagEntity>>

    @Query(
        """
        SELECT a.packageName AS packageName,
               MAX(b.blockedUntilEpochMs) AS blockedUntilEpochMs
        FROM blocked_tags b
        INNER JOIN app_tags a ON a.tagId = b.tagId
        WHERE b.blockedUntilEpochMs > :nowMs
        GROUP BY a.packageName
        """,
    )
    fun observeActivePackages(nowMs: Long): Flow<List<PackageBlockExpiry>>

    @Query("SELECT * FROM blocked_tags WHERE tagId = :tagId LIMIT 1")
    suspend fun getByTag(tagId: Long): BlockedTagEntity?

    @Upsert
    suspend fun upsert(entity: BlockedTagEntity)

    @Query("DELETE FROM blocked_tags WHERE blockedUntilEpochMs <= :nowMs")
    suspend fun pruneExpired(nowMs: Long)
}
