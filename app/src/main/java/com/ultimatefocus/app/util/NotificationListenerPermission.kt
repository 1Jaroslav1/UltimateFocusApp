/**
 * Helpers for checking whether the user has granted notification-listener
 * access to this launcher and for deep-linking them into the system
 * Settings screen where the toggle lives.
 * isEnabled(enabledListenersSetting, component) is factored out so the
 * parsing logic is unit-testable without mocking Settings.Secure.
 * Covers: ULT-22
 */
package com.ultimatefocus.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.ultimatefocus.app.service.BlockedAppNotificationListenerService

object NotificationListenerPermission {

    fun isGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        )
        return isEnabled(
            enabled,
            ComponentName(context, BlockedAppNotificationListenerService::class.java),
        )
    }

    fun isEnabled(enabledListenersSetting: String?, component: ComponentName): Boolean {
        if (enabledListenersSetting.isNullOrBlank()) return false
        val target = component.flattenToString()
        return enabledListenersSetting.split(':').any { it.trim() == target }
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
