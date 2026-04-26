/**
 * Integration tests for the In-App Reminder global keys persisted via
 * UserPreferencesDataStore. Each test deletes the on-disk DataStore file
 * before exercising round-trip + default behavior so the real Preferences
 * codec (including enum string serialization) is exercised end-to-end.
 *
 * Covers: ULT-76
 */
package com.ultimatefocus.app.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.SessionExpiryMode
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
class UserPreferencesDataStoreInAppReminderTest {

    private lateinit var dataStore: UserPreferencesDataStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "datastore/user_preferences.preferences_pb").delete()
        dataStore = UserPreferencesDataStore(context)
    }

    // ── Defaults ─────────────────────────────────────────────────────────────

    @Test
    fun defaults_masterDisabled() = runTest {
        assertFalse(dataStore.preferences.first().inAppReminderMasterEnabled)
    }

    @Test
    fun defaults_precisionAndDelayDisabled_modeIsOnlyReminder() = runTest {
        val prefs = dataStore.preferences.first()
        assertFalse(prefs.inAppReminderPrecisionMode)
        assertFalse(prefs.inAppReminderMindfulLaunchDelay)
        assertEquals(SessionExpiryMode.ONLY_REMINDER, prefs.inAppReminderExpiryMode)
    }

    // ── Round-trip ───────────────────────────────────────────────────────────

    @Test
    fun updateMaster_persistsTrue() = runTest {
        dataStore.updateInAppReminderMasterEnabled(true)
        assertTrue(dataStore.preferences.first().inAppReminderMasterEnabled)
    }

    @Test
    fun updatePrecisionMode_persistsTrue() = runTest {
        dataStore.updateInAppReminderPrecisionMode(true)
        assertTrue(dataStore.preferences.first().inAppReminderPrecisionMode)
    }

    @Test
    fun updateMindfulLaunchDelay_persistsTrue() = runTest {
        dataStore.updateInAppReminderMindfulLaunchDelay(true)
        assertTrue(dataStore.preferences.first().inAppReminderMindfulLaunchDelay)
    }

    @Test
    fun updateExpiryMode_persistsEachEnumValue() = runTest {
        for (mode in SessionExpiryMode.entries) {
            dataStore.updateInAppReminderExpiryMode(mode)
            assertEquals(mode, dataStore.preferences.first().inAppReminderExpiryMode)
        }
    }

    // ── Border ───────────────────────────────────────────────────────────────

    @Test
    fun multipleUpdates_lastWriteWins() = runTest {
        dataStore.updateInAppReminderMasterEnabled(true)
        dataStore.updateInAppReminderMasterEnabled(false)
        assertFalse(dataStore.preferences.first().inAppReminderMasterEnabled)
    }

    @Test
    fun unrelatedFlagsAreIndependent() = runTest {
        dataStore.updateInAppReminderMasterEnabled(true)
        dataStore.updateInAppReminderMindfulLaunchDelay(true)
        val prefs = dataStore.preferences.first()
        assertTrue(prefs.inAppReminderMasterEnabled)
        assertTrue(prefs.inAppReminderMindfulLaunchDelay)
        assertFalse(prefs.inAppReminderPrecisionMode)
        assertEquals(SessionExpiryMode.ONLY_REMINDER, prefs.inAppReminderExpiryMode)
    }
}
