/**
 * In-memory test double for BlockedTagRepository. Mirrors FakeBlockedAppRepository:
 * uses an injected FakeClock for deterministic expiry, exposes the seeded map
 * directly so tests can assert side-effects without going through the
 * suspend API.
 * Covers: ULT-75
 */
package com.ultimatefocus.app.domain.repository

import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.BlockedTag
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.domain.service.FakeClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBlockedTagRepository(
    private val clock: Clock = FakeClock(current = 0L),
) : BlockedTagRepository {

    private val _blocks = MutableStateFlow<Map<Long, BlockedTag>>(emptyMap())

    /** package → set of tagIds to drive observeActiveBlockedPackagesByTags. */
    private val _appsByTag = MutableStateFlow<Map<Long, List<String>>>(emptyMap())

    val blockedTags: Map<Long, BlockedTag> get() = _blocks.value

    fun setAppsForTag(tagId: Long, packages: List<String>) {
        _appsByTag.value = _appsByTag.value + (tagId to packages)
    }

    override fun observeActiveTagBlocks(): Flow<List<BlockedTag>> =
        _blocks.map { m -> m.values.filter { it.blockedUntilEpochMs > clock.now() } }

    override fun observeActiveBlockedPackagesByTags(): Flow<Map<String, Long>> =
        _blocks.map { m ->
            val now = clock.now()
            val packageToMaxExpiry = mutableMapOf<String, Long>()
            for ((tagId, block) in m) {
                if (block.blockedUntilEpochMs <= now) continue
                val packages = _appsByTag.value[tagId].orEmpty()
                for (pkg in packages) {
                    val current = packageToMaxExpiry[pkg]
                    if (current == null || current < block.blockedUntilEpochMs) {
                        packageToMaxExpiry[pkg] = block.blockedUntilEpochMs
                    }
                }
            }
            packageToMaxExpiry
        }

    override suspend fun getActive(tagId: Long): BlockedTag? {
        val b = _blocks.value[tagId] ?: return null
        return if (b.blockedUntilEpochMs > clock.now()) b else null
    }

    override suspend fun isTagBlocked(tagId: Long): Boolean = getActive(tagId) != null

    override suspend fun blockTag(tagId: Long, duration: BlockDuration) {
        val now = clock.now()
        _blocks.value = _blocks.value + (tagId to BlockedTag(
            tagId = tagId,
            blockedUntilEpochMs = now + duration.millis,
            blockedSinceEpochMs = now,
            durationLabel = duration.label,
        ))
    }

    override suspend fun pruneExpired() {
        val now = clock.now()
        _blocks.value = _blocks.value.filterValues { it.blockedUntilEpochMs > now }
    }
}
