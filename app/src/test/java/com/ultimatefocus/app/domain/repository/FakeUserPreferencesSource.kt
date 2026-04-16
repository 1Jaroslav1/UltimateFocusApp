/**
 * In-memory test double for UserPreferencesSource. Returns configurable preference
 * flows without any Android context or DataStore dependency.
 * Covers: ULT-6, ULT-8, ULT-16, ULT-18
 */
package com.ultimatefocus.app.domain.repository

import com.ultimatefocus.app.domain.model.FontSize
import com.ultimatefocus.app.domain.model.HomeAlignment
import com.ultimatefocus.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserPreferencesSource : UserPreferencesSource {

    private val _preferences = MutableStateFlow(UserPreferences())

    override val preferences: Flow<UserPreferences> = _preferences

    fun setPreferences(prefs: UserPreferences) { _preferences.value = prefs }

    override suspend fun updateDarkTheme(isDark: Boolean) {
        _preferences.value = _preferences.value.copy(isDarkTheme = isDark)
    }

    override suspend fun updateFontSize(fontSize: FontSize) {
        _preferences.value = _preferences.value.copy(fontSize = fontSize)
    }

    override suspend fun updateHomeAlignment(alignment: HomeAlignment) {
        _preferences.value = _preferences.value.copy(homeAlignment = alignment)
    }

    override suspend fun updateAutoKeyboard(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(autoKeyboard = enabled)
    }

    override suspend fun updateSearchProviderPackage(packageName: String?) {
        _preferences.value = _preferences.value.copy(searchProviderPackage = packageName)
    }

    override suspend fun updateDoubleTapToLock(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(doubleTapToLockEnabled = enabled)
    }
}
