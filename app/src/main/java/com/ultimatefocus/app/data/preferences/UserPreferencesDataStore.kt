/**
 * DataStore-backed implementation of UserPreferencesSource.
 * Persists user preferences (theme, font size, alignment, auto-keyboard, search
 * provider, double-tap-to-lock) to disk and exposes them as a Flow so the UI
 * reacts to changes without polling.
 * Also persists optional bottom-bar quick-access shortcut overrides (per slot) as
 * a flattened component-name + label pair; absent keys mean "use the system default".
 * The In-App Reminder global flags (master / precision mode / mindful launch
 * delay / expiry mode) live here too so the settings page binds against a
 * single Flow<UserPreferences> projection.
 * Covers: ULT-6, ULT-8, ULT-13, ULT-14, ULT-16, ULT-18, ULT-76
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
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.model.ShortcutSlot
import com.ultimatefocus.app.domain.model.ShortcutTarget
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
        val TWO_FINGER_DOUBLE_TAP_TO_LOCK = booleanPreferencesKey("two_finger_double_tap_to_lock")
        val LEFT_SHORTCUT_COMPONENT = stringPreferencesKey("left_shortcut_component")
        val LEFT_SHORTCUT_LABEL = stringPreferencesKey("left_shortcut_label")
        val RIGHT_SHORTCUT_COMPONENT = stringPreferencesKey("right_shortcut_component")
        val RIGHT_SHORTCUT_LABEL = stringPreferencesKey("right_shortcut_label")
        val IN_APP_REMINDER_MASTER = booleanPreferencesKey("in_app_reminder_master")
        val IN_APP_REMINDER_PRECISION_MODE =
            booleanPreferencesKey("in_app_reminder_precision_mode")
        val IN_APP_REMINDER_MINDFUL_LAUNCH_DELAY =
            booleanPreferencesKey("in_app_reminder_mindful_launch_delay")
        val IN_APP_REMINDER_EXPIRY_MODE = stringPreferencesKey("in_app_reminder_expiry_mode")
    }

    override val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            fontSize = prefs[Keys.FONT_SIZE]?.let { FontSize.valueOf(it) } ?: FontSize.MEDIUM,
            homeAlignment = prefs[Keys.HOME_ALIGNMENT]?.let { HomeAlignment.valueOf(it) }
                ?: HomeAlignment.LEFT,
            autoKeyboard = prefs[Keys.AUTO_KEYBOARD] ?: true,
            searchProviderPackage = prefs[Keys.SEARCH_PROVIDER_PACKAGE],
            twoFingerDoubleTapToLockEnabled = prefs[Keys.TWO_FINGER_DOUBLE_TAP_TO_LOCK] ?: false,
            leftShortcut = readShortcut(
                prefs,
                Keys.LEFT_SHORTCUT_COMPONENT,
                Keys.LEFT_SHORTCUT_LABEL,
            ),
            rightShortcut = readShortcut(
                prefs,
                Keys.RIGHT_SHORTCUT_COMPONENT,
                Keys.RIGHT_SHORTCUT_LABEL,
            ),
            inAppReminderMasterEnabled = prefs[Keys.IN_APP_REMINDER_MASTER] ?: false,
            inAppReminderPrecisionMode = prefs[Keys.IN_APP_REMINDER_PRECISION_MODE] ?: false,
            inAppReminderMindfulLaunchDelay =
                prefs[Keys.IN_APP_REMINDER_MINDFUL_LAUNCH_DELAY] ?: false,
            inAppReminderExpiryMode = prefs[Keys.IN_APP_REMINDER_EXPIRY_MODE]
                ?.let { runCatching { SessionExpiryMode.valueOf(it) }.getOrNull() }
                ?: SessionExpiryMode.ONLY_REMINDER,
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

    override suspend fun updateTwoFingerDoubleTapToLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TWO_FINGER_DOUBLE_TAP_TO_LOCK] = enabled }
    }

    override suspend fun updateShortcut(slot: ShortcutSlot, target: ShortcutTarget?) {
        val (componentKey, labelKey) = keysFor(slot)
        context.dataStore.edit { prefs ->
            if (target == null) {
                prefs.remove(componentKey)
                prefs.remove(labelKey)
            } else {
                prefs[componentKey] = target.componentName
                prefs[labelKey] = target.label
            }
        }
    }

    override suspend fun updateInAppReminderMasterEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IN_APP_REMINDER_MASTER] = enabled }
    }

    override suspend fun updateInAppReminderPrecisionMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IN_APP_REMINDER_PRECISION_MODE] = enabled }
    }

    override suspend fun updateInAppReminderMindfulLaunchDelay(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IN_APP_REMINDER_MINDFUL_LAUNCH_DELAY] = enabled }
    }

    override suspend fun updateInAppReminderExpiryMode(mode: SessionExpiryMode) {
        context.dataStore.edit { it[Keys.IN_APP_REMINDER_EXPIRY_MODE] = mode.name }
    }

    private fun keysFor(
        slot: ShortcutSlot,
    ): Pair<Preferences.Key<String>, Preferences.Key<String>> = when (slot) {
        ShortcutSlot.LEFT -> Keys.LEFT_SHORTCUT_COMPONENT to Keys.LEFT_SHORTCUT_LABEL
        ShortcutSlot.RIGHT -> Keys.RIGHT_SHORTCUT_COMPONENT to Keys.RIGHT_SHORTCUT_LABEL
    }

    private fun readShortcut(
        prefs: Preferences,
        componentKey: Preferences.Key<String>,
        labelKey: Preferences.Key<String>,
    ): ShortcutTarget? {
        val component = prefs[componentKey] ?: return null
        val packageName = component.substringBefore('/', missingDelimiterValue = "")
        if (packageName.isEmpty()) return null
        return ShortcutTarget(
            packageName = packageName,
            componentName = component,
            label = prefs[labelKey] ?: packageName,
        )
    }
}
