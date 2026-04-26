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
 *    the active session reaches zero.
 *
 * Covers: ULT-5, ULT-80, ULT-81
 */
package com.ultimatefocus.app

import android.app.Application
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

    override fun onCreate() {
        super.onCreate()
        sessionWarningPresenter.start()
        sessionExpiryRunner.start()
    }
}
