/**
 * Full-screen reminder shown when the user attempts to launch a blocked app.
 * Hosted as a separate Activity (rather than a modal) so the reminder is
 * reachable from any launch path — favorites, drawer, quick shortcuts — and
 * survives process death.
 *
 * Extras carry the packageName, human label, expiry timestamp, and the
 * source-specific copy fragment:
 *   - durationLabel  (manual block, e.g. "1h", "24h"; ULT-21)
 *   - scheduleName   (recurring schedule, e.g. "Work Focus"; ULT-24)
 * exactly one of those is non-empty.
 *
 * excludeFromRecents + singleTask ensure the reminder is not stackable.
 *
 * ULT-84: extras are hoisted into Compose mutableState and refreshed in
 * onNewIntent so re-entries with a different blocked app render the new
 * label + countdown instead of stale data from the first entry. Clock is
 * injected via Hilt and provided to the Compose tree as a parameter.
 *
 * Covers: ULT-21, ULT-24, ULT-84
 */
package com.ultimatefocus.app.ui.blocked

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class BlockedAppActivity : ComponentActivity() {

    @Inject
    lateinit var clock: Clock

    private val extrasState: MutableStateFlow<BlockedAppExtras> =
        MutableStateFlow(BlockedAppExtras())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extrasState.value = intent.readBlockedAppExtras()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = finishToHome()
            },
        )
        setContent {
            val extras by extrasState.asStateFlow().collectAsState()
            UltimateFocusTheme {
                BlockedAppScreen(
                    extras = extras,
                    clock = clock,
                    onDismiss = ::finishToHome,
                )
            }
        }
    }

    /**
     * singleTask + FLAG_ACTIVITY_CLEAR_TOP makes Android reuse this
     * existing instance and deliver new launches via onNewIntent. Update
     * the activity's intent and the Compose extras so the screen
     * re-renders against the new app — without this override, ULT-84's
     * "wrong app shown" bug would re-appear.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extrasState.value = intent.readBlockedAppExtras()
    }

    private fun finishToHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(home)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_BLOCKED_UNTIL_MS = "extra_blocked_until_ms"
        const val EXTRA_DURATION_LABEL = "extra_duration_label"
        const val EXTRA_SCHEDULE_NAME = "extra_schedule_name"

        fun intent(
            context: Context,
            packageName: String,
            appLabel: String,
            blockedUntilEpochMs: Long,
            durationLabel: String,
            scheduleName: String?,
        ): Intent = Intent(context, BlockedAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_LABEL, appLabel)
            putExtra(EXTRA_BLOCKED_UNTIL_MS, blockedUntilEpochMs)
            putExtra(EXTRA_DURATION_LABEL, durationLabel)
            if (scheduleName != null) putExtra(EXTRA_SCHEDULE_NAME, scheduleName)
        }
    }
}
