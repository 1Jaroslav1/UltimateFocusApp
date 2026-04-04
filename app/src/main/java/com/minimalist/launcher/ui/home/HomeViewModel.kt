/**
 * ViewModel for the home screen. Manages:
 * - Reactive favorite apps list (ULT-6)
 * - Clock/battery state updated every second (ULT-6)
 * - User preferences for layout alignment and theme (ULT-6)
 * Covers: ULT-6
 */
package com.minimalist.launcher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minimalist.launcher.domain.repository.UserPreferencesSource
import com.minimalist.launcher.data.repository.AppRepository
import com.minimalist.launcher.domain.model.AppInfo
import com.minimalist.launcher.domain.model.UserPreferences
import com.minimalist.launcher.util.getBatteryLevel
import com.minimalist.launcher.util.isCharging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val appRepository: AppRepository,
    private val preferencesSource: UserPreferencesSource,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val favoriteApps: StateFlow<List<AppInfo>> = appRepository.getFavoriteApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val preferences: StateFlow<UserPreferences> = preferencesSource.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    init {
        startClockLoop()
    }

    private fun startClockLoop() {
        viewModelScope.launch {
            while (isActive) {
                _uiState.value = HomeUiState(
                    currentTime = java.time.LocalDateTime.now(),
                    batteryLevel = getBatteryLevel(application),
                    isCharging = isCharging(application),
                )
                delay(1_000L)
            }
        }
    }

    fun launchApp(appInfo: AppInfo) {
        viewModelScope.launch { appRepository.launchApp(appInfo) }
    }

    fun toggleFavorite(appInfo: AppInfo) {
        viewModelScope.launch { appRepository.toggleFavorite(appInfo) }
    }
}
