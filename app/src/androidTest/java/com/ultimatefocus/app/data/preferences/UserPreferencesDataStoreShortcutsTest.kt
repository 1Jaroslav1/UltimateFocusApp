/**
 * Integration tests for UserPreferencesDataStore shortcut persistence.
 * Verifies that the bottom-bar quick-access shortcut overrides survive
 * round-trips through the real DataStore file, including the null (reset) case
 * and persistence across a newly constructed DataStore instance.
 * Covers: ULT-13, ULT-14
 */
package com.ultimatefocus.app.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultimatefocus.app.domain.model.ShortcutSlot
import com.ultimatefocus.app.domain.model.ShortcutTarget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UserPreferencesDataStoreShortcutsTest {

    private lateinit var context: Context
    private lateinit var dataStore: UserPreferencesDataStore

    private val mapsTarget = ShortcutTarget(
        packageName = "com.google.android.apps.maps",
        componentName = "com.google.android.apps.maps/com.google.android.maps.MapsActivity",
        label = "Maps",
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "datastore/user_preferences.preferences_pb").delete()
        dataStore = UserPreferencesDataStore(context)
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    fun defaultShortcuts_areNull_onFreshStore() = runTest {
        val prefs = dataStore.preferences.first()
        assertNull(prefs.leftShortcut)
        assertNull(prefs.rightShortcut)
    }

    @Test
    fun updateShortcut_LEFT_roundTrips() = runTest {
        dataStore.updateShortcut(ShortcutSlot.LEFT, mapsTarget)
        assertEquals(mapsTarget, dataStore.preferences.first().leftShortcut)
    }

    @Test
    fun updateShortcut_RIGHT_roundTrips() = runTest {
        dataStore.updateShortcut(ShortcutSlot.RIGHT, mapsTarget)
        assertEquals(mapsTarget, dataStore.preferences.first().rightShortcut)
    }

    @Test
    fun updateShortcut_onlyAffectsTargetSlot() = runTest {
        dataStore.updateShortcut(ShortcutSlot.LEFT, mapsTarget)
        val prefs = dataStore.preferences.first()
        assertEquals(mapsTarget, prefs.leftShortcut)
        assertNull(prefs.rightShortcut)
    }

    // ── Border cases ─────────────────────────────────────────────────────────

    @Test
    fun updateShortcut_withNull_clearsPreviousOverride() = runTest {
        dataStore.updateShortcut(ShortcutSlot.LEFT, mapsTarget)
        dataStore.updateShortcut(ShortcutSlot.LEFT, null)
        assertNull(dataStore.preferences.first().leftShortcut)
    }

    @Test
    fun updateShortcut_persistsAcrossNewInstance() = runTest {
        dataStore.updateShortcut(ShortcutSlot.LEFT, mapsTarget)
        val reopened = UserPreferencesDataStore(context)
        assertEquals(mapsTarget, reopened.preferences.first().leftShortcut)
    }

    @Test
    fun lastWriteWins_forSameSlot() = runTest {
        val replacement = mapsTarget.copy(label = "Maps Beta")
        dataStore.updateShortcut(ShortcutSlot.LEFT, mapsTarget)
        dataStore.updateShortcut(ShortcutSlot.LEFT, replacement)
        assertEquals(replacement, dataStore.preferences.first().leftShortcut)
    }
}
