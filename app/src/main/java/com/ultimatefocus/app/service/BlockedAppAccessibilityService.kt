/**
 * System-layer enforcement of app blocks. Subscribes to window-state-changed
 * events so whenever a blocked package comes to the foreground via ANY path
 * (Settings, recents, a notification tap, another app's intent, Play Store)
 * the user is redirected to BlockedAppActivity on top.
 *
 * The active-blocked set is kept in memory via a hot subscription to
 * BlockedAppRepository.observeActivePackageNames() so the hot-path event
 * handler avoids a DB hit per window transition.
 *
 * Debounce + system-UI / own-package exclusion live in
 * BlockedAppForegroundHandler so they can be unit-tested without booting the
 * service lifecycle.
 * Covers: ULT-20, ULT-21
 */
package com.ultimatefocus.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.ultimatefocus.app.domain.model.BlockedApp
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import com.ultimatefocus.app.domain.service.BlockedAppForegroundHandler
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
    lateinit var blockedAppRepository: BlockedAppRepository

    @Inject
    lateinit var foregroundHandler: BlockedAppForegroundHandler

    @Inject
    lateinit var clock: Clock

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    private var activeBlocked: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        blockedAppRepository.observeActivePackageNames()
            .onEach { activeBlocked = it }
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
            scope.launch { showReminder(pkg) }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun showReminder(pkg: String) {
        val block: BlockedApp = blockedAppRepository.getBlock(pkg) ?: return
        val label = runCatching {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
        val intent = BlockedAppActivity.intent(
            context = this,
            packageName = pkg,
            appLabel = label,
            blockedUntilEpochMs = block.blockedUntilEpochMs,
            durationLabel = block.durationLabel,
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
}
