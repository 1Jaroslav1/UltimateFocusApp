/**
 * ViewModel for the home screen. Manages:
 * - Reactive favorite apps list (ULT-6)
 * - Clock/battery state updated every second (ULT-6)
 * - User preferences for layout alignment and theme (ULT-6)
 * Covers: ULT-6
 */
package com.ultimatefocus.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.domain.repository.UserPreferencesSource
import com.ultimatefocus.app.data.repository.AppRepository
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.UserPreferences
import com.ultimatefocus.app.util.getBatteryLevel
import com.ultimatefocus.app.util.isCharging
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
                _uiState.value = try {
                    HomeUiState(
                        currentTime = java.time.LocalDateTime.now(),
                        batteryLevel = getBatteryLevel(application),
                        isCharging = isCharging(application),
                    )
                } catch (_: Exception) {
                    // Battery reading may fail in unit-test environments without Android context
                    _uiState.value.copy(currentTime = java.time.LocalDateTime.now())
                }
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
