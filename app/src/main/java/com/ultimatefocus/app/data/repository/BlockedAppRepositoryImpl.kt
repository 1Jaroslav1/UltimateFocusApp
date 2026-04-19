/**
 * Room-backed implementation of BlockedAppRepository.
 * All "is active" decisions go through the injected Clock so tests can
 * advance time deterministically. Re-blocking an already-blocked app
 * upserts the row (extending the block) — matches the user's mental
 * model better than refusing the second attempt.
 * Covers: ULT-20
 */
package com.ultimatefocus.app.data.repository

import com.ultimatefocus.app.data.local.dao.BlockedAppDao
import com.ultimatefocus.app.data.local.entity.BlockedAppEntity
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.BlockedApp
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import com.ultimatefocus.app.domain.service.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedAppRepositoryImpl @Inject constructor(
    private val dao: BlockedAppDao,
    private val clock: Clock,
) : BlockedAppRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveBlocks(): Flow<List<BlockedApp>> =
        flowOf(Unit).flatMapLatest {
            dao.observeActiveBlocks(clock.now()).map { rows ->
                rows.map { it.toDomain() }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActivePackageNames(): Flow<Set<String>> =
        flowOf(Unit).flatMapLatest {
            dao.observeActivePackageNames(clock.now()).map { it.toSet() }
        }

    override suspend fun isBlocked(packageName: String): Boolean =
        getBlock(packageName) != null

    override suspend fun getBlock(packageName: String): BlockedApp? {
        val row = dao.getByPackage(packageName) ?: return null
        return if (row.blockedUntilEpochMs > clock.now()) row.toDomain() else null
    }

    override suspend fun block(packageName: String, duration: BlockDuration) {
        val now = clock.now()
        dao.upsert(
            BlockedAppEntity(
                packageName = packageName,
                blockedUntilEpochMs = now + duration.millis,
                blockedSinceEpochMs = now,
                durationLabel = duration.label,
            ),
        )
    }

    override suspend fun pruneExpired() {
        dao.pruneExpired(clock.now())
    }

    private fun BlockedAppEntity.toDomain() = BlockedApp(
        packageName = packageName,
        blockedUntilEpochMs = blockedUntilEpochMs,
        blockedSinceEpochMs = blockedSinceEpochMs,
        durationLabel = durationLabel,
    )
}
