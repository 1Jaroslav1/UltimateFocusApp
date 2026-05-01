/**
 * ViewModel for the In-App Reminder settings page. Exposes a single
 * StateFlow<InAppReminderUiState> that combines the global feature
 * configuration (master / precision / mindful delay / default expiry mode)
 * with the per-app opt-in list and the live filter query, so the screen
 * can render disabled state (master OFF → grey out everything) and the
 * searchable app list as pure functions of one snapshot.
 *
 * Per ULT-82 #4 the per-app list mirrors the schedule editor's picker:
 * each row carries weeklyUsageMillis (read from AppUsageStatsRepository),
 * and the list is sorted by weekly usage descending then label ascending
 * so the most-used apps surface first. When the PACKAGE_USAGE_STATS
 * permission is missing, all apps fall through with weekly usage 0L and
 * the sort collapses to alphabetical.
 *
 * All mutations are write-through to the repository — every control
 * commits immediately (per ULT-77 AC: "no separate save action needed").
 *
 * Covers: ULT-76, ULT-77, ULT-82
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.data.repository.AppRepository
import com.ultimatefocus.app.domain.model.InAppReminderSettings
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.repository.AppUsageStatsRepository
import com.ultimatefocus.app.domain.repository.SessionTimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppToggleRow(
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    val weeklyUsageMillis: Long = 0L,
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
    private val usageStatsRepository: AppUsageStatsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    /**
     * Snapshot of trailing-7-day per-package usage. Recomputed each time
     * the installed-apps list changes (the membership flow is the natural
     * trigger — a new install / uninstall is the only thing that can
     * meaningfully change the picker). Empty map when usage permission
     * has not been granted.
     */
    private val weeklyUsageFlow = appRepository.getInstalledApps().map { apps ->
        // Capture both the apps list and the usage snapshot in a tuple so
        // downstream combine has a stable join key.
        apps to runCatching { usageStatsRepository.weeklyUsageMillis() }.getOrDefault(emptyMap())
    }

    val uiState: StateFlow<InAppReminderUiState> = combine(
        repository.observeSettings(),
        repository.observeEnabledPackages(),
        weeklyUsageFlow,
        query,
    ) { settings, enabledPkgs, (allApps, usageMap), q ->
        val rows = allApps.map { app ->
            AppToggleRow(
                packageName = app.packageName,
                label = app.label,
                enabled = app.packageName in enabledPkgs,
                weeklyUsageMillis = usageMap[app.packageName] ?: 0L,
            )
        }
        val sorted = rows.sortedWith(
            compareByDescending<AppToggleRow> { it.weeklyUsageMillis }
                .thenBy { it.label.lowercase() },
        )
        val filtered = if (q.isBlank()) {
            sorted
        } else {
            sorted.filter { it.label.contains(q.trim(), ignoreCase = true) }
        }
        InAppReminderUiState(
            settings = settings,
            query = q,
            visibleApps = filtered,
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
