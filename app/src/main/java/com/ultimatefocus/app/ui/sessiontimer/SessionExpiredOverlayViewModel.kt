/**
 * ViewModel for SessionExpiredOverlayActivity (ULT-81 / Mode 1 — mindful
 * extension delay). Owns:
 *  - the forced countdown timer (EXTEND_DELAY_MS) before the Extend button
 *    becomes tappable;
 *  - a state.canExtend flag that the screen binds against to render the
 *    button's enabled state;
 *  - events the host activity consumes (CountdownDone → release back-press
 *    suppression; ExtendRequested → re-open the picker; Cancelled → finish).
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
    val secondsRemaining: Int = 0,
    val canExtend: Boolean = false,
    /** Progress 0f..1f over the forced wait — 0f = dot, 1f = full circle. */
    val progress: Float = 0f,
)

@HiltViewModel
class SessionExpiredOverlayViewModel @Inject constructor(
    private val coordinator: SessionTimerCoordinator,
) : ViewModel() {

    sealed interface Event {
        data object CountdownDone : Event
        data object ExtendRequested : Event
        data object Cancelled : Event
    }

    private val _state = MutableStateFlow(SessionExpiredOverlayUiState())
    val state: StateFlow<SessionExpiredOverlayUiState> = _state.asStateFlow()

    private val _events = Channel<Event>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var bound = false

    fun bind(packageName: String, appLabel: String) {
        if (bound) return
        bound = true
        val totalMs = SessionExpiredOverlayActivity.EXTEND_DELAY_MS
        _state.value = SessionExpiredOverlayUiState(
            packageName = packageName,
            appLabel = appLabel,
            secondsRemaining = (totalMs / 1_000L).toInt(),
            canExtend = false,
            progress = 0f,
        )
        viewModelScope.launch {
            val tick = PROGRESS_TICK_MS
            val startMs = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startMs
                val ratio = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                val sec = ((totalMs - elapsed).toInt() / 1_000).coerceAtLeast(0)
                _state.value = _state.value.copy(
                    secondsRemaining = sec,
                    progress = ratio,
                )
                if (elapsed >= totalMs) break
                delay(tick)
            }
            _state.value = _state.value.copy(
                secondsRemaining = 0,
                progress = 1f,
                canExtend = true,
            )
            _events.send(Event.CountdownDone)
        }
    }

    fun extend() {
        if (!_state.value.canExtend) return
        // Clear the prior session so the picker's pre-check can re-prompt
        // for a fresh duration without colliding with the now-expired one.
        coordinator.endSession()
        viewModelScope.launch { _events.send(Event.ExtendRequested) }
    }

    fun cancel() {
        coordinator.endSession()
        viewModelScope.launch { _events.send(Event.Cancelled) }
    }

    private companion object {
        const val PROGRESS_TICK_MS: Long = 32L
    }
}
