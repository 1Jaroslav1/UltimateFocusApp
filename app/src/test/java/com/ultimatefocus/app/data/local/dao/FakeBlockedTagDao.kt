/**
 * In-memory stand-in for BlockedTagDao used by JVM unit tests so Room
 * doesn't need to boot. Maintains the same strict > expiry semantics as
 * the real DAO and supports observeActivePackages join via an injected
 * appsByTag function (default empty).
 * Covers: ULT-75
 */
package com.ultimatefocus.app.data.local.dao

import com.ultimatefocus.app.data.local.entity.BlockedTagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBlockedTagDao(
    private val appsByTag: () -> Map<Long, List<String>> = { emptyMap() },
) : BlockedTagDao {

    private val rows = MutableStateFlow<Map<Long, BlockedTagEntity>>(emptyMap())

    val size: Int get() = rows.value.size

    override fun observeActiveBlocks(nowMs: Long): Flow<List<BlockedTagEntity>> =
        rows.map { m -> m.values.filter { it.blockedUntilEpochMs > nowMs } }

    override fun observeActivePackages(nowMs: Long): Flow<List<PackageBlockExpiry>> =
        rows.map { m ->
            val byPackage = mutableMapOf<String, Long>()
            for ((tagId, b) in m) {
                if (b.blockedUntilEpochMs <= nowMs) continue
                val packages = appsByTag()[tagId].orEmpty()
                for (pkg in packages) {
                    val current = byPackage[pkg]
                    if (current == null || current < b.blockedUntilEpochMs) {
                        byPackage[pkg] = b.blockedUntilEpochMs
                    }
                }
            }
            byPackage.map { (pkg, until) -> PackageBlockExpiry(pkg, until) }
        }

    override suspend fun getByTag(tagId: Long): BlockedTagEntity? = rows.value[tagId]

    override suspend fun upsert(entity: BlockedTagEntity) {
        rows.value = rows.value + (entity.tagId to entity)
    }

    override suspend fun pruneExpired(nowMs: Long) {
        rows.value = rows.value.filterValues { it.blockedUntilEpochMs > nowMs }
    }
}
