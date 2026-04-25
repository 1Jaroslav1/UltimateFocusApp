/**
 * In-memory stand-in for BlockedAppDao used by JVM unit tests so Room (which
 * needs an Android runtime) doesn't need to be booted to exercise the
 * repository logic. Maintains strict > semantics for expiry — same as the
 * real Room DAO.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.data.local.dao

import com.ultimatefocus.app.data.local.entity.BlockedAppEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBlockedAppDao : BlockedAppDao {

    private val rows = MutableStateFlow<Map<String, BlockedAppEntity>>(emptyMap())

    val size: Int get() = rows.value.size

    override fun observeActiveBlocks(nowMs: Long): Flow<List<BlockedAppEntity>> =
        rows.map { m -> m.values.filter { it.blockedUntilEpochMs > nowMs } }

    override fun observeActivePackageNames(nowMs: Long): Flow<List<String>> =
        rows.map { m ->
            m.values
                .filter { it.blockedUntilEpochMs > nowMs }
                .map { it.packageName }
        }

    override suspend fun getByPackage(packageName: String): BlockedAppEntity? =
        rows.value[packageName]

    override suspend fun upsert(entity: BlockedAppEntity) {
        rows.value = rows.value + (entity.packageName to entity)
    }

    override suspend fun deleteByPackage(packageName: String) {
        rows.value = rows.value - packageName
    }

    override suspend fun pruneExpired(nowMs: Long) {
        rows.value = rows.value.filterValues { it.blockedUntilEpochMs > nowMs }
    }
}
