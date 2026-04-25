/**
 * Room-backed implementation of BlockingScheduleRepository.
 *
 * observeActivePackageNames() mirrors BlockedAppRepositoryImpl's pattern: a
 * 60-second ticker keyed via flatMapLatest re-evaluates ScheduleActivePredicate
 * against clock.now() so windows that begin/end without any DB writes still
 * propagate to subscribers. The AccessibilityService keeps an in-memory
 * Set<String> updated from this flow on the main hot path.
 *
 * create()/update() validate input via require checks and bundle the schedule
 * insert + junction rows together with the DAO's @Transaction-bound
 * replaceApps so observers never see a half-applied state.
 *
 * update()/delete() re-check ScheduleActivePredicate against the persisted
 * row immediately before mutating and throw ScheduleCurrentlyActiveException
 * — UI also disables those buttons but the repo enforces it as the last
 * line of defense (parallels the no-unblock invariant of manual blocks).
 *
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.data.repository

import com.ultimatefocus.app.data.local.dao.BlockingScheduleDao
import com.ultimatefocus.app.data.local.entity.BlockingScheduleEntity
import com.ultimatefocus.app.data.local.relation.BlockingScheduleWithApps
import com.ultimatefocus.app.domain.model.BlockingSchedule
import com.ultimatefocus.app.domain.model.DayOfWeekMask
import com.ultimatefocus.app.domain.repository.ActiveSchedule
import com.ultimatefocus.app.domain.repository.BlockingScheduleDraft
import com.ultimatefocus.app.domain.repository.BlockingScheduleRepository
import com.ultimatefocus.app.domain.repository.InvalidBlockingScheduleException
import com.ultimatefocus.app.domain.repository.ScheduleCurrentlyActiveException
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.domain.service.ScheduleActivePredicate
import com.ultimatefocus.app.domain.service.ZoneIdProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class BlockingScheduleRepositoryImpl @Inject constructor(
    private val dao: BlockingScheduleDao,
    private val clock: Clock,
    private val zoneIdProvider: ZoneIdProvider,
) : BlockingScheduleRepository {

    override fun observeAll(): Flow<List<BlockingSchedule>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActivePackageNames(): Flow<Set<String>> =
        flow { while (true) { emit(Unit); delay(TICK_MILLIS.milliseconds) } }
            .flatMapLatest {
                dao.observeAll().map { rows ->
                    val now = clock.now()
                    val zone = zoneIdProvider.current()
                    rows
                        .map { it.toDomain() }
                        .filter { ScheduleActivePredicate.isActive(it, now, zone) }
                        .flatMap { it.packageNames }
                        .toSet()
                }
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveWindowEndByPackage(): Flow<Map<String, Long>> =
        flow { while (true) { emit(Unit); delay(TICK_MILLIS.milliseconds) } }
            .flatMapLatest {
                dao.observeAll().map { rows ->
                    val now = clock.now()
                    val zone = zoneIdProvider.current()
                    val result = mutableMapOf<String, Long>()
                    rows.map { it.toDomain() }.forEach { schedule ->
                        val windowEnd =
                            ScheduleActivePredicate.activeWindowEndEpochMs(schedule, now, zone)
                                ?: return@forEach
                        for (pkg in schedule.packageNames) {
                            val current = result[pkg]
                            if (current == null || windowEnd < current) {
                                result[pkg] = windowEnd
                            }
                        }
                    }
                    result.toMap()
                }
            }

    override suspend fun getById(id: Long): BlockingSchedule? =
        dao.getById(id)?.toDomain()

    override suspend fun create(draft: BlockingScheduleDraft): Long {
        validate(draft)
        val id = dao.insertSchedule(
            BlockingScheduleEntity(
                name = draft.name.trim(),
                startMinuteOfDay = draft.startMinuteOfDay,
                endMinuteOfDay = draft.endMinuteOfDay,
                daysOfWeekMask = draft.daysOfWeekMask,
                createdEpochMs = clock.now(),
            ),
        )
        dao.replaceApps(id, draft.packageNames)
        return id
    }

    override suspend fun update(id: Long, draft: BlockingScheduleDraft) {
        validate(draft)
        val existing = dao.getById(id)?.toDomain()
            ?: throw InvalidBlockingScheduleException("Schedule $id not found")
        if (ScheduleActivePredicate.isActive(existing, clock.now(), zoneIdProvider.current())) {
            throw ScheduleCurrentlyActiveException(id)
        }
        dao.updateSchedule(
            BlockingScheduleEntity(
                id = id,
                name = draft.name.trim(),
                startMinuteOfDay = draft.startMinuteOfDay,
                endMinuteOfDay = draft.endMinuteOfDay,
                daysOfWeekMask = draft.daysOfWeekMask,
                createdEpochMs = existing.createdEpochMs,
            ),
        )
        dao.replaceApps(id, draft.packageNames)
    }

    override suspend fun delete(id: Long) {
        val existing = dao.getById(id)?.toDomain() ?: return
        if (ScheduleActivePredicate.isActive(existing, clock.now(), zoneIdProvider.current())) {
            throw ScheduleCurrentlyActiveException(id)
        }
        // FK CASCADE removes junction rows automatically.
        dao.deleteById(id)
    }

    override suspend fun purgePackage(packageName: String) {
        dao.deleteAssignmentsForPackage(packageName)
    }

    override suspend fun activePackageNamesNow(): Set<String> {
        val now = clock.now()
        val zone = zoneIdProvider.current()
        return dao.observeAll().first()
            .map { it.toDomain() }
            .filter { ScheduleActivePredicate.isActive(it, now, zone) }
            .flatMap { it.packageNames }
            .toSet()
    }

    override suspend fun activeScheduleFor(packageName: String): ActiveSchedule? {
        val now = clock.now()
        val zone = zoneIdProvider.current()
        return dao.observeAll().first()
            .map { it.toDomain() }
            .filter { packageName in it.packageNames }
            .filter { ScheduleActivePredicate.isActive(it, now, zone) }
            .mapNotNull { schedule ->
                ScheduleActivePredicate.activeWindowEndEpochMs(schedule, now, zone)?.let { end ->
                    ActiveSchedule(schedule.id, schedule.name, end)
                }
            }
            .minByOrNull { it.windowEndEpochMs }
    }

    private fun validate(draft: BlockingScheduleDraft) {
        if (draft.name.isBlank()) {
            throw InvalidBlockingScheduleException("Schedule name must not be blank")
        }
        if (draft.startMinuteOfDay !in 0..1439 || draft.endMinuteOfDay !in 0..1439) {
            throw InvalidBlockingScheduleException("Time must be within 00:00..23:59")
        }
        if (draft.startMinuteOfDay == draft.endMinuteOfDay) {
            throw InvalidBlockingScheduleException("Start and end time must differ")
        }
        if (draft.daysOfWeekMask and 0b1111111 == 0) {
            throw InvalidBlockingScheduleException("At least one day must be selected")
        }
        if (draft.packageNames.isEmpty()) {
            throw InvalidBlockingScheduleException("At least one app must be assigned")
        }
    }

    private fun BlockingScheduleWithApps.toDomain(): BlockingSchedule = BlockingSchedule(
        id = schedule.id,
        name = schedule.name,
        start = LocalTime.of(schedule.startMinuteOfDay / 60, schedule.startMinuteOfDay % 60),
        end = LocalTime.of(schedule.endMinuteOfDay / 60, schedule.endMinuteOfDay % 60),
        days = DayOfWeekMask.decode(schedule.daysOfWeekMask),
        packageNames = apps.map { it.packageName }.toSet(),
        createdEpochMs = schedule.createdEpochMs,
    )

    private companion object {
        const val TICK_MILLIS: Long = 60_000L
    }
}
