/**
 * Helpers for checking whether BlockedAppAccessibilityService is enabled and
 * for deep-linking the user to the system Accessibility settings where the
 * toggle lives. Parsing is factored into isEnabled(...) so tests don't need
 * to mock Settings.Secure.
 * Covers: ULT-20, ULT-21
 */
package com.ultimatefocus.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.ultimatefocus.app.service.BlockedAppAccessibilityService

object AccessibilityPermission {

    fun isGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        return isEnabled(
            enabled,
            ComponentName(context, BlockedAppAccessibilityService::class.java),
        )
    }

    fun isEnabled(enabledServicesSetting: String?, component: ComponentName): Boolean {
        if (enabledServicesSetting.isNullOrBlank()) return false
        val target = component.flattenToString()
        return enabledServicesSetting.split(':').any { it.trim() == target }
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
