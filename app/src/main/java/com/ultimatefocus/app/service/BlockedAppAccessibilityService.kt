/**
 * System-layer enforcement of app blocks AND of the In-App Time Reminder
 * launch gate. Subscribes to window-state-changed events so whenever a
 * blocked package comes to the foreground via ANY path (Settings, recents,
 * a notification tap, another app's intent, Play Store) the user is
 * redirected to BlockedAppActivity on top; whenever a reminder-enabled
 * package comes foreground without an in-flight session, the user is
 * routed to the SessionDurationPickerActivity instead (ULT-78).
 *
 * Reads the "currently blocked packages" set from BlockingSourceCoordinator
 * which unifies manual blocks (ULT-20), recurring schedules (ULT-24), and
 * tag-derived blocks (ULT-75); reads the reminder-enabled set from
 * SessionTimerRepository (ULT-77). Both sets are cached in memory so the
 * hot-path event handler avoids DB hits per window transition.
 *
 * Debounce + system-UI / own-package exclusion live in
 * BlockedAppForegroundHandler so they can be unit-tested without booting the
 * service lifecycle.
 *
 * Note: session-expiry side-effect dispatch (ULT-81) is owned by
 * SessionExpiryRunner started from UltimateFocusApp.onCreate, NOT by
 * this service. That keeps mode dispatch process-lifetime so a killed
 * or disabled accessibility service does not leave the user with a
 * silently-failing timer.
 *
 * ULT-86: this service is the FAST path, not the only one. It goes silent
 * whenever the OS suspends it (common under OEM battery management) and it
 * does not reliably observe a task being resumed from the recents switcher.
 * ForegroundPoller polls UsageStatsManager as an accessibility-free fallback
 * running the same BlockedAppForegroundHandler rule; the two dedupe against
 * each other through BlockedScreenState and share intent construction via
 * BlockedAppIntentFactory.
 *
 * Covers: ULT-20, ULT-21, ULT-24, ULT-75, ULT-78, ULT-86
 */
package com.ultimatefocus.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.repository.SessionTimerRepository
import com.ultimatefocus.app.domain.service.BlockedAppForegroundHandler
import com.ultimatefocus.app.domain.service.BlockedScreenState
import com.ultimatefocus.app.domain.service.BlockingSourceCoordinator
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.domain.service.ForegroundAction
import com.ultimatefocus.app.domain.service.SessionTimerCoordinator
import com.ultimatefocus.app.ui.sessiontimer.SessionDurationPickerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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

    @Inject
    lateinit var sessionTimerRepository: SessionTimerRepository

    @Inject
    lateinit var sessionTimerCoordinator: SessionTimerCoordinator

    @Inject
    lateinit var blockedScreenState: BlockedScreenState

    @Inject
    lateinit var blockedAppIntentFactory: BlockedAppIntentFactory

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    private var activeBlocked: Set<String> = emptySet()

    @Volatile
    private var reminderEnabled: Set<String> = emptySet()

    @Volatile
    private var defaultExpiryMode: SessionExpiryMode = SessionExpiryMode.ONLY_REMINDER

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "connected")
        coordinator.observeActivePackageNames()
            .onEach {
                activeBlocked = it
                Log.d(TAG, "activeBlocked updated: $it")
            }
            .launchIn(scope)
        sessionTimerRepository.observeEnabledPackages()
            .onEach {
                reminderEnabled = it
                Log.d(TAG, "reminderEnabled updated: $it")
            }
            .launchIn(scope)
        sessionTimerRepository.observeSettings()
            .onEach { defaultExpiryMode = it.expiryMode }
            .launchIn(scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        // Undebounced pause/resume accounting for the session timer (ULT-86)
        // — kept independent of the debounced decision below since active-
        // time accounting needs the raw per-event foreground signal.
        sessionTimerCoordinator.onForegroundPackageChanged(pkg, clock.now())
        val sessionActiveFor = if (sessionTimerCoordinator.isSessionActiveFor(pkg)) {
            setOf(pkg)
        } else {
            emptySet()
        }
        val decision = foregroundHandler.onForegroundPackage(
            packageName = pkg,
            nowMs = clock.now(),
            activeBlocked = activeBlocked,
            ownPackage = packageName,
            reminderEnabled = reminderEnabled,
            sessionActiveFor = sessionActiveFor,
            blockScreenVisiblePackage = blockedScreenState.visiblePackage,
        )
        when (decision) {
            is ForegroundAction.ShowBlocked -> {
                Log.d(TAG, "redirecting blocked foreground pkg=$pkg")
                scope.launch { showReminder(pkg) }
            }
            is ForegroundAction.RequireSessionDuration -> {
                Log.d(TAG, "routing to duration picker pkg=$pkg")
                scope.launch { showDurationPicker(pkg) }
            }
            ForegroundAction.Ignore -> Unit
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun showReminder(pkg: String) {
        // Launched from the service's own context so the background-activity
        // -launch privilege on API 29+ applies. Intent construction is shared
        // with ForegroundPoller's fallback path so the two cannot drift.
        val intent = blockedAppIntentFactory.intentFor(launchContext = this, packageName = pkg)
            ?: return
        startActivity(intent)
    }

    private suspend fun showDurationPicker(pkg: String) {
        val label = blockedAppIntentFactory.appLabel(pkg)
        // Read the latest default mode from the repository here rather than
        // the cached @Volatile field — guarantees we never use a stale
        // value during the brief window between settings change and the
        // Flow's next emission.
        val freshDefault = sessionTimerRepository.observeSettings().first().expiryMode
        val intent = SessionDurationPickerActivity.intent(
            context = this,
            packageName = pkg,
            appLabel = label,
            defaultMode = freshDefault,
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "BlockedAppA11y"
    }
}
