/**
 * ViewModel for the mindful launch delay overlay (ULT-79). Owns the
 * countdown timer and the post-delay launch dispatch.
 *
 * Drives a determinate progress value (0f → 1f) over DELAY_MS so the
 * overlay can show a circle that visibly fills from a dot to a full
 * circle. The progress is updated on a 32ms tick (~30fps) which is
 * smooth enough for the eye and cheap enough on the main thread.
 *
 * Splits responsibility into two events: `Done` (timer elapsed) and an
 * explicit `completeLaunch()` call from the activity. This keeps the
 * side-effect (Intent launch) outside the timer coroutine so tests can
 * drive the state machine without an Android context.
 *
 * Covers: ULT-79
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MindfulLaunchDelayUiState(
    val packageName: String = "",
    val appLabel: String = "",
    val started: Boolean = false,
    /** Progress 0f..1f — 0f = dot, 1f = full circle. */
    val progress: Float = 0f,
)

@HiltViewModel
class MindfulLaunchDelayViewModel @Inject constructor(
    private val appRepository: AppRepository,
) : ViewModel() {

    sealed interface Event {
        data object Done : Event
    }

    private val _state = MutableStateFlow(MindfulLaunchDelayUiState())
    val state: StateFlow<MindfulLaunchDelayUiState> = _state.asStateFlow()

    private val _events = Channel<Event>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun bind(packageName: String, appLabel: String) {
        if (_state.value.started) return // ignore re-binds (config change)
        _state.value = MindfulLaunchDelayUiState(
            packageName = packageName,
            appLabel = appLabel,
            started = true,
            progress = 0f,
        )
        viewModelScope.launch {
            val total = MindfulLaunchDelayActivity.DELAY_MS
            val tick = PROGRESS_TICK_MS
            val startMs = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startMs
                val ratio = (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                _state.value = _state.value.copy(progress = ratio)
                if (elapsed >= total) break
                delay(tick)
            }
            _events.send(Event.Done)
        }
    }

    /**
     * Resolves the AppInfo and dispatches the launch through AppRepository.
     * Called by the activity after the Done event is observed.
     */
    fun completeLaunch() {
        val s = _state.value
        if (s.packageName.isBlank()) return
        viewModelScope.launch {
            val target = appRepository.getInstalledApps().first()
                .firstOrNull { it.packageName == s.packageName } ?: return@launch
            appRepository.launchApp(target)
        }
    }

    private companion object {
        const val PROGRESS_TICK_MS: Long = 32L
    }
}
