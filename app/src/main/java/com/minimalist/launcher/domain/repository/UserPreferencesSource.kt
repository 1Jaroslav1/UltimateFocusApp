/**
 * Interface for reading and writing user preferences.
 * Decouples ViewModels from the DataStore implementation so they can be
 * unit-tested with a fake in-memory implementation.
 * Covers: ULT-6
 */
package com.minimalist.launcher.domain.repository

import com.minimalist.launcher.domain.model.FontSize
import com.minimalist.launcher.domain.model.HomeAlignment
import com.minimalist.launcher.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesSource {
    val preferences: Flow<UserPreferences>
    suspend fun updateDarkTheme(isDark: Boolean)
    suspend fun updateFontSize(fontSize: FontSize)
    suspend fun updateHomeAlignment(alignment: HomeAlignment)
    suspend fun updateAutoKeyboard(enabled: Boolean)
}
