/**
 * Application entry point. Bootstraps the Hilt dependency injection container
 * so that all @Inject dependencies are available throughout the app lifecycle.
 * Covers: ULT-5
 */
package com.ultimatefocus.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class UltimateFocusApp : Application()
