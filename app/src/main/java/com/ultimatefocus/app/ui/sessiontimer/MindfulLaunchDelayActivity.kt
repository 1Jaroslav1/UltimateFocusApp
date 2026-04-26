/**
 * Hosts the "mindful launch delay" overlay shown between the duration
 * picker and the actual app launch (ULT-79). Lives in its own Activity
 * for the same reason as the picker — reachable from any launch path.
 *
 * The delay duration is fixed (DELAY_MS = 4_000ms) and not configurable,
 * matching the AC: "the delay duration is fixed and not configurable by
 * the user". After the animation completes the activity dispatches the
 * actual launch via AppRepository.launchApp (so manual blocks, schedules,
 * etc. still gate, even though SessionTimerCoordinator has already started
 * a session and short-circuits the session pre-check) and finishes.
 *
 * Back press is suppressed during the animation so the user must wait —
 * also from the AC ("the user cannot skip or dismiss the animation").
 *
 * Covers: ULT-79
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
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MindfulLaunchDelayActivity : ComponentActivity() {

    private val viewModel: MindfulLaunchDelayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Suppress back during the delay so the user is forced to wait (AC).
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )

        val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val label = intent.getStringExtra(EXTRA_APP_LABEL).orEmpty()
        viewModel.bind(pkg, label)

        lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    MindfulLaunchDelayViewModel.Event.Done -> {
                        viewModel.completeLaunch()
                        finish()
                    }
                }
            }
        }

        setContent {
            UltimateFocusTheme {
                MindfulLaunchDelayScreen(viewModel = viewModel)
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"

        /**
         * Fixed delay duration. AC explicitly forbids user configurability
         * — the friction would lose meaning otherwise.
         */
        const val DELAY_MS: Long = 4_000L

        fun intent(
            context: Context,
            packageName: String,
            appLabel: String,
        ): Intent = Intent(context, MindfulLaunchDelayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_LABEL, appLabel)
        }
    }
}
