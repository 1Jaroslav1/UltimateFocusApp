/**
 * Repository contract for tag-level blocks. Like BlockedAppRepository, this
 * intentionally has NO unblock() method — preserves the ULT-19/21 "no
 * self-unblock escape hatch" requirement. The "Unblocking a tag" wording in
 * ULT-75's AC refers to NATURAL EXPIRY of the tag block, not a manual
 * undo: when blockedUntilEpochMs <= now, observers stop emitting the row.
 * Covers: ULT-75
 */
package com.ultimatefocus.app.domain.repository

import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.BlockedTag
import kotlinx.coroutines.flow.Flow

interface BlockedTagRepository {

    fun observeActiveTagBlocks(): Flow<List<BlockedTag>>

    /**
     * Map of package → latest expiry across the active tag-blocks affecting
     * that package. If an app belongs to multiple blocked tags, the value is
     * the MAX of their expiries — the natural "blocked at least until X"
     * semantics required by ULT-75.
     */
    fun observeActiveBlockedPackagesByTags(): Flow<Map<String, Long>>

    suspend fun getActive(tagId: Long): BlockedTag?

    suspend fun isTagBlocked(tagId: Long): Boolean

    suspend fun blockTag(tagId: Long, duration: BlockDuration)

    suspend fun pruneExpired()
}
