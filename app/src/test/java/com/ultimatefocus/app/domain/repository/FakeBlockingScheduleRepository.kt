/**
 * In-memory test double for BlockingScheduleRepository. Mirrors the
 * production semantics of the predicate-driven flows but lets tests
 * (1) seed schedules directly and (2) flip the active set without standing
 * up Room or the 60s ticker. Validation in create()/update() matches the
 * production rules so it can sub in for negative-path tests too.
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.domain.repository

import com.ultimatefocus.app.domain.model.BlockingSchedule
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.domain.service.FakeClock
import com.ultimatefocus.app.domain.service.FixedZoneIdProvider
import com.ultimatefocus.app.domain.service.ScheduleActivePredicate
import com.ultimatefocus.app.domain.service.ZoneIdProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.ZoneId

class FakeBlockingScheduleRepository(
    private val clock: Clock = FakeClock(current = 0L),
    private val zoneIdProvider: ZoneIdProvider = FixedZoneIdProvider(ZoneId.of("UTC")),
) : BlockingScheduleRepository {

    private val _schedules = MutableStateFlow<List<BlockingSchedule>>(emptyList())
    private var nextId: Long = 1L

    var failPurgeWith: Throwable? = null

    fun seed(schedules: List<BlockingSchedule>) {
        _schedules.value = schedules
        nextId = (schedules.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    val all: List<BlockingSchedule>
        get() = _schedules.value

    override fun observeAll(): Flow<List<BlockingSchedule>> = _schedules

    override fun observeActivePackageNames(): Flow<Set<String>> = _schedules.map { list ->
        val now = clock.now()
        val zone = zoneIdProvider.current()
        list.filter { ScheduleActivePredicate.isActive(it, now, zone) }
            .flatMap { it.packageNames }
            .toSet()
    }

    override fun observeActiveWindowEndByPackage(): Flow<Map<String, Long>> =
        _schedules.map { list ->
            val now = clock.now()
            val zone = zoneIdProvider.current()
            val out = mutableMapOf<String, Long>()
            list.forEach { schedule ->
                val end = ScheduleActivePredicate.activeWindowEndEpochMs(schedule, now, zone)
                    ?: return@forEach
                for (pkg in schedule.packageNames) {
                    val current = out[pkg]
                    if (current == null || end < current) out[pkg] = end
                }
            }
            out.toMap()
        }

    override suspend fun getById(id: Long): BlockingSchedule? =
        _schedules.value.firstOrNull { it.id == id }

    override suspend fun create(draft: BlockingScheduleDraft): Long {
        validate(draft)
        val id = nextId++
        val schedule = draft.toDomain(id, clock.now())
        _schedules.value = _schedules.value + schedule
        return id
    }

    override suspend fun update(id: Long, draft: BlockingScheduleDraft) {
        validate(draft)
        val existing = getById(id) ?: throw InvalidBlockingScheduleException("Schedule $id not found")
        if (ScheduleActivePredicate.isActive(existing, clock.now(), zoneIdProvider.current())) {
            throw ScheduleCurrentlyActiveException(id)
        }
        val replacement = draft.toDomain(id, existing.createdEpochMs)
        _schedules.value = _schedules.value.map { if (it.id == id) replacement else it }
    }

    override suspend fun delete(id: Long) {
        val existing = getById(id) ?: return
        if (ScheduleActivePredicate.isActive(existing, clock.now(), zoneIdProvider.current())) {
            throw ScheduleCurrentlyActiveException(id)
        }
        _schedules.value = _schedules.value.filterNot { it.id == id }
    }

    override suspend fun purgePackage(packageName: String) {
        failPurgeWith?.let { throw it }
        _schedules.value = _schedules.value.map { schedule ->
            schedule.copy(packageNames = schedule.packageNames - packageName)
        }
    }

    override suspend fun activePackageNamesNow(): Set<String> {
        val now = clock.now()
        val zone = zoneIdProvider.current()
        return _schedules.value
            .filter { ScheduleActivePredicate.isActive(it, now, zone) }
            .flatMap { it.packageNames }
            .toSet()
    }

    override suspend fun activeScheduleFor(packageName: String): ActiveSchedule? {
        val now = clock.now()
        val zone = zoneIdProvider.current()
        return _schedules.value
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

    private fun BlockingScheduleDraft.toDomain(id: Long, createdEpochMs: Long): BlockingSchedule =
        BlockingSchedule(
            id = id,
            name = name.trim(),
            start = LocalTime.of(startMinuteOfDay / 60, startMinuteOfDay % 60),
            end = LocalTime.of(endMinuteOfDay / 60, endMinuteOfDay % 60),
            days = com.ultimatefocus.app.domain.model.DayOfWeekMask.decode(daysOfWeekMask),
            packageNames = packageNames,
            createdEpochMs = createdEpochMs,
        )
}
