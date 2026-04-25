/**
 * Repository contract for manual app blocks.
 * Intentionally has NO `unblock()` method — satisfies the "no self-unblock
 * escape hatch" requirement of ULT-19/ULT-21 at the architectural layer so
 * the UI physically cannot offer an override.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.domain.repository

import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.BlockedApp
import kotlinx.coroutines.flow.Flow

interface BlockedAppRepository {
    fun observeActiveBlocks(): Flow<List<BlockedApp>>
    fun observeActivePackageNames(): Flow<Set<String>>
    suspend fun isBlocked(packageName: String): Boolean
    suspend fun getBlock(packageName: String): BlockedApp?
    suspend fun block(packageName: String, duration: BlockDuration)
    suspend fun pruneExpired()
}
