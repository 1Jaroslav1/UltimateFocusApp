/**
 * ViewModel for the In-App Reminder settings page. Exposes a single
 * StateFlow<InAppReminderUiState> that combines the global feature
 * configuration (master / precision / mindful delay / default expiry mode)
 * with the per-app opt-in list and the live filter query, so the screen
 * can render disabled state (master OFF → grey out everything) and the
 * searchable app list as pure functions of one snapshot.
 *
 * All mutations are write-through to the repository — every control
 * commits immediately (per ULT-77 AC: "no separate save action needed").
 *
 * Covers: ULT-76, ULT-77
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.data.repository.AppRepository
import com.ultimatefocus.app.domain.model.InAppReminderSettings
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.repository.SessionTimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppToggleRow(
    val packageName: String,
    val label: String,
    val enabled: Boolean,
)

data class InAppReminderUiState(
    val settings: InAppReminderSettings = InAppReminderSettings(),
    val query: String = "",
    val visibleApps: List<AppToggleRow> = emptyList(),
    val totalAppCount: Int = 0,
)

@HiltViewModel
class InAppReminderViewModel @Inject constructor(
    private val repository: SessionTimerRepository,
    appRepository: AppRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<InAppReminderUiState> = combine(
        repository.observeSettings(),
        repository.observeEnabledPackages(),
        appRepository.getInstalledApps(),
        query,
    ) { settings, enabledPkgs, allApps, q ->
        val sorted = allApps.sortedBy { it.label.lowercase() }
        val filtered = if (q.isBlank()) {
            sorted
        } else {
            sorted.filter { it.label.contains(q.trim(), ignoreCase = true) }
        }
        InAppReminderUiState(
            settings = settings,
            query = q,
            visibleApps = filtered.map {
                AppToggleRow(
                    packageName = it.packageName,
                    label = it.label,
                    enabled = it.packageName in enabledPkgs,
                )
            },
            totalAppCount = sorted.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InAppReminderUiState(),
    )

    /** Convenience accessor for tests/legacy ViewModel tests scoped to global only. */
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

    fun setQuery(newQuery: String) { query.value = newQuery }

    fun setAppEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(packageName, enabled) }
    }
}
