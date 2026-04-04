/**
 * Integration tests for UserPreferencesDataStore using a real DataStore instance
 * backed by a temporary test context. Verifies default values, persistence of
 * updates, and correct reading of the autoKeyboard flag.
 * Covers: ULT-8
 */
package com.minimalist.launcher.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minimalist.launcher.domain.model.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferencesDataStoreTest {

    private lateinit var context: Context
    private lateinit var dataStore: UserPreferencesDataStore

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        // Clear any persisted state before each test
        clearDataStore(context)
        dataStore = UserPreferencesDataStore(context)
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun defaultPreferences_darkThemeIsTrue() = runTest {
        val prefs = dataStore.preferences.first()
        assertTrue(prefs.isDarkTheme)
    }

    @Test
    fun defaultPreferences_autoKeyboardIsTrue() = runTest {
        val prefs = dataStore.preferences.first()
        assertTrue(prefs.autoKeyboard)
    }

    @Test
    fun updateDarkTheme_persistsNewValue() = runTest {
        dataStore.updateDarkTheme(false)
        val prefs = dataStore.preferences.first()
        assertFalse(prefs.isDarkTheme)
    }

    @Test
    fun updateAutoKeyboard_toFalse_persistsNewValue() = runTest {
        dataStore.updateAutoKeyboard(false)
        val prefs = dataStore.preferences.first()
        assertFalse(prefs.autoKeyboard)
    }

    @Test
    fun updateAutoKeyboard_toTrue_persistsNewValue() = runTest {
        dataStore.updateAutoKeyboard(false) // set to false first
        dataStore.updateAutoKeyboard(true)  // then back to true
        val prefs = dataStore.preferences.first()
        assertTrue(prefs.autoKeyboard)
    }

    @Test
    fun multipleUpdates_lastWriteWins() = runTest {
        dataStore.updateDarkTheme(false)
        dataStore.updateDarkTheme(true)
        val prefs = dataStore.preferences.first()
        assertTrue(prefs.isDarkTheme)
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun preferences_returnsDefaultsForAllFields() = runTest {
        val prefs = dataStore.preferences.first()
        val expected = UserPreferences()
        assertTrue(prefs.isDarkTheme == expected.isDarkTheme)
        assertTrue(prefs.fontSize == expected.fontSize)
        assertTrue(prefs.homeAlignment == expected.homeAlignment)
        assertTrue(prefs.autoKeyboard == expected.autoKeyboard)
    }

    @Test
    fun updateAutoKeyboard_doesNotAffectOtherPreferences() = runTest {
        dataStore.updateDarkTheme(false)
        dataStore.updateAutoKeyboard(false)
        val prefs = dataStore.preferences.first()
        assertFalse(prefs.isDarkTheme)  // dark theme still false
        assertFalse(prefs.autoKeyboard) // auto keyboard also false
    }

    private companion object {
        private val Context.testDataStore by preferencesDataStore("user_preferences")
        suspend fun clearDataStore(context: Context) {
            context.testDataStore.edit { it.clear() }
        }
    }
}
