/**
 * Process-lifetime consumer that turns SessionExpiryEffect events into
 * concrete Android side effects:
 *  - LaunchOverlayCountdown → goHome() FIRST (HOME intent has the
 *    background-launch exception → brings the launcher to foreground),
 *    brief delay, then startActivity(SessionExpiredOverlayActivity).
 *    With the launcher momentarily foreground the second startActivity
 *    is allowed even on API 29+. If the second launch is still rejected
 *    on some OEM the user lands on the home screen and re-tap will
 *    correctly re-trigger the picker (the session was already cleared).
 *  - GoHomeOnly             → SystemActions.goHome().
 *  - GoHomeAndBlock         → SystemActions.goHome() + BlockedAppRepository
 *                             .blockForMillis() with the original session
 *                             duration label. Closes the loop into the
 *                             existing manual-block pipeline.
 *  - ShowReminderOnly       → Toast — copy is "You spent N min in <App>"
 *                             so the popup carries the AC's "how much
 *                             time was spent" information.
 *
 * Started from UltimateFocusApp.onCreate so dispatch is process-lifetime
 * — independent of whether the AccessibilityService is currently
 * connected. (We previously moved this into the AccessibilityService to
 * gain background-activity-launch privileges, but that introduced a
 * regression where modes failed to fire if accessibility was off /
 * killed; the goHome-then-startActivity sequencing in the overlay
 * branch restores Mode 1 without that dependency.)
 *
 * Covers: ULT-81
 */
package com.ultimatefocus.app.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import com.ultimatefocus.app.domain.service.SessionExpiryEffect
import com.ultimatefocus.app.domain.service.SessionExpiryWatcher
import com.ultimatefocus.app.domain.service.SessionTimerCoordinator
import com.ultimatefocus.app.domain.service.SystemActions
import com.ultimatefocus.app.ui.sessiontimer.SessionExpiredOverlayActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionExpiryRunner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val watcher: SessionExpiryWatcher,
    private val coordinator: SessionTimerCoordinator,
    private val systemActions: SystemActions,
    private val blockedAppRepository: BlockedAppRepository,
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var started = false

    fun start() {
        if (started) return
        started = true
        watcher.expiryEffects()
            .onEach { effect -> scope.launch { dispatchExpiryEffect(effect) } }
            .launchIn(scope)
    }

    private suspend fun dispatchExpiryEffect(effect: SessionExpiryEffect) {
        when (effect) {
            is SessionExpiryEffect.LaunchOverlayCountdown -> {
                // Bring the launcher foreground via the HOME intent (which
                // is one of the few startActivity calls allowed from a
                // background app context) so the next startActivity for
                // the overlay can succeed.
                systemActions.goHome()
                delay(OVERLAY_HANDOFF_DELAY_MS)
                val intent = SessionExpiredOverlayActivity.intent(
                    context = context,
                    packageName = effect.packageName,
                    appLabel = effect.appLabel,
                )
                runCatching { context.startActivity(intent) }
                // The overlay clears the session in its bind() — leave
                // coordinator state alone here.
            }
            is SessionExpiryEffect.GoHomeOnly -> {
                systemActions.goHome()
                coordinator.endSession()
            }
            is SessionExpiryEffect.GoHomeAndBlock -> {
                systemActions.goHome()
                blockedAppRepository.blockForMillis(
                    packageName = effect.packageName,
                    durationMillis = effect.blockDurationMinutes.toLong() * 60_000L,
                    label = "${effect.blockDurationMinutes}m",
                )
                coordinator.endSession()
            }
            is SessionExpiryEffect.ShowReminderOnly -> {
                val target = effect.appLabel.ifBlank { "this app" }
                val text = "You spent ${effect.minutesSpent} min in $target"
                mainHandler.post {
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                }
                coordinator.endSession()
            }
        }
    }

    private companion object {
        /**
         * Brief gap between firing the HOME intent and the overlay's
         * startActivity. 300ms gives Android time to process the home
         * intent so the launcher counts as foreground for the second
         * launch — anecdotally enough on modern devices, low enough that
         * the user doesn't see a flicker.
         */
        const val OVERLAY_HANDOFF_DELAY_MS: Long = 300L
    }
}
