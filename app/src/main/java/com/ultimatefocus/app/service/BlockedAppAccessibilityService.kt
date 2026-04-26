/**
 * System-layer enforcement of app blocks. Subscribes to window-state-changed
 * events so whenever a blocked package comes to the foreground via ANY path
 * (Settings, recents, a notification tap, another app's intent, Play Store)
 * the user is redirected to BlockedAppActivity on top.
 *
 * Reads the "currently blocked packages" set from BlockingSourceCoordinator
 * which unifies manual blocks (ULT-20), recurring schedules (ULT-24), and
 * tag-derived blocks (ULT-75) into a single Set<String>. The hot-path
 * event handler checks an in-memory copy to avoid a DB hit per window
 * transition.
 *
 * Debounce + system-UI / own-package exclusion live in
 * BlockedAppForegroundHandler so they can be unit-tested without booting the
 * service lifecycle.
 * Covers: ULT-20, ULT-21, ULT-24, ULT-75
 */
package com.ultimatefocus.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.ultimatefocus.app.domain.model.ActiveBlockInfo
import com.ultimatefocus.app.domain.service.BlockedAppForegroundHandler
import com.ultimatefocus.app.domain.service.BlockingSourceCoordinator
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.domain.service.ForegroundAction
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BlockedAppAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var coordinator: BlockingSourceCoordinator

    @Inject
    lateinit var foregroundHandler: BlockedAppForegroundHandler

    @Inject
    lateinit var clock: Clock

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    private var activeBlocked: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "connected")
        coordinator.observeActivePackageNames()
            .onEach {
                activeBlocked = it
                Log.d(TAG, "activeBlocked updated: $it")
            }
            .launchIn(scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val decision = foregroundHandler.onForegroundPackage(
            packageName = pkg,
            nowMs = clock.now(),
            activeBlocked = activeBlocked,
            ownPackage = packageName,
        )
        if (decision is ForegroundAction.ShowBlocked) {
            Log.d(TAG, "redirecting blocked foreground pkg=$pkg")
            scope.launch { showReminder(pkg) }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun showReminder(pkg: String) {
        val info: ActiveBlockInfo = coordinator.activeBlockInfo(pkg) ?: return
        val label = runCatching {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
        val intent = when (info) {
            is ActiveBlockInfo.Manual -> BlockedAppActivity.intent(
                context = this,
                packageName = pkg,
                appLabel = label,
                blockedUntilEpochMs = info.blockedUntilEpochMs,
                durationLabel = info.durationLabel,
                scheduleName = null,
            )
            is ActiveBlockInfo.Scheduled -> BlockedAppActivity.intent(
                context = this,
                packageName = pkg,
                appLabel = label,
                blockedUntilEpochMs = info.blockedUntilEpochMs,
                durationLabel = "",
                scheduleName = info.scheduleName,
            )
            is ActiveBlockInfo.Tag -> BlockedAppActivity.intent(
                context = this,
                packageName = pkg,
                appLabel = label,
                blockedUntilEpochMs = info.blockedUntilEpochMs,
                durationLabel = info.durationLabel,
                scheduleName = null,
            )
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "BlockedAppA11y"
    }
}
