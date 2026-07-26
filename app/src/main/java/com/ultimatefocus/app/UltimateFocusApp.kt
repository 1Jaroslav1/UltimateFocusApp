/**
 * Application entry point. Bootstraps the Hilt dependency injection container
 * so that all @Inject dependencies are available throughout the app lifecycle.
 *
 * Also starts process-lifetime observers that the system services do not
 * own:
 *  - SessionWarningPresenter (ULT-80) — surfaces the 1-minute pre-end
 *    Toast warning regardless of which app is in the foreground.
 *  - SessionExpiryRunner (ULT-81) — dispatches the configured behavior
 *    mode (overlay countdown / exit / exit-and-block / reminder) when
 *    the active session reaches zero. Process-lifetime so it does not
 *    depend on the AccessibilityService being connected; the overlay
 *    branch uses a goHome → brief delay → startActivity sequence so the
 *    second launch happens while the launcher is foreground.
 *  - ForegroundPoller (ULT-86) — samples the current foreground package via
 *    UsageStatsManager to enforce blocks and drive session pause/resume
 *    independently of BlockedAppAccessibilityService. Needed because that
 *    service goes silent when OEM battery management (e.g. Samsung)
 *    suspends it, and because resuming a task from the recents switcher
 *    does not reliably emit a window-state event — while SessionExpiryRunner
 *    keeps dispatching regardless. Without this fallback a killed service
 *    left blocked apps usable and left the countdown permanently "resumed"
 *    against its last known package, so it could still evict whatever app
 *    was actually in front once the deadline passed.
 *
 * Covers: ULT-5, ULT-80, ULT-81, ULT-86
 */
package com.ultimatefocus.app

import android.app.Application
import com.ultimatefocus.app.service.ForegroundPoller
import com.ultimatefocus.app.service.SessionExpiryRunner
import com.ultimatefocus.app.service.SessionWarningPresenter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class UltimateFocusApp : Application() {

    @Inject
    lateinit var sessionWarningPresenter: SessionWarningPresenter

    @Inject
    lateinit var sessionExpiryRunner: SessionExpiryRunner

    @Inject
    lateinit var foregroundPoller: ForegroundPoller

    override fun onCreate() {
        super.onCreate()
        sessionWarningPresenter.start()
        sessionExpiryRunner.start()
        foregroundPoller.start()
    }
}
