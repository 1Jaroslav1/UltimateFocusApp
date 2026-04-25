/**
 * ViewModel for BlockingSchedulesListScreen. Combines the persisted schedule
 * list with a 1-second tick (driven by the injected Clock + ZoneIdProvider)
 * so each row's "currently active" indicator stays accurate without a
 * manual refresh — also fed into the editor's save/delete guard via the
 * id-based observation that re-emits when the predicate flips.
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.ui.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.domain.model.BlockingSchedule
import com.ultimatefocus.app.domain.repository.BlockingScheduleRepository
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.domain.service.ScheduleActivePredicate
import com.ultimatefocus.app.domain.service.ZoneIdProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BlockingSchedulesListViewModel @Inject constructor(
    private val repository: BlockingScheduleRepository,
    private val clock: Clock,
    private val zoneIdProvider: ZoneIdProvider,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BlockingSchedulesListUiState> = combine(
        repository.observeAll(),
        flow {
            // 1-second tick to keep the "active now" indicator fresh without
            // relying on schedule mutations to invalidate the flow.
            while (true) {
                emit(clock.now())
                delay(1_000L)
            }
        },
    ) { schedules, now ->
        val zone = zoneIdProvider.current()
        BlockingSchedulesListUiState(
            schedules = schedules.map { schedule ->
                BlockingScheduleRow(
                    schedule = schedule,
                    isCurrentlyActive = ScheduleActivePredicate.isActive(schedule, now, zone),
                )
            },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        BlockingSchedulesListUiState(),
    )
}

data class BlockingSchedulesListUiState(
    val schedules: List<BlockingScheduleRow> = emptyList(),
)

data class BlockingScheduleRow(
    val schedule: BlockingSchedule,
    val isCurrentlyActive: Boolean,
)
