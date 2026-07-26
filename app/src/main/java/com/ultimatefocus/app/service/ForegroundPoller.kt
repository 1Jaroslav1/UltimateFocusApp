/**
 * Accessibility-free foreground-package watcher — the fallback enforcement
 * path for both app blocking and session pause/resume (ULT-86).
 *
 * BlockedAppAccessibilityService is the primary path: it reacts immediately
 * to TYPE_WINDOW_STATE_CHANGED. But it has two failure modes that together
 * explain the reported "blocked apps are still usable" and "the timer closed
 * the wrong app" bugs:
 *
 *  1. Aggressive OEM battery management (Samsung's Device Care especially)
 *     suspends or kills bound accessibility services in the background. When
 *     that happens the service stops delivering events entirely, so nothing
 *     re-shows the block screen and nothing pauses a running session — while
 *     SessionExpiryRunner, which is deliberately process-lifetime and
 *     accessibility-independent, keeps counting down and evicts whatever app
 *     is in front when it fires.
 *  2. Resuming an existing task from the recents/"active applications"
 *     switcher does not reliably emit TYPE_WINDOW_STATE_CHANGED across OEMs
 *     — the window regains focus rather than a new window being added. Since
 *     BlockedAppActivity is excludeFromRecents and its dismissal only routes
 *     Home, the blocked app's own task survives underneath and is resumed
 *     fully interactive with no event to trigger a re-block.
 *
 * This poller samples the current foreground package roughly once a second
 * via UsageStatsManager (the PACKAGE_USAGE_STATS permission already used for
 * weekly stats, ULT-24 — no accessibility service involved) and feeds it
 * through the exact same BlockedAppForegroundHandler decision rule, so both
 * paths enforce identically and dedupe against each other via
 * BlockedScreenState. Started from UltimateFocusApp.onCreate so it shares
 * SessionExpiryRunner's process lifetime.
 *
 * The loop is level-triggered, not edge-triggered: every tick re-evaluates
 * the live foreground package from scratch. A tick that is skipped or whose
 * activity launch fails simply self-heals on the next tick, so there is no
 * window in which a blocked app stays usable.
 *
 * Activity launches mirror SessionExpiryRunner's proven sequence — goHome()
 * first (the HOME intent is one of the few startActivity calls permitted
 * from a background app context on API 29+), a brief handoff delay, then the
 * real launch while the launcher counts as foreground. For block enforcement
 * that ordering is desirable anyway: the user should be ejected from the
 * blocked app regardless of whether the screen itself renders.
 *
 * Covers: ULT-86
 */
package com.ultimatefocus.app.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.ultimatefocus.app.domain.repository.SessionTimerRepository
import com.ultimatefocus.app.domain.service.BlockedAppForegroundHandler
import com.ultimatefocus.app.domain.service.BlockedScreenState
import com.ultimatefocus.app.domain.service.BlockingSourceCoordinator
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.domain.service.ForegroundAction
import com.ultimatefocus.app.domain.service.SessionTimerCoordinator
import com.ultimatefocus.app.domain.service.SystemActions
import com.ultimatefocus.app.ui.sessiontimer.SessionDurationPickerActivity
import com.ultimatefocus.app.util.UsageStatsPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForegroundPoller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionTimerCoordinator: SessionTimerCoordinator,
    private val sessionTimerRepository: SessionTimerRepository,
    private val blockingSourceCoordinator: BlockingSourceCoordinator,
    private val foregroundHandler: BlockedAppForegroundHandler,
    private val blockedScreenState: BlockedScreenState,
    private val blockedAppIntentFactory: BlockedAppIntentFactory,
    private val systemActions: SystemActions,
    private val clock: Clock,
) {

    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var started = false

    @Volatile
    private var activeBlocked: Set<String> = emptySet()

    @Volatile
    private var reminderEnabled: Set<String> = emptySet()

    fun start() {
        if (started) return
        started = true
        blockingSourceCoordinator.observeActivePackageNames()
            .onEach { activeBlocked = it }
            .launchIn(scope)
        sessionTimerRepository.observeEnabledPackages()
            .onEach { reminderEnabled = it }
            .launchIn(scope)
        scope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                runCatching { pollOnce() }
                    .onFailure { Log.w(TAG, "poll failed", it) }
            }
        }
    }

    private suspend fun pollOnce() {
        val pkg = currentForegroundPackage() ?: return
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
            ownPackage = context.packageName,
            reminderEnabled = reminderEnabled,
            sessionActiveFor = sessionActiveFor,
            blockScreenVisiblePackage = blockedScreenState.visiblePackage,
        )
        when (decision) {
            is ForegroundAction.ShowBlocked -> {
                Log.d(TAG, "poller re-blocking pkg=$pkg")
                val intent = blockedAppIntentFactory.intentFor(context, pkg) ?: return
                systemActions.goHome()
                delay(LAUNCH_HANDOFF_DELAY_MS)
                runCatching { context.startActivity(intent) }
            }
            is ForegroundAction.RequireSessionDuration -> {
                Log.d(TAG, "poller routing to duration picker pkg=$pkg")
                val defaultMode = sessionTimerRepository.observeSettings().first().expiryMode
                val intent = SessionDurationPickerActivity.intent(
                    context = context,
                    packageName = pkg,
                    appLabel = blockedAppIntentFactory.appLabel(pkg),
                    defaultMode = defaultMode,
                )
                systemActions.goHome()
                delay(LAUNCH_HANDOFF_DELAY_MS)
                runCatching { context.startActivity(intent) }
            }
            ForegroundAction.Ignore -> Unit
        }
    }

    /**
     * The most recent MOVE_TO_FOREGROUND event in the trailing
     * [LOOKBACK_MS] window — the standard way to read "what is in front
     * right now" without an accessibility service. Returns null when usage
     * access has not been granted or the query fails, in which case the
     * tick is skipped and the accessibility path (when alive) still covers
     * enforcement on its own.
     */
    private fun currentForegroundPackage(): String? {
        val manager = usageStatsManager ?: return null
        if (!UsageStatsPermission.isGranted(context)) return null
        val end = System.currentTimeMillis()
        val events = runCatching {
            manager.queryEvents(end - LOOKBACK_MS, end)
        }.getOrNull() ?: return null
        var lastForegroundPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundPackage = event.packageName
            }
        }
        return lastForegroundPackage
    }

    private companion object {
        const val TAG = "ForegroundPoller"
        const val POLL_INTERVAL_MS: Long = 1_000L

        /**
         * Matches SessionExpiryRunner.OVERLAY_HANDOFF_DELAY_MS — long enough
         * for the HOME intent to make the launcher foreground so the second
         * startActivity is permitted, short enough to be imperceptible.
         */
        const val LAUNCH_HANDOFF_DELAY_MS: Long = 300L

        /**
         * Usage events can lag the actual transition by a few hundred ms, so
         * the lookback window is generously wider than the poll interval.
         */
        const val LOOKBACK_MS: Long = 10_000L
    }
}
