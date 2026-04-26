/**
 * ViewModel for the mindful launch delay overlay (ULT-79). Owns the
 * countdown timer (a single delay) and the post-delay launch dispatch.
 *
 * Splits responsibility into two events: `Done` (timer elapsed) and an
 * explicit `completeLaunch()` call from the activity, mirroring the
 * picker's pattern. This keeps the side-effect (Intent launch) outside
 * the timer coroutine so tests can drive the state machine without an
 * Android context.
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
        )
        viewModelScope.launch {
            delay(MindfulLaunchDelayActivity.DELAY_MS)
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
}
