/**
 * ViewModel that exposes user preferences and provides actions to update them.
 * Used by LauncherActivity to apply the theme before rendering any screen.
 * Covers: ULT-6, ULT-8
 */
package com.minimalist.launcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalist.launcher.domain.repository.UserPreferencesSource
import com.minimalist.launcher.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesSource: UserPreferencesSource,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesSource.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    fun toggleDarkTheme() {
        viewModelScope.launch {
            preferencesSource.updateDarkTheme(!preferences.value.isDarkTheme)
        }
    }

    fun toggleAutoKeyboard() {
        viewModelScope.launch {
            preferencesSource.updateAutoKeyboard(!preferences.value.autoKeyboard)
        }
    }
}
