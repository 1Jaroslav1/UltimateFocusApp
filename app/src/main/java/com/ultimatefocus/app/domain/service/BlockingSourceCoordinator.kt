/**
 * Single seam that unifies the THREE block sources — manual
 * (BlockedAppRepository, ULT-20), scheduled (BlockingScheduleRepository,
 * ULT-24/25), and tag-derived (BlockedTagRepository ⋈ TagRepository,
 * ULT-75) — so the downstream enforcement consumers
 * (BlockedAppAccessibilityService, BlockedAppChecker, the launcher's
 * blocked-launch routing in AppRepositoryImpl.launchApp) depend on one
 * Set<String> of "currently blocked packages" instead of managing the
 * union themselves.
 *
 * activeBlockInfo() resolves the priority: Manual > Scheduled > Tag.
 * Manual is the most binding (no self-unblock, ULT-20/21); scheduled has
 * a named structured window; tag is a category-level fallback. Whichever
 * wins is what the BlockedAppActivity reminder screen surfaces.
 *
 * Covers: ULT-24, ULT-75
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.ActiveBlockInfo
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import com.ultimatefocus.app.domain.repository.BlockedTagRepository
import com.ultimatefocus.app.domain.repository.BlockingScheduleRepository
import com.ultimatefocus.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockingSourceCoordinator @Inject constructor(
    private val manualRepository: BlockedAppRepository,
    private val scheduleRepository: BlockingScheduleRepository,
    private val blockedTagRepository: BlockedTagRepository,
    private val tagRepository: TagRepository,
) {

    fun observeActivePackageNames(): Flow<Set<String>> =
        combine(
            manualRepository.observeActivePackageNames(),
            scheduleRepository.observeActivePackageNames(),
            blockedTagRepository.observeActiveBlockedPackagesByTags(),
        ) { manual, scheduled, tagBlocked -> manual + scheduled + tagBlocked.keys }

    suspend fun isBlocked(packageName: String): Boolean {
        if (manualRepository.isBlocked(packageName)) return true
        if (packageName in scheduleRepository.activePackageNamesNow()) return true
        return isPackageTagBlocked(packageName)
    }

    /**
     * Returns the active-block reason for [packageName], picking the
     * highest-priority source (Manual > Scheduled > Tag). Null if not
     * currently blocked.
     */
    suspend fun activeBlockInfo(packageName: String): ActiveBlockInfo? {
        manualRepository.getBlock(packageName)?.let { manual ->
            return ActiveBlockInfo.Manual(
                packageName = manual.packageName,
                blockedUntilEpochMs = manual.blockedUntilEpochMs,
                durationLabel = manual.durationLabel,
            )
        }
        scheduleRepository.activeScheduleFor(packageName)?.let { scheduled ->
            return ActiveBlockInfo.Scheduled(
                packageName = packageName,
                blockedUntilEpochMs = scheduled.windowEndEpochMs,
                scheduleName = scheduled.name,
            )
        }
        // Tag fallback. Walk this package's tags, pick the latest active
        // tag-block expiry. Returns null if no tag-block covers the app.
        val tagBlock = activeTagBlockFor(packageName) ?: return null
        return ActiveBlockInfo.Tag(
            packageName = packageName,
            blockedUntilEpochMs = tagBlock.blockedUntilEpochMs,
            durationLabel = tagBlock.durationLabel,
        )
    }

    private suspend fun isPackageTagBlocked(packageName: String): Boolean =
        activeTagBlockFor(packageName) != null

    private suspend fun activeTagBlockFor(packageName: String):
        com.ultimatefocus.app.domain.model.BlockedTag? {
        val tags = tagRepository.observeTagsForApp(packageName).first()
        return tags
            .mapNotNull { blockedTagRepository.getActive(it.id) }
            .maxByOrNull { it.blockedUntilEpochMs }
    }
}
