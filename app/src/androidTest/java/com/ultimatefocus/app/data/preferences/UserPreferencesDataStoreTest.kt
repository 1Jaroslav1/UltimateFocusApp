/**
 * Integration tests for UserPreferencesDataStore using a real DataStore instance.
 * Verifies default values, persistence of updates, and round-trip of
 * gesture-navigation preferences (search provider, two-finger-long-press-to-lock).
 * Covers: ULT-8, ULT-16, ULT-18
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
import org.junit.Assert.assertNull
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

    // ── ULT-16: search provider ──────────────────────────────────────────────

    @Test
    fun defaultPreferences_searchProviderPackageIsNull() = runTest {
        assertNull(dataStore.preferences.first().searchProviderPackage)
    }

    @Test
    fun updateSearchProviderPackage_persistsSelectedPackage() = runTest {
        dataStore.updateSearchProviderPackage("com.duckduckgo.mobile.android")
        assertEquals(
            "com.duckduckgo.mobile.android",
            dataStore.preferences.first().searchProviderPackage,
        )
    }

    @Test
    fun updateSearchProviderPackage_withNull_clearsStoredPackage() = runTest {
        dataStore.updateSearchProviderPackage("com.android.chrome")
        dataStore.updateSearchProviderPackage(null)
        assertNull(dataStore.preferences.first().searchProviderPackage)
    }

    // ── ULT-18: two-finger-long-press-to-lock ────────────────────────────────

    @Test
    fun defaultPreferences_twoFingerLongPressToLockIsFalse() = runTest {
        assertFalse(dataStore.preferences.first().twoFingerLongPressToLockEnabled)
    }

    @Test
    fun updateTwoFingerLongPressToLock_persistsNewValue() = runTest {
        dataStore.updateTwoFingerLongPressToLock(true)
        assertTrue(dataStore.preferences.first().twoFingerLongPressToLockEnabled)
    }

    @Test
    fun updateTwoFingerLongPressToLock_toFalse_clearsPreviousValue() = runTest {
        dataStore.updateTwoFingerLongPressToLock(true)
        dataStore.updateTwoFingerLongPressToLock(false)
        assertFalse(dataStore.preferences.first().twoFingerLongPressToLockEnabled)
    }
}
