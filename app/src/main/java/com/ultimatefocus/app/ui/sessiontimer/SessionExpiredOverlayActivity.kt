/**
 * Fullscreen overlay shown when a session expires under the
 * MINDFUL_EXTENSION_DELAY behavior mode (ULT-81). The overlay forces a
 * countdown (EXTEND_DELAY_MS) before the "Extend session" button
 * activates — friction is the whole point.
 *
 * Back press is suppressed until the countdown elapses (you cannot
 * dismiss your way past the wait), and after the user taps "Extend"
 * we route them through the duration picker again so they re-commit a
 * fresh duration.
 *
 * Covers: ULT-81
 */
package com.ultimatefocus.app.ui.sessiontimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SessionExpiredOverlayActivity : ComponentActivity() {

    private val viewModel: SessionExpiredOverlayViewModel by viewModels()
    private var backCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        }
        onBackPressedDispatcher.addCallback(this, backCallback!!)

        val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val label = intent.getStringExtra(EXTRA_APP_LABEL).orEmpty()
        viewModel.bind(pkg, label)

        lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    SessionExpiredOverlayViewModel.Event.CountdownDone -> {
                        backCallback?.isEnabled = false // allow back now
                    }
                    SessionExpiredOverlayViewModel.Event.ExtendRequested -> {
                        // Route the user back through the duration picker so
                        // they consciously re-commit a fresh duration. The
                        // global default mode is used; the picker's expanded
                        // panel still allows a per-session override.
                        startActivity(
                            SessionDurationPickerActivity.intent(
                                context = this@SessionExpiredOverlayActivity,
                                packageName = pkg,
                                appLabel = label,
                                defaultMode = SessionExpiryMode.MINDFUL_EXTENSION_DELAY,
                            ),
                        )
                        finish()
                    }
                    SessionExpiredOverlayViewModel.Event.Cancelled -> finish()
                }
            }
        }

        setContent {
            UltimateFocusTheme {
                SessionExpiredOverlayScreen(
                    viewModel = viewModel,
                    onCancel = { viewModel.cancel() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"

        /**
         * Forced wait before the Extend button becomes tappable. Sits at the
         * lower end of the AC's "15..30 second" suggestion to keep friction
         * meaningful without being punitive.
         */
        const val EXTEND_DELAY_MS: Long = 20_000L

        fun intent(
            context: Context,
            packageName: String,
            appLabel: String,
        ): Intent = Intent(context, SessionExpiredOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_LABEL, appLabel)
        }
    }
}
