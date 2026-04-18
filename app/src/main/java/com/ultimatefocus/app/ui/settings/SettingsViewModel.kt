/**
 * ViewModel that exposes user preferences and provides actions to update them.
 * Used by LauncherActivity to apply the theme before rendering any screen.
 * Also exposes the list of discovered web-search providers (ULT-16) and the
 * two-finger-long-press-to-lock toggle, which triggers a device-admin prompt
 * when enabled without existing permission (ULT-18).
 * Covers: ULT-6, ULT-8, ULT-16, ULT-18
 */
package com.ultimatefocus.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.domain.repository.UserPreferencesSource
import com.ultimatefocus.app.domain.model.UserPreferences
import com.ultimatefocus.app.domain.service.SystemActions
import com.ultimatefocus.app.domain.service.WebSearchProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesSource: UserPreferencesSource,
    private val systemActions: SystemActions,
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

    /** Installed apps that can handle the swipe-up search intent (ULT-16). */
    fun availableSearchProviders(): List<WebSearchProvider> =
        systemActions.queryWebSearchProviders()

    /** null ⇒ "System default" (ULT-16). */
    fun updateSearchProviderPackage(packageName: String?) {
        viewModelScope.launch {
            preferencesSource.updateSearchProviderPackage(packageName)
        }
    }

    /**
     * Enable or disable two-finger-long-press-to-lock (ULT-18). When enabling
     * without existing device-admin permission, the system prompt is launched
     * so the feature is usable immediately.
     */
    fun setTwoFingerLongPressToLock(enabled: Boolean) {
        if (enabled && !systemActions.isLockPermissionGranted()) {
            systemActions.requestLockPermission()
        }
        viewModelScope.launch {
            preferencesSource.updateTwoFingerLongPressToLock(enabled)
        }
    }
}
