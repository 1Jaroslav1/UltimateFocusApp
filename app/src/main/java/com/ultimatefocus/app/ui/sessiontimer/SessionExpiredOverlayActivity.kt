/**
 * Fullscreen overlay shown when a session expires under the
 * MINDFUL_EXTENSION_DELAY behavior mode (ULT-81). The overlay forces a
 * loader-only wait (EXTEND_DELAY_MS): no headline buttons, no extend
 * affordance, just a determinate progress circle filling 0% → 100%.
 * When the loader completes the activity automatically launches the
 * duration picker so the user re-commits to a fresh session — this is
 * functionally an "ask me again" rather than an "extend by N more"
 * because the picker presents the same 1/5/10/15 minute options as
 * the original launch.
 *
 * Back press is suppressed for the entire wait. There is intentionally
 * no cancel option: tapping back would otherwise trivially defeat the
 * friction the mode is designed to enforce.
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Suppress back for the entire forced wait — the friction is the
        // wait itself, no escape hatch.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )

        val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val label = intent.getStringExtra(EXTRA_APP_LABEL).orEmpty()
        val priorMode = intent.getStringExtra(EXTRA_DEFAULT_MODE)
            ?.let { runCatching { SessionExpiryMode.valueOf(it) }.getOrNull() }
            ?: SessionExpiryMode.MINDFUL_EXTENSION_DELAY
        viewModel.bind(pkg, label)

        lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    SessionExpiredOverlayViewModel.Event.Done -> {
                        // Loader complete — automatically launch the picker
                        // so the user re-commits a fresh session. We pass
                        // priorMode through so MINDFUL_EXTENSION_DELAY
                        // remains the default for the new session as well.
                        startActivity(
                            SessionDurationPickerActivity.intent(
                                context = this@SessionExpiredOverlayActivity,
                                packageName = pkg,
                                appLabel = label,
                                defaultMode = priorMode,
                            ),
                        )
                        finish()
                    }
                }
            }
        }

        setContent {
            UltimateFocusTheme {
                SessionExpiredOverlayScreen(viewModel = viewModel)
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_DEFAULT_MODE = "extra_default_mode"

        /**
         * Forced wait before the picker re-launches. Sits at the lower
         * end of the AC's "15..30 second" suggestion to keep friction
         * meaningful without being punitive.
         */
        const val EXTEND_DELAY_MS: Long = 20_000L

        fun intent(
            context: Context,
            packageName: String,
            appLabel: String,
            defaultMode: SessionExpiryMode = SessionExpiryMode.MINDFUL_EXTENSION_DELAY,
        ): Intent = Intent(context, SessionExpiredOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_LABEL, appLabel)
            putExtra(EXTRA_DEFAULT_MODE, defaultMode.name)
        }
    }
}
