/**
 * ViewModel for SessionExpiredOverlayActivity (ULT-81 / Mode 1 — mindful
 * extension delay). Owns:
 *  - the forced wait timer (EXTEND_DELAY_MS), driving a determinate
 *    progress value 0f..1f for the loader;
 *  - a single `Done` event the host activity consumes to automatically
 *    launch the duration picker.
 *
 * Per the user's preferred UX, this overlay has no "Extend" button and
 * no "Cancel" affordance — the friction is the wait, full stop. When
 * the timer completes the activity finishes after handing control to
 * the picker for a fresh session commitment.
 *
 * Covers: ULT-81
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.domain.service.SessionTimerCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionExpiredOverlayUiState(
    val packageName: String = "",
    val appLabel: String = "",
    /** Progress 0f..1f over the forced wait — 0f = dot, 1f = full circle. */
    val progress: Float = 0f,
)

@HiltViewModel
class SessionExpiredOverlayViewModel @Inject constructor(
    private val coordinator: SessionTimerCoordinator,
) : ViewModel() {

    sealed interface Event {
        /** Loader finished — host activity should launch the picker and finish. */
        data object Done : Event
    }

    private val _state = MutableStateFlow(SessionExpiredOverlayUiState())
    val state: StateFlow<SessionExpiredOverlayUiState> = _state.asStateFlow()

    private val _events = Channel<Event>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var bound = false

    fun bind(packageName: String, appLabel: String) {
        if (bound) return
        bound = true
        // Clear the prior session so the picker's pre-check can re-prompt
        // for a fresh commitment without colliding with the now-expired
        // session record.
        coordinator.endSession()
        val totalMs = SessionExpiredOverlayActivity.EXTEND_DELAY_MS
        _state.value = SessionExpiredOverlayUiState(
            packageName = packageName,
            appLabel = appLabel,
            progress = 0f,
        )
        viewModelScope.launch {
            val tick = PROGRESS_TICK_MS
            val startMs = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startMs
                val ratio = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                _state.value = _state.value.copy(progress = ratio)
                if (elapsed >= totalMs) break
                delay(tick)
            }
            _state.value = _state.value.copy(progress = 1f)
            _events.send(Event.Done)
        }
    }

    private companion object {
        const val PROGRESS_TICK_MS: Long = 32L
    }
}
