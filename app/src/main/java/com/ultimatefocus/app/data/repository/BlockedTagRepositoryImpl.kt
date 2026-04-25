/**
 * Room-backed implementation of BlockedTagRepository. Mirrors
 * BlockedAppRepositoryImpl: a 60-second ticker re-evaluates clock.now() so
 * expired rows drop out of observers without an unrelated DB write to
 * invalidate the Flow. All "is active" decisions go through the injected
 * Clock so tests can advance time deterministically. Re-blocking upserts
 * (extends the duration if longer; mirrors BlockedAppRepository's UX).
 * Covers: ULT-75
 */
package com.ultimatefocus.app.data.repository

import com.ultimatefocus.app.data.local.dao.BlockedTagDao
import com.ultimatefocus.app.data.local.entity.BlockedTagEntity
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.BlockedTag
import com.ultimatefocus.app.domain.repository.BlockedTagRepository
import com.ultimatefocus.app.domain.service.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class BlockedTagRepositoryImpl @Inject constructor(
    private val dao: BlockedTagDao,
    private val clock: Clock,
) : BlockedTagRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveTagBlocks(): Flow<List<BlockedTag>> =
        flow { while (true) { emit(Unit); delay(60_000L.milliseconds) } }
            .flatMapLatest {
                dao.observeActiveBlocks(clock.now()).map { rows ->
                    rows.map { it.toDomain() }
                }
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveBlockedPackagesByTags(): Flow<Map<String, Long>> =
        flow { while (true) { emit(Unit); delay(60_000L.milliseconds) } }
            .flatMapLatest {
                dao.observeActivePackages(clock.now()).map { rows ->
                    rows.associate { it.packageName to it.blockedUntilEpochMs }
                }
            }

    override suspend fun getActive(tagId: Long): BlockedTag? {
        val row = dao.getByTag(tagId) ?: return null
        return if (row.blockedUntilEpochMs > clock.now()) row.toDomain() else null
    }

    override suspend fun isTagBlocked(tagId: Long): Boolean = getActive(tagId) != null

    override suspend fun blockTag(tagId: Long, duration: BlockDuration) {
        val now = clock.now()
        dao.upsert(
            BlockedTagEntity(
                tagId = tagId,
                blockedUntilEpochMs = now + duration.millis,
                blockedSinceEpochMs = now,
                durationLabel = duration.label,
            ),
        )
    }

    override suspend fun pruneExpired() {
        dao.pruneExpired(clock.now())
    }

    private fun BlockedTagEntity.toDomain() = BlockedTag(
        tagId = tagId,
        blockedUntilEpochMs = blockedUntilEpochMs,
        blockedSinceEpochMs = blockedSinceEpochMs,
        durationLabel = durationLabel,
    )
}
