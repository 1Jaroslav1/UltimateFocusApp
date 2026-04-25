/**
 * Repository contract for recurring blocking schedules.
 *
 * Unlike BlockedAppRepository (manual blocks, no unblock by design) this
 * repository exposes update() and delete() — schedules are mutable. To
 * preserve the "no self-unblock" spirit when a schedule is currently active,
 * update() and delete() throw ScheduleCurrentlyActiveException at call time
 * (defense in depth; the editor UI also disables the buttons).
 *
 * observeActivePackageNames() emits the union of every schedule that is
 * active at clock.now() ∩ each schedule's package set. Combined with manual
 * blocks via BlockingSourceCoordinator before reaching the AccessibilityService.
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.domain.repository

import com.ultimatefocus.app.domain.model.BlockingSchedule
import kotlinx.coroutines.flow.Flow

interface BlockingScheduleRepository {
    fun observeAll(): Flow<List<BlockingSchedule>>
    fun observeActivePackageNames(): Flow<Set<String>>

    /**
     * Per-package soonest window-end for currently scheduled-blocked apps.
     * Used by AppRepositoryImpl to populate AppInfo.blockedUntilEpochMs so
     * the context menu can show an accurate "remaining" countdown for
     * schedule-blocked apps.
     */
    fun observeActiveWindowEndByPackage(): Flow<Map<String, Long>>
    suspend fun getById(id: Long): BlockingSchedule?

    /** Insert; returns the assigned id. Validates name/days/apps/window. */
    suspend fun create(draft: BlockingScheduleDraft): Long

    /**
     * Update an existing schedule. Throws ScheduleCurrentlyActiveException
     * if the **persisted** schedule is currently active in the device zone
     * — the user cannot disable a block by mutating its row mid-window.
     */
    suspend fun update(id: Long, draft: BlockingScheduleDraft)

    /**
     * Delete a schedule and its app assignments. Throws
     * ScheduleCurrentlyActiveException if the schedule is currently active.
     */
    suspend fun delete(id: Long)

    /** Called by AppRepository when an app is uninstalled — tidies junction. */
    suspend fun purgePackage(packageName: String)

    /** Snapshot version of observeActivePackageNames for one-shot checks. */
    suspend fun activePackageNamesNow(): Set<String>

    /**
     * Returns the currently-active schedule covering [packageName] if any
     * (the one whose window-end is soonest, when several apply). Used by
     * BlockingSourceCoordinator.activeBlockInfo.
     */
    suspend fun activeScheduleFor(packageName: String): ActiveSchedule?
}

/** Editor-side draft. id-less; create() ignores any id, update() takes it as a parameter. */
data class BlockingScheduleDraft(
    val name: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val daysOfWeekMask: Int,
    val packageNames: Set<String>,
)

/** Lightweight projection for the coordinator: we only need name + window end. */
data class ActiveSchedule(
    val id: Long,
    val name: String,
    val windowEndEpochMs: Long,
)

class ScheduleCurrentlyActiveException(val scheduleId: Long) :
    IllegalStateException("Schedule $scheduleId is currently active and cannot be modified or deleted.")

class InvalidBlockingScheduleException(message: String) : IllegalArgumentException(message)
