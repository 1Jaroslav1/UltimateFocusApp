/**
 * ViewModel for the full app drawer. Holds the search query and produces
 * a filtered, alphabetically sorted list of installed apps for display.
 * Exposes the autoKeyboard preference so the screen auto-focuses the
 * search field when the setting is enabled (ULT-8).
 * Exposes blockApp() to commit a manual block on a chosen app + duration (ULT-20).
 * Covers: ULT-7, ULT-8, ULT-20
 */
package com.ultimatefocus.app.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.data.repository.AppRepository
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import com.ultimatefocus.app.domain.repository.UserPreferencesSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val preferencesSource: UserPreferencesSource,
    private val blockedAppRepository: BlockedAppRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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
        viewModelScope.launch { appRepository.launchApp(appInfo) }
    }

    fun toggleFavorite(appInfo: AppInfo) {
        viewModelScope.launch { appRepository.toggleFavorite(appInfo) }
    }

    fun blockApp(packageName: String, duration: BlockDuration) {
        viewModelScope.launch { blockedAppRepository.block(packageName, duration) }
    }
}
