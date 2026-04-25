/**
 * Unit tests for AccessibilityPermission.isEnabled(setting, component).
 * Mirrors NotificationListenerPermissionTest — the parsing rule is the same
 * (colon-separated flattened component names).
 * Covers: ULT-20, ULT-21
 */
package com.ultimatefocus.app.util

import android.content.ComponentName
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityPermissionTest {

    private val ours = ComponentName(
        "com.ultimatefocus.app",
        "com.ultimatefocus.app.service.BlockedAppAccessibilityService",
    )

    @Test
    fun isEnabled_returnsFalseForNullSetting() {
        assertFalse(AccessibilityPermission.isEnabled(null, ours))
    }

    @Test
    fun isEnabled_returnsFalseForBlankSetting() {
        assertFalse(AccessibilityPermission.isEnabled("   ", ours))
    }

    @Test
    fun isEnabled_returnsTrueWhenComponentIsOnlyEntry() {
        assertTrue(AccessibilityPermission.isEnabled(ours.flattenToString(), ours))
    }

    @Test
    fun isEnabled_returnsTrueWhenComponentIsAmongMany() {
        val setting = "com.other.app/.ListenerService:${ours.flattenToString()}:com.third.app/.Other"
        assertTrue(AccessibilityPermission.isEnabled(setting, ours))
    }

    @Test
    fun isEnabled_returnsFalseWhenComponentIsAbsent() {
        val setting = "com.other.app/.SomeService:com.third.app/.Other"
        assertFalse(AccessibilityPermission.isEnabled(setting, ours))
    }

    @Test
    fun isEnabled_trimsEachEntry() {
        val setting = "  com.other/.X  : ${ours.flattenToString()} :com.third/.Y"
        assertTrue(AccessibilityPermission.isEnabled(setting, ours))
    }
}
