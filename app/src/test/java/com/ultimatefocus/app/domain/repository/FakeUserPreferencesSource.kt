/**
 * In-memory test double for UserPreferencesSource. Returns configurable preference
 * flows without any Android context or DataStore dependency. Supports shortcut
 * assignment mutations used by HomeViewModel tests, and the In-App Reminder
 * global flags used by InAppReminderViewModel tests.
 * Covers: ULT-6, ULT-8, ULT-13, ULT-14, ULT-16, ULT-18, ULT-76
 */
package com.ultimatefocus.app.domain.repository

import com.ultimatefocus.app.domain.model.FontSize
import com.ultimatefocus.app.domain.model.HomeAlignment
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.model.ShortcutSlot
import com.ultimatefocus.app.domain.model.ShortcutTarget
import com.ultimatefocus.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserPreferencesSource : UserPreferencesSource {

    private val _preferences = MutableStateFlow(UserPreferences())
    private val _categoriesSeeded = MutableStateFlow(false)

    override val preferences: Flow<UserPreferences> = _preferences

    override val categoriesSeeded: Flow<Boolean> = _categoriesSeeded

    fun setPreferences(prefs: UserPreferences) { _preferences.value = prefs }

    fun setCategoriesSeeded(seeded: Boolean) { _categoriesSeeded.value = seeded }
    fun categoriesSeededValue(): Boolean = _categoriesSeeded.value

    override suspend fun markCategoriesSeeded() { _categoriesSeeded.value = true }

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

    override suspend fun updateTwoFingerDoubleTapToLock(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(twoFingerDoubleTapToLockEnabled = enabled)
    }

    override suspend fun updateShortcut(slot: ShortcutSlot, target: ShortcutTarget?) {
        _preferences.value = when (slot) {
            ShortcutSlot.LEFT -> _preferences.value.copy(leftShortcut = target)
            ShortcutSlot.RIGHT -> _preferences.value.copy(rightShortcut = target)
        }
    }

    override suspend fun updateInAppReminderMasterEnabled(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(inAppReminderMasterEnabled = enabled)
    }

    override suspend fun updateInAppReminderPrecisionMode(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(inAppReminderPrecisionMode = enabled)
    }

    override suspend fun updateInAppReminderMindfulLaunchDelay(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(inAppReminderMindfulLaunchDelay = enabled)
    }

    override suspend fun updateInAppReminderExpiryMode(mode: SessionExpiryMode) {
        _preferences.value = _preferences.value.copy(inAppReminderExpiryMode = mode)
    }
}
