/**
 * ViewModel for the full app drawer. Holds the search query and produces
 * a filtered, alphabetically sorted list of installed apps for display.
 * Reactive: any change to query or installed apps re-emits the list.
 * Covers: ULT-7, ULT-8
 */
package com.minimalist.launcher.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalist.launcher.data.repository.AppRepository
import com.minimalist.launcher.domain.model.AppInfo
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
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AppDrawerUiState> = _searchQuery
        .flatMapLatest { query -> appRepository.searchApps(query) }
        .map { apps -> AppDrawerUiState.Success(apps) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDrawerUiState.Loading)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun launchApp(appInfo: AppInfo) {
        viewModelScope.launch { appRepository.launchApp(appInfo) }
    }

    fun toggleFavorite(appInfo: AppInfo) {
        viewModelScope.launch { appRepository.toggleFavorite(appInfo) }
    }
}
