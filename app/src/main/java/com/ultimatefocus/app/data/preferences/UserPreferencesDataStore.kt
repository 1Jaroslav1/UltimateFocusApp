/**
 * DataStore-backed implementation of UserPreferencesSource.
 * Persists user preferences (theme, font size, alignment, auto-keyboard, search
 * provider, double-tap-to-lock) to disk and exposes them as a Flow so the UI
 * reacts to changes without polling.
 * Covers: ULT-6, ULT-8, ULT-16, ULT-18
 */
package com.ultimatefocus.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ultimatefocus.app.domain.model.FontSize
import com.ultimatefocus.app.domain.model.HomeAlignment
import com.ultimatefocus.app.domain.model.UserPreferences
import com.ultimatefocus.app.domain.repository.UserPreferencesSource
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
        val SEARCH_PROVIDER_PACKAGE = stringPreferencesKey("search_provider_package")
        val DOUBLE_TAP_TO_LOCK = booleanPreferencesKey("double_tap_to_lock")
    }

    override val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            fontSize = prefs[Keys.FONT_SIZE]?.let { FontSize.valueOf(it) } ?: FontSize.MEDIUM,
            homeAlignment = prefs[Keys.HOME_ALIGNMENT]?.let { HomeAlignment.valueOf(it) }
                ?: HomeAlignment.LEFT,
            autoKeyboard = prefs[Keys.AUTO_KEYBOARD] ?: true,
            searchProviderPackage = prefs[Keys.SEARCH_PROVIDER_PACKAGE],
            doubleTapToLockEnabled = prefs[Keys.DOUBLE_TAP_TO_LOCK] ?: false,
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

    override suspend fun updateSearchProviderPackage(packageName: String?) {
        context.dataStore.edit { prefs ->
            if (packageName == null) prefs.remove(Keys.SEARCH_PROVIDER_PACKAGE)
            else prefs[Keys.SEARCH_PROVIDER_PACKAGE] = packageName
        }
    }

    override suspend fun updateDoubleTapToLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DOUBLE_TAP_TO_LOCK] = enabled }
    }
}
