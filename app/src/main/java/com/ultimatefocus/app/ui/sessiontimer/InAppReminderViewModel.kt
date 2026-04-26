/**
 * ViewModel for the In-App Reminder settings page. Exposes a single
 * StateFlow<InAppReminderSettings> so the screen can render disabled state
 * (master OFF → grey out everything below) as a pure function of a single
 * snapshot. All mutations are write-through to the repository — there is no
 * intermediate "dirty" state because every control commits immediately.
 *
 * Per-app opt-in state and toggle handlers ship in ULT-77; this commit is
 * scoped to the global controls (master, precision mode, mindful launch
 * delay, default expiry mode).
 *
 * Covers: ULT-76
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.domain.model.InAppReminderSettings
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.repository.SessionTimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InAppReminderViewModel @Inject constructor(
    private val repository: SessionTimerRepository,
) : ViewModel() {

    val settings: StateFlow<InAppReminderSettings> = repository.observeSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InAppReminderSettings(),
        )

    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateMasterEnabled(enabled) }
    }

    fun togglePrecisionMode() {
        viewModelScope.launch {
            repository.updatePrecisionMode(!settings.value.precisionMode)
        }
    }

    fun setMindfulLaunchDelay(enabled: Boolean) {
        viewModelScope.launch { repository.updateMindfulLaunchDelay(enabled) }
    }

    fun setExpiryMode(mode: SessionExpiryMode) {
        viewModelScope.launch { repository.updateExpiryMode(mode) }
    }
}
