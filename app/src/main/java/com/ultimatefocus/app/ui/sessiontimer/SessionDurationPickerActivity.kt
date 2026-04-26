/**
 * Hosts the session-duration picker shown when the user attempts to open
 * an app with the In-App Reminder enabled. Lives in its own Activity
 * (rather than as a modal inside the launcher) for the same reason as
 * BlockedAppActivity: the picker has to be reachable from any launch path
 * — the launcher's home, the app drawer, or the AccessibilityService —
 * and survive process death.
 *
 * Intent extras: target package name, target app label, and the global
 * default expiry mode (the picker's expanded panel allows a per-session
 * override).
 *
 * On confirmation the picker calls SessionTimerCoordinator.startSession
 * and then triggers the launch via AppRepository.launchApp again — the
 * pre-check there short-circuits because isSessionActiveFor returns true,
 * so the actual launch proceeds. Back press dismisses without starting
 * a session (AC: "the app launch is cancelled").
 *
 * Covers: ULT-78
 */
package com.ultimatefocus.app.ui.sessiontimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

@AndroidEntryPoint
class SessionDurationPickerActivity : ComponentActivity() {

    private val viewModel: SessionDurationPickerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val label = intent.getStringExtra(EXTRA_APP_LABEL).orEmpty()
        val defaultMode = intent.getStringExtra(EXTRA_DEFAULT_MODE)
            ?.let { runCatching { SessionExpiryMode.valueOf(it) }.getOrNull() }
            ?: SessionExpiryMode.ONLY_REMINDER
        viewModel.bind(pkg, label, defaultMode)

        lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is SessionDurationPickerViewModel.Event.LaunchTarget -> {
                        // Picker → launch the target through AppRepository so
                        // any other gates (manual block etc.) still apply.
                        viewModel.launchTarget()
                        finish()
                    }
                    SessionDurationPickerViewModel.Event.Cancelled -> finish()
                }
            }
        }

        setContent {
            UltimateFocusTheme {
                SessionDurationPickerScreen(
                    viewModel = viewModel,
                    onDismiss = { viewModel.cancel() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_DEFAULT_MODE = "extra_default_mode"

        fun intent(
            context: Context,
            packageName: String,
            appLabel: String,
            defaultMode: SessionExpiryMode,
        ): Intent = Intent(context, SessionDurationPickerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_LABEL, appLabel)
            putExtra(EXTRA_DEFAULT_MODE, defaultMode.name)
        }
    }
}
