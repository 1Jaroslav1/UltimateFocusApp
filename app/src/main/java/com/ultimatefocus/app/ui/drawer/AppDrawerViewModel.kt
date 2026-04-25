/**
 * ViewModel for the full app drawer. Holds the search query and produces
 * a filtered, alphabetically sorted list of installed apps for display.
 * Exposes the autoKeyboard preference so the screen auto-focuses the
 * search field when the setting is enabled (ULT-8).
 * Exposes blockApp() to commit a manual block on a chosen app + duration (ULT-20).
 * Emits blocked launch attempts on `blockedAttempts` so the Screen can
 * open BlockedAppActivity — ViewModel stays Context-free (ULT-21).
 * Exposes selectionState for bulk-select multi-app tagging (ULT-73): when
 * active, drawer rows toggle membership rather than launching, and a
 * selection bar replaces the SearchBar.
 * Covers: ULT-7, ULT-8, ULT-20, ULT-21, ULT-73
 */
package com.ultimatefocus.app.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.data.repository.AppRepository
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.LaunchResult
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import com.ultimatefocus.app.domain.repository.UserPreferencesSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Snapshot of the bulk selection mode state. */
data class SelectionState(
    val active: Boolean = false,
    val selectedPackages: Set<String> = emptySet(),
)

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val preferencesSource: UserPreferencesSource,
    private val blockedAppRepository: BlockedAppRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _blockedAttempts = MutableSharedFlow<LaunchResult.Blocked>(extraBufferCapacity = 4)
    val blockedAttempts: SharedFlow<LaunchResult.Blocked> = _blockedAttempts.asSharedFlow()

    private val _selectionState = MutableStateFlow(SelectionState())
    val selectionState: StateFlow<SelectionState> = _selectionState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AppDrawerUiState> = _searchQuery
        .flatMapLatest { query -> appRepository.searchApps(query) }
        .map { apps -> AppDrawerUiState.Success(apps) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDrawerUiState.Loading)

    val autoKeyboard: StateFlow<Boolean> = preferencesSource.preferences
        .map { it.autoKeyboard }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun launchApp(appInfo: AppInfo) {
        viewModelScope.launch {
            val result = appRepository.launchApp(appInfo)
            if (result is LaunchResult.Blocked) _blockedAttempts.emit(result)
        }
    }

    fun toggleFavorite(appInfo: AppInfo) {
        viewModelScope.launch { appRepository.toggleFavorite(appInfo) }
    }

    fun blockApp(packageName: String, duration: BlockDuration) {
        viewModelScope.launch { blockedAppRepository.block(packageName, duration) }
    }

    fun toggleSelection(packageName: String) {
        val current = _selectionState.value
        val nextSet = if (packageName in current.selectedPackages) {
            current.selectedPackages - packageName
        } else {
            current.selectedPackages + packageName
        }
        _selectionState.value = SelectionState(
            active = nextSet.isNotEmpty(),
            selectedPackages = nextSet,
        )
    }

    fun exitSelectionMode() {
        _selectionState.value = SelectionState()
    }
}
