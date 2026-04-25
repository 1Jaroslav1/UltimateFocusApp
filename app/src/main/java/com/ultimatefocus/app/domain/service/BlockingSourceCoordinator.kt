/**
 * Single seam that unifies the two block sources — manual (BlockedAppRepository,
 * ULT-20) and scheduled (BlockingScheduleRepository, ULT-24/25) — so the
 * three downstream enforcement consumers (BlockedAppAccessibilityService,
 * BlockedAppChecker / NotificationListener, AppRepositoryImpl.launchApp)
 * depend on one Set<String> of "currently blocked packages" instead of
 * managing the union themselves.
 *
 * activeBlockInfo() resolves the priority: when both a manual block and a
 * scheduled block apply to the same package the manual one wins (it is the
 * stricter contract — manual blocks have no override at all, ULT-20/21).
 *
 * Covers: ULT-24
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.model.ActiveBlockInfo
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import com.ultimatefocus.app.domain.repository.BlockingScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockingSourceCoordinator @Inject constructor(
    private val manualRepository: BlockedAppRepository,
    private val scheduleRepository: BlockingScheduleRepository,
) {

    fun observeActivePackageNames(): Flow<Set<String>> =
        combine(
            manualRepository.observeActivePackageNames(),
            scheduleRepository.observeActivePackageNames(),
        ) { manual, scheduled -> manual + scheduled }

    suspend fun isBlocked(packageName: String): Boolean {
        if (manualRepository.isBlocked(packageName)) return true
        return packageName in scheduleRepository.activePackageNamesNow()
    }

    /**
     * Returns the active-block reason for [packageName], preferring the
     * manual block when both apply. Null if not currently blocked.
     */
    suspend fun activeBlockInfo(packageName: String): ActiveBlockInfo? {
        manualRepository.getBlock(packageName)?.let { manual ->
            return ActiveBlockInfo.Manual(
                packageName = manual.packageName,
                blockedUntilEpochMs = manual.blockedUntilEpochMs,
                durationLabel = manual.durationLabel,
            )
        }
        val scheduled = scheduleRepository.activeScheduleFor(packageName) ?: return null
        return ActiveBlockInfo.Scheduled(
            packageName = packageName,
            blockedUntilEpochMs = scheduled.windowEndEpochMs,
            scheduleName = scheduled.name,
        )
    }
}
