/**
 * Unit tests for NotificationListenerPermission.isEnabled(setting, component).
 * The Settings.Secure lookup itself is kept in a thin wrapper; this test
 * focuses on the parsing rule: colon-separated component names, no nulls,
 * exact-match per entry.
 * Covers: ULT-22
 */
package com.ultimatefocus.app.util

import android.content.ComponentName
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationListenerPermissionTest {

    private val ours = ComponentName(
        "com.ultimatefocus.app",
        "com.ultimatefocus.app.service.BlockedAppNotificationListenerService",
    )

    @Test
    fun isEnabled_returnsFalseForNullSetting() {
        assertFalse(NotificationListenerPermission.isEnabled(null, ours))
    }

    @Test
    fun isEnabled_returnsFalseForBlankSetting() {
        assertFalse(NotificationListenerPermission.isEnabled("   ", ours))
    }

    @Test
    fun isEnabled_returnsTrueWhenComponentIsOnlyEntry() {
        val setting = ours.flattenToString()
        assertTrue(NotificationListenerPermission.isEnabled(setting, ours))
    }

    @Test
    fun isEnabled_returnsTrueWhenComponentIsAmongMany() {
        val setting = "com.other.app/.ListenerService:${ours.flattenToString()}:com.third.app/.Other"
        assertTrue(NotificationListenerPermission.isEnabled(setting, ours))
    }

    @Test
    fun isEnabled_returnsFalseWhenComponentIsAbsent() {
        val setting = "com.other.app/.ListenerService:com.third.app/.Other"
        assertFalse(NotificationListenerPermission.isEnabled(setting, ours))
    }

    @Test
    fun isEnabled_trimsEachEntry() {
        val setting = "  com.other/.X  : ${ours.flattenToString()} :com.third/.Y"
        assertTrue(NotificationListenerPermission.isEnabled(setting, ours))
    }
}
