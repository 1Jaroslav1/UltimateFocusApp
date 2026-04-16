/**
 * Integration tests for UserPreferencesDataStore using a real DataStore instance.
 * Verifies default values, persistence of updates, and correct reading
 * of the autoKeyboard flag.
 * Covers: ULT-8
 */
package com.ultimatefocus.app.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UserPreferencesDataStoreTest {

    private lateinit var dataStore: UserPreferencesDataStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Delete the DataStore file before each test to ensure clean state
        File(context.filesDir, "datastore/user_preferences.preferences_pb").delete()
        dataStore = UserPreferencesDataStore(context)
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun defaultPreferences_darkThemeIsTrue() = runTest {
        assertTrue(dataStore.preferences.first().isDarkTheme)
    }

    @Test
    fun defaultPreferences_autoKeyboardIsTrue() = runTest {
        assertTrue(dataStore.preferences.first().autoKeyboard)
    }

    @Test
    fun updateDarkTheme_persistsNewValue() = runTest {
        dataStore.updateDarkTheme(false)
        assertFalse(dataStore.preferences.first().isDarkTheme)
    }

    @Test
    fun updateAutoKeyboard_toFalse_persistsNewValue() = runTest {
        dataStore.updateAutoKeyboard(false)
        assertFalse(dataStore.preferences.first().autoKeyboard)
    }

    @Test
    fun updateAutoKeyboard_toTrue_persistsNewValue() = runTest {
        dataStore.updateAutoKeyboard(false)
        dataStore.updateAutoKeyboard(true)
        assertTrue(dataStore.preferences.first().autoKeyboard)
    }

    @Test
    fun multipleUpdates_lastWriteWins() = runTest {
        dataStore.updateDarkTheme(false)
        dataStore.updateDarkTheme(true)
        assertTrue(dataStore.preferences.first().isDarkTheme)
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun preferences_returnsDefaultsForAllFields() = runTest {
        val prefs = dataStore.preferences.first()
        val expected = UserPreferences()
        assertEquals(expected.isDarkTheme, prefs.isDarkTheme)
        assertEquals(expected.fontSize, prefs.fontSize)
        assertEquals(expected.homeAlignment, prefs.homeAlignment)
        assertEquals(expected.autoKeyboard, prefs.autoKeyboard)
    }

    @Test
    fun updateAutoKeyboard_doesNotAffectOtherPreferences() = runTest {
        dataStore.updateDarkTheme(false)
        dataStore.updateAutoKeyboard(false)
        val prefs = dataStore.preferences.first()
        assertFalse(prefs.isDarkTheme)
        assertFalse(prefs.autoKeyboard)
    }
}
