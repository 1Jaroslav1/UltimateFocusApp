/**
 * Process-lifetime consumer that turns SessionExpiryEffect events into
 * concrete Android side effects:
 *  - LaunchOverlayCountdown → start SessionExpiredOverlayActivity.
 *  - GoHomeOnly             → SystemActions.goHome().
 *  - GoHomeAndBlock         → SystemActions.goHome() + block via
 *                             BlockedAppRepository.blockForMillis() with
 *                             the original session duration label.
 *  - ShowReminderOnly       → Toast (system notifications could replace
 *                             this later — Toast keeps zero permission
 *                             dependencies for the MVP).
 *
 * After dispatching, the coordinator's active session is cleared so the
 * launch interceptor will ask the user to commit a fresh duration on
 * the next launch (and the warning state machine releases its latch).
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

    fun start() {
        watcher.expiryEffects()
            .onEach { effect ->
                when (effect) {
                    is SessionExpiryEffect.LaunchOverlayCountdown -> launchOverlay(effect)
                    is SessionExpiryEffect.GoHomeOnly -> systemActions.goHome()
                    is SessionExpiryEffect.GoHomeAndBlock -> goHomeAndBlock(effect)
                    is SessionExpiryEffect.ShowReminderOnly -> showToast(
                        "Session ended for ${effect.appLabel.ifBlank { "this app" }}",
                    )
                }
                // Clear the session so the next launch attempt re-prompts.
                // The overlay activity owns its own session lifecycle (it can
                // re-start a session when the user extends), so for the
                // overlay path we do NOT clear here.
                if (effect !is SessionExpiryEffect.LaunchOverlayCountdown) {
                    coordinator.endSession()
                }
            }
            .launchIn(scope)
    }

    private fun launchOverlay(effect: SessionExpiryEffect.LaunchOverlayCountdown) {
        val intent = SessionExpiredOverlayActivity.intent(
            context = context,
            packageName = effect.packageName,
            appLabel = effect.appLabel,
        )
        runCatching { context.startActivity(intent) }
    }

    private fun goHomeAndBlock(effect: SessionExpiryEffect.GoHomeAndBlock) {
        systemActions.goHome()
        scope.launch {
            blockedAppRepository.blockForMillis(
                packageName = effect.packageName,
                durationMillis = effect.blockDurationMinutes.toLong() * 60_000L,
                label = "${effect.blockDurationMinutes}m",
            )
        }
    }

    private fun showToast(text: String) {
        mainHandler.post {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }
}
