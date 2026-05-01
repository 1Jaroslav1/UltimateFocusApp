/**
 * Interface for reading and writing user preferences.
 * Decouples ViewModels from the DataStore implementation so they can be
 * unit-tested with a fake in-memory implementation.
 * Covers: ULT-6, ULT-13, ULT-14, ULT-16, ULT-18, ULT-76
 */
package com.ultimatefocus.app.domain.repository

import com.ultimatefocus.app.domain.model.FontSize
import com.ultimatefocus.app.domain.model.HomeAlignment
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.model.ShortcutSlot
import com.ultimatefocus.app.domain.model.ShortcutTarget
import com.ultimatefocus.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesSource {
    val preferences: Flow<UserPreferences>

    /**
     * Onboarding flag: true once CategorySeeder has run successfully (or
     * deliberately skipped, e.g. user-already-has-N-tags). Stored
     * separately from [preferences] because it is internal bookkeeping,
     * not a user-facing setting. (ULT-85)
     */
    val categoriesSeeded: Flow<Boolean>

    suspend fun updateDarkTheme(isDark: Boolean)
    suspend fun updateFontSize(fontSize: FontSize)
    suspend fun updateHomeAlignment(alignment: HomeAlignment)
    suspend fun updateAutoKeyboard(enabled: Boolean)
    suspend fun updateSearchProviderPackage(packageName: String?)
    suspend fun updateTwoFingerDoubleTapToLock(enabled: Boolean)
    suspend fun updateShortcut(slot: ShortcutSlot, target: ShortcutTarget?)
    suspend fun updateInAppReminderMasterEnabled(enabled: Boolean)
    suspend fun updateInAppReminderPrecisionMode(enabled: Boolean)
    suspend fun updateInAppReminderMindfulLaunchDelay(enabled: Boolean)
    suspend fun updateInAppReminderExpiryMode(mode: SessionExpiryMode)

    /** Marks the auto-categories onboarding as completed. (ULT-85) */
    suspend fun markCategoriesSeeded()
}
