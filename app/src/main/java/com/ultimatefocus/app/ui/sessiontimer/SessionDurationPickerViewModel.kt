/**
 * ViewModel for the session-duration picker (ULT-78).
 *
 * State surface:
 *  - the target package + app label (received via bind from the activity).
 *  - the per-session expiry-mode override (initialized to the global
 *    default; the user can change it in the expanded panel).
 *  - the typed custom-minutes string + parsed validity flag.
 *  - whether the expanded panel is open.
 *
 * Event surface:
 *  - LaunchTarget: emitted when the user confirms a duration. The activity
 *    receives the event, asks the ViewModel to launch the target through
 *    AppRepository, and finishes.
 *  - Cancelled: back press / dismiss. The activity finishes without
 *    starting a session, satisfying the AC ("the app launch is cancelled").
 *
 * Per-session overrides do NOT mutate the global default — only the
 * snapshot started by SessionTimerCoordinator carries the override.
 *
 * Covers: ULT-78
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.data.repository.AppRepository
import com.ultimatefocus.app.domain.model.SessionDurationMinutes
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.repository.SessionTimerRepository
import com.ultimatefocus.app.domain.service.SessionTimerCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionDurationPickerUiState(
    val packageName: String = "",
    val appLabel: String = "",
    val expanded: Boolean = false,
    val customMinutes: String = "",
    val customMinutesValid: Boolean = false,
    val selectedMode: SessionExpiryMode = SessionExpiryMode.ONLY_REMINDER,
)

@HiltViewModel
class SessionDurationPickerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val coordinator: SessionTimerCoordinator,
    private val sessionTimerRepository: SessionTimerRepository,
) : ViewModel() {

    sealed interface Event {
        /**
         * Open the target app immediately (no mindful delay configured). The
         * activity hosting the picker calls launchTarget() and finishes.
         */
        data class LaunchTarget(val packageName: String) : Event

        /**
         * Route through the mindful launch delay overlay (ULT-79). The
         * activity starts MindfulLaunchDelayActivity and finishes; the delay
         * activity is responsible for completing the launch after the
         * animation finishes.
         */
        data class LaunchAfterDelay(val packageName: String, val appLabel: String) : Event

        data object Cancelled : Event
    }

    private val _state = MutableStateFlow(SessionDurationPickerUiState())
    val state: StateFlow<SessionDurationPickerUiState> = _state.asStateFlow()

    private val _events = Channel<Event>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun bind(packageName: String, appLabel: String, defaultMode: SessionExpiryMode) {
        _state.value = SessionDurationPickerUiState(
            packageName = packageName,
            appLabel = appLabel,
            selectedMode = defaultMode,
        )
    }

    fun toggleExpanded() {
        _state.value = _state.value.copy(expanded = !_state.value.expanded)
    }

    fun onCustomMinutesChange(input: String) {
        val sanitized = input.filter { it.isDigit() }.take(4)
        val parsed = sanitized.toIntOrNull()
        _state.value = _state.value.copy(
            customMinutes = sanitized,
            customMinutesValid = parsed != null && SessionDurationMinutes.isValid(parsed),
        )
    }

    fun onSelectExpiryMode(mode: SessionExpiryMode) {
        _state.value = _state.value.copy(selectedMode = mode)
    }

    fun confirmPreset(minutes: Int) {
        val duration = SessionDurationMinutes.coerce(minutes)
        startAndDispatch(duration)
    }

    fun confirmCustom() {
        val parsed = _state.value.customMinutes.toIntOrNull() ?: return
        if (!SessionDurationMinutes.isValid(parsed)) return
        startAndDispatch(SessionDurationMinutes(parsed))
    }

    fun cancel() {
        viewModelScope.launch { _events.send(Event.Cancelled) }
    }

    fun launchTarget() {
        val s = _state.value
        if (s.packageName.isBlank()) return
        viewModelScope.launch {
            // Resolve the AppInfo by packageName so we hand the real
            // ComponentName to launchApp; the launch goes through
            // AppRepository so any other gates (manual block, schedule, tag)
            // still apply. AppRepositoryImpl's session pre-check short-
            // circuits because we just started a session.
            val target = appRepository.getInstalledApps().first()
                .firstOrNull { it.packageName == s.packageName } ?: return@launch
            appRepository.launchApp(target)
        }
    }

    private fun startAndDispatch(duration: SessionDurationMinutes) {
        val s = _state.value
        if (s.packageName.isBlank()) return
        coordinator.startSession(
            packageName = s.packageName,
            appLabel = s.appLabel,
            durationMinutes = duration.value,
            expiryMode = s.selectedMode,
        )
        viewModelScope.launch {
            // Read the global mindful-launch-delay flag at confirmation time
            // — the user could have just toggled it from the In-App Reminder
            // settings page, and we want the freshest value.
            val delay = sessionTimerRepository.observeSettings().first().mindfulLaunchDelay
            val event = if (delay) {
                Event.LaunchAfterDelay(packageName = s.packageName, appLabel = s.appLabel)
            } else {
                Event.LaunchTarget(packageName = s.packageName)
            }
            _events.send(event)
        }
    }
}
