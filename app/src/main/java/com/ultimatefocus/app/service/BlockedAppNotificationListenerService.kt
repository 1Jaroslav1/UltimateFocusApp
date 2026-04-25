/**
 * NotificationListenerService that silently cancels any notification posted
 * by a package with an active block. Runs in the background whenever the
 * user grants notification-listener access in Settings.
 * The suppression decision is delegated to BlockedAppChecker so the rule
 * can be unit-tested without booting a system-bound service.
 * Covers: ULT-22
 */
package com.ultimatefocus.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ultimatefocus.app.domain.service.BlockedAppChecker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BlockedAppNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var checker: BlockedAppChecker

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val pkg = notification.packageName ?: return
        // Defensive: never suppress our own notifications.
        if (pkg == packageName) return
        scope.launch {
            if (checker.shouldSuppress(pkg)) {
                cancelNotification(notification.key)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
