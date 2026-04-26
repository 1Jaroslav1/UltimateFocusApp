/**
 * Subscribes to SessionWarningController.warningEvents() and surfaces each
 * event as a Toast with the documented copy ("1 minute left in [App]").
 *
 * Lives in the Application's Hilt-scoped singleton graph so the
 * subscription is process-lifetime: warnings fire whether the launcher
 * is in the foreground, the user is inside the target app, or anywhere
 * else (the AC explicitly requires "the popup appears even if the user
 * has navigated away from the app and returned").
 *
 * Toast.makeText is sufficient on modern Android because we are an
 * installed launcher and the popup target is always a foreground app —
 * we don't need SYSTEM_ALERT_WINDOW.
 *
 * Covers: ULT-80
 */
package com.ultimatefocus.app.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.ultimatefocus.app.domain.service.SessionWarningController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionWarningPresenter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: SessionWarningController,
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    fun start() {
        controller.warningEvents()
            .onEach { event ->
                val target = event.appLabel.ifBlank { "this app" }
                val text = "1 minute left in $target"
                mainHandler.post {
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                }
            }
            .launchIn(scope)
    }
}
