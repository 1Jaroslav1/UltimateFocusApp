/**
 * Repository contract for manual app blocks.
 * Intentionally has NO `unblock()` method — satisfies the "no self-unblock
 * escape hatch" requirement of ULT-19/ULT-21 at the architectural layer so
 * the UI physically cannot offer an override.
 *
 * blockForMillis (ULT-81) is used by the EXIT_AND_BLOCK session-expiry
 * mode to block an app for the same duration the user just spent in it;
 * since session durations are arbitrary minutes (1..240) and don't map
 * onto the predefined BlockDuration enum, the value is provided as raw
 * millis with a custom label.
 *
 * Covers: ULT-20, ULT-81
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
    suspend fun blockForMillis(packageName: String, durationMillis: Long, label: String)
    suspend fun pruneExpired()
}
