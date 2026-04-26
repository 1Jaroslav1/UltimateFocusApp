/**
 * In-memory test double for BlockedAppRepository. Tests can seed the
 * internal map directly via blockedPackages, or exercise the full
 * block(packageName, duration) flow. Uses an injected FakeClock so
 * expiry checks are deterministic.
 * Covers: ULT-20, ULT-21, ULT-22, ULT-81
 */
package com.ultimatefocus.app.domain.repository

import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.BlockedApp
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.domain.service.FakeClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBlockedAppRepository(
    private val clock: Clock = FakeClock(current = 0L),
) : BlockedAppRepository {

    private val _blocks = MutableStateFlow<Map<String, BlockedApp>>(emptyMap())

    val blockedPackages: Map<String, BlockedApp>
        get() = _blocks.value

    fun seed(blocks: List<BlockedApp>) {
        _blocks.value = blocks.associateBy { it.packageName }
    }

    override fun observeActiveBlocks(): Flow<List<BlockedApp>> =
        _blocks.map { m -> m.values.filter { it.blockedUntilEpochMs > clock.now() } }

    override fun observeActivePackageNames(): Flow<Set<String>> =
        _blocks.map { m ->
            m.values
                .filter { it.blockedUntilEpochMs > clock.now() }
                .map { it.packageName }
                .toSet()
        }

    override suspend fun isBlocked(packageName: String): Boolean =
        getBlock(packageName) != null

    override suspend fun getBlock(packageName: String): BlockedApp? {
        val block = _blocks.value[packageName] ?: return null
        return if (block.blockedUntilEpochMs > clock.now()) block else null
    }

    override suspend fun block(packageName: String, duration: BlockDuration) {
        blockForMillis(packageName, duration.millis, duration.label)
    }

    override suspend fun blockForMillis(
        packageName: String,
        durationMillis: Long,
        label: String,
    ) {
        val now = clock.now()
        val next = BlockedApp(
            packageName = packageName,
            blockedUntilEpochMs = now + durationMillis,
            blockedSinceEpochMs = now,
            durationLabel = label,
        )
        _blocks.value = _blocks.value + (packageName to next)
    }

    override suspend fun pruneExpired() {
        val now = clock.now()
        _blocks.value = _blocks.value.filterValues { it.blockedUntilEpochMs > now }
    }
}
