/**
 * System-layer enforcement of app blocks AND of the In-App Time Reminder
 * launch gate AND of session-expiry effect dispatch. Subscribes to
 * window-state-changed events so whenever a blocked package comes to the
 * foreground via ANY path (Settings, recents, a notification tap, another
 * app's intent, Play Store) the user is redirected to BlockedAppActivity
 * on top; whenever a reminder-enabled package comes foreground without an
 * in-flight session, the user is routed to the SessionDurationPickerActivity
 * instead (ULT-78). Also subscribes to SessionExpiryWatcher so when an
 * active session reaches zero the configured behavior mode (ULT-81)
 * dispatches from THIS service rather than the Application — Android
 * blocks background activity launches from app-context coroutines on
 * API 29+, but AccessibilityService context launches are allowed.
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
 * Covers: ULT-20, ULT-21, ULT-24, ULT-75, ULT-78, ULT-81
 */
package com.ultimatefocus.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.ultimatefocus.app.domain.model.ActiveBlockInfo
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import com.ultimatefocus.app.domain.repository.SessionTimerRepository
import com.ultimatefocus.app.domain.service.BlockedAppForegroundHandler
import com.ultimatefocus.app.domain.service.BlockingSourceCoordinator
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.domain.service.ForegroundAction
import com.ultimatefocus.app.domain.service.SessionExpiryEffect
import com.ultimatefocus.app.domain.service.SessionExpiryWatcher
import com.ultimatefocus.app.domain.service.SessionTimerCoordinator
import com.ultimatefocus.app.domain.service.SystemActions
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity
import com.ultimatefocus.app.ui.sessiontimer.SessionDurationPickerActivity
import com.ultimatefocus.app.ui.sessiontimer.SessionExpiredOverlayActivity
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

    @Inject
    lateinit var sessionTimerRepository: SessionTimerRepository

    @Inject
    lateinit var sessionTimerCoordinator: SessionTimerCoordinator

    @Inject
    lateinit var sessionExpiryWatcher: SessionExpiryWatcher

    @Inject
    lateinit var systemActions: SystemActions

    @Inject
    lateinit var blockedAppRepository: BlockedAppRepository

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

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
        // ULT-81: dispatch session-expiry effects from THIS service so the
        // overlay activity launch (LaunchOverlayCountdown branch) is
        // allowed even when the launcher is in the background — Android 10+
        // forbids that from app-context coroutines.
        sessionExpiryWatcher.expiryEffects()
            .onEach { effect -> dispatchExpiryEffect(effect) }
            .launchIn(scope)
    }

    private fun dispatchExpiryEffect(effect: SessionExpiryEffect) {
        when (effect) {
            is SessionExpiryEffect.LaunchOverlayCountdown -> {
                val intent = SessionExpiredOverlayActivity.intent(
                    context = this,
                    packageName = effect.packageName,
                    appLabel = effect.appLabel,
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                runCatching { startActivity(intent) }
                // The overlay owns session lifecycle (extend re-starts it,
                // cancel ends it), so we deliberately do NOT clear the
                // session here.
            }
            is SessionExpiryEffect.GoHomeOnly -> {
                systemActions.goHome()
                sessionTimerCoordinator.endSession()
            }
            is SessionExpiryEffect.GoHomeAndBlock -> {
                systemActions.goHome()
                scope.launch {
                    blockedAppRepository.blockForMillis(
                        packageName = effect.packageName,
                        durationMillis = effect.blockDurationMinutes.toLong() * 60_000L,
                        label = "${effect.blockDurationMinutes}m",
                    )
                    sessionTimerCoordinator.endSession()
                }
            }
            is SessionExpiryEffect.ShowReminderOnly -> {
                val text = "Session ended for ${effect.appLabel.ifBlank { "this app" }}"
                mainHandler.post {
                    Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
                }
                sessionTimerCoordinator.endSession()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val sessionActiveFor = sessionTimerCoordinator.active.value
            ?.takeIf { it.packageName == pkg && clock.now() < it.endsEpochMs }
            ?.let { setOf(it.packageName) } ?: emptySet()
        val decision = foregroundHandler.onForegroundPackage(
            packageName = pkg,
            nowMs = clock.now(),
            activeBlocked = activeBlocked,
            ownPackage = packageName,
            reminderEnabled = reminderEnabled,
            sessionActiveFor = sessionActiveFor,
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

    private fun showDurationPicker(pkg: String) {
        val label = runCatching {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
        val intent = SessionDurationPickerActivity.intent(
            context = this,
            packageName = pkg,
            appLabel = label,
            defaultMode = defaultExpiryMode,
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "BlockedAppA11y"
    }
}
