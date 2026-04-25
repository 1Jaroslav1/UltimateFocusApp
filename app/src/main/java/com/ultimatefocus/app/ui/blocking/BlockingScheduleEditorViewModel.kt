/**
 * ViewModel for BlockingScheduleEditorScreen. Owns the in-progress draft
 * (name / start / end / days / packages) and exposes:
 *  - validation flags so the Save button enables only with valid input;
 *  - isCurrentlyActive — recomputed every second against the persisted
 *    schedule (edit mode only) so the editor disables Save and Delete
 *    while the schedule is currently active (ULT-25 self-unblock guard);
 *  - effectEvents — one-shot Saved / Deleted / Error signals consumed by
 *    the screen to navigate or surface a snackbar.
 *
 * Reads the installed-apps list from AppRepository and joins it with weekly
 * foreground-usage from AppUsageStatsRepository so the picker can sort by
 * "most distracting" (descending usage) with alphabetical tiebreak. When
 * PACKAGE_USAGE_STATS hasn't been granted the usage map is empty and the
 * sort degrades to alphabetical — the picker still works, just without
 * the smarts.
 *
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.ui.blocking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.data.repository.AppRepository
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.BlockingSchedule
import com.ultimatefocus.app.domain.model.DayOfWeekMask
import com.ultimatefocus.app.domain.repository.AppUsageStatsRepository
import com.ultimatefocus.app.domain.repository.BlockingScheduleDraft
import com.ultimatefocus.app.domain.repository.BlockingScheduleRepository
import com.ultimatefocus.app.domain.repository.InvalidBlockingScheduleException
import com.ultimatefocus.app.domain.repository.ScheduleCurrentlyActiveException
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.domain.service.ScheduleActivePredicate
import com.ultimatefocus.app.domain.service.ZoneIdProvider
import com.ultimatefocus.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class BlockingScheduleEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BlockingScheduleRepository,
    private val appRepository: AppRepository,
    private val usageStatsRepository: AppUsageStatsRepository,
    private val clock: Clock,
    private val zoneIdProvider: ZoneIdProvider,
) : ViewModel() {

    private val argId: Long =
        savedStateHandle.get<Long>(Screen.BlockingScheduleEditor.ARG_ID)
            ?: Screen.BlockingScheduleEditor.NEW_ID
    private val isEditMode: Boolean = argId != Screen.BlockingScheduleEditor.NEW_ID

    private val _uiState = MutableStateFlow(BlockingScheduleEditorUiState(isEditMode = isEditMode))
    val uiState: StateFlow<BlockingScheduleEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    val installedApps: StateFlow<List<AppInfo>> =
        appRepository.getInstalledApps()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Fetched once when the editor opens — usage data is stable enough that
     * re-querying mid-edit isn't worth the cost. Falls back to an empty map
     * if PACKAGE_USAGE_STATS hasn't been granted.
     */
    private val weeklyUsage: StateFlow<Map<String, Long>> =
        flow { emit(usageStatsRepository.weeklyUsageMillis()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /**
     * All installed apps decorated with weekly usage and sorted by
     * (usage desc, label asc). The screen takes the first 15 for the
     * inline picker and uses the full list as the "Browse all" source.
     */
    val pickerItems: StateFlow<List<AppPickerItem>> =
        combine(installedApps, weeklyUsage) { apps, usage ->
            apps
                .map { app ->
                    AppPickerItem(
                        packageName = app.packageName,
                        label = app.label,
                        weeklyUsageMillis = usage[app.packageName] ?: 0L,
                    )
                }
                .sortedWith(
                    compareByDescending<AppPickerItem> { it.weeklyUsageMillis }
                        .thenBy { it.label.lowercase() },
                )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val usageStatsAvailable: Boolean
        get() = usageStatsRepository.isAvailable()

    init {
        if (isEditMode) loadExisting()
        startActiveTicker()
    }

    private fun loadExisting() {
        viewModelScope.launch {
            val schedule = repository.getById(argId)
            if (schedule == null) {
                _events.emit(EditorEvent.LoadError("Schedule not found"))
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                name = schedule.name,
                start = schedule.start,
                end = schedule.end,
                days = schedule.days,
                packageNames = schedule.packageNames,
                isCurrentlyActive = ScheduleActivePredicate.isActive(
                    schedule, clock.now(), zoneIdProvider.current(),
                ),
                isLoaded = true,
            )
        }
    }

    private fun startActiveTicker() {
        viewModelScope.launch {
            while (isActive) {
                if (isEditMode) {
                    val schedule = repository.getById(argId)
                    val active = schedule != null && ScheduleActivePredicate.isActive(
                        schedule, clock.now(), zoneIdProvider.current(),
                    )
                    if (_uiState.value.isCurrentlyActive != active) {
                        _uiState.value = _uiState.value.copy(isCurrentlyActive = active)
                    }
                }
                delay(1_000L)
            }
        }
    }

    fun setName(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun setStart(time: LocalTime) {
        _uiState.value = _uiState.value.copy(start = time)
    }

    fun setEnd(time: LocalTime) {
        _uiState.value = _uiState.value.copy(end = time)
    }

    fun toggleDay(day: DayOfWeek) {
        val current = _uiState.value.days
        _uiState.value = _uiState.value.copy(
            days = if (day in current) current - day else current + day,
        )
    }

    fun togglePackage(packageName: String) {
        val current = _uiState.value.packageNames
        _uiState.value = _uiState.value.copy(
            packageNames = if (packageName in current) current - packageName else current + packageName,
        )
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isCurrentlyActive) {
            viewModelScope.launch {
                _events.emit(
                    EditorEvent.Error("This schedule is currently active and cannot be modified."),
                )
            }
            return
        }
        if (!state.isValid) return
        val draft = BlockingScheduleDraft(
            name = state.name.trim(),
            startMinuteOfDay = state.start.hour * 60 + state.start.minute,
            endMinuteOfDay = state.end.hour * 60 + state.end.minute,
            daysOfWeekMask = DayOfWeekMask.encode(state.days),
            packageNames = state.packageNames,
        )
        viewModelScope.launch {
            try {
                if (isEditMode) {
                    repository.update(argId, draft)
                } else {
                    repository.create(draft)
                }
                _events.emit(EditorEvent.Saved)
            } catch (e: ScheduleCurrentlyActiveException) {
                _events.emit(
                    EditorEvent.Error("This schedule is currently active and cannot be modified."),
                )
            } catch (e: InvalidBlockingScheduleException) {
                _events.emit(EditorEvent.Error(e.message ?: "Invalid schedule"))
            }
        }
    }

    fun onDelete() {
        if (!isEditMode) return
        if (_uiState.value.isCurrentlyActive) {
            viewModelScope.launch {
                _events.emit(
                    EditorEvent.Error("This schedule is currently active and cannot be deleted."),
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.delete(argId)
                _events.emit(EditorEvent.Deleted)
            } catch (e: ScheduleCurrentlyActiveException) {
                _events.emit(
                    EditorEvent.Error("This schedule is currently active and cannot be deleted."),
                )
            }
        }
    }
}

data class BlockingScheduleEditorUiState(
    val isEditMode: Boolean = false,
    val isLoaded: Boolean = false,
    val name: String = "",
    val start: LocalTime = LocalTime.of(9, 0),
    val end: LocalTime = LocalTime.of(17, 0),
    val days: Set<DayOfWeek> = emptySet(),
    val packageNames: Set<String> = emptySet(),
    val isCurrentlyActive: Boolean = false,
) {
    val nameError: Boolean get() = name.isBlank()
    val timeError: Boolean
        get() = start == end
    val daysError: Boolean get() = days.isEmpty()
    val appsError: Boolean get() = packageNames.isEmpty()

    val isValid: Boolean
        get() = !nameError && !timeError && !daysError && !appsError

    val canSave: Boolean
        get() = isValid && !isCurrentlyActive

    val canDelete: Boolean
        get() = isEditMode && !isCurrentlyActive
}

/**
 * Display row for the schedule editor's app picker. weeklyUsageMillis is the
 * trailing-7-day total time-in-foreground for [packageName]; 0 means either
 * no recorded usage or no permission to read usage stats.
 */
data class AppPickerItem(
    val packageName: String,
    val label: String,
    val weeklyUsageMillis: Long,
)

sealed interface EditorEvent {
    data object Saved : EditorEvent
    data object Deleted : EditorEvent
    data class Error(val message: String) : EditorEvent
    data class LoadError(val message: String) : EditorEvent
}

/**
 * Convert a [BlockingSchedule] back into a [BlockingScheduleEditorUiState].
 * Convenience for tests; UI uses the ViewModel directly.
 */
fun BlockingSchedule.toEditorUiState(): BlockingScheduleEditorUiState =
    BlockingScheduleEditorUiState(
        isEditMode = true,
        isLoaded = true,
        name = name,
        start = start,
        end = end,
        days = days,
        packageNames = packageNames,
    )
