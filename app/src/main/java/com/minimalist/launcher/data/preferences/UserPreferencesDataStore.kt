/**
 * DataStore-backed implementation of UserPreferencesSource.
 * Persists user preferences (theme, font size, alignment, auto-keyboard) to disk
 * and exposes them as a Flow so the UI reacts to changes without polling.
 * Covers: ULT-6, ULT-8
 */
package com.minimalist.launcher.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.minimalist.launcher.domain.model.FontSize
import com.minimalist.launcher.domain.model.HomeAlignment
import com.minimalist.launcher.domain.model.UserPreferences
import com.minimalist.launcher.domain.repository.UserPreferencesSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesSource {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val HOME_ALIGNMENT = stringPreferencesKey("home_alignment")
        val AUTO_KEYBOARD = booleanPreferencesKey("auto_keyboard")
    }

    override val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            fontSize = prefs[Keys.FONT_SIZE]?.let { FontSize.valueOf(it) } ?: FontSize.MEDIUM,
            homeAlignment = prefs[Keys.HOME_ALIGNMENT]?.let { HomeAlignment.valueOf(it) }
                ?: HomeAlignment.LEFT,
            autoKeyboard = prefs[Keys.AUTO_KEYBOARD] ?: true,
        )
    }

    override suspend fun updateDarkTheme(isDark: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = isDark }
    }

    override suspend fun updateFontSize(fontSize: FontSize) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = fontSize.name }
    }

    override suspend fun updateHomeAlignment(alignment: HomeAlignment) {
        context.dataStore.edit { it[Keys.HOME_ALIGNMENT] = alignment.name }
    }

    override suspend fun updateAutoKeyboard(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_KEYBOARD] = enabled }
    }
}
