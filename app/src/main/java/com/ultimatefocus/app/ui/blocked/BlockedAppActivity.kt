/**
 * Full-screen reminder shown when the user attempts to launch a blocked app.
 * Hosted as a separate Activity (rather than a modal) so the reminder is
 * reachable from any launch path — favorites, drawer, quick shortcuts — and
 * survives process death.
 * Extras carry the packageName, human label, expiry timestamp, and the
 * user's chosen duration label so the ViewModel can render a live countdown.
 * excludeFromRecents + singleTask ensure the reminder is not stackable.
 * Covers: ULT-21
 */
package com.ultimatefocus.app.ui.blocked

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import com.ultimatefocus.app.ui.theme.UltimateFocusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockedAppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = finishToHome()
            },
        )
        setContent {
            UltimateFocusTheme {
                BlockedAppScreen(onDismiss = ::finishToHome)
            }
        }
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

        fun intent(
            context: Context,
            packageName: String,
            appLabel: String,
            blockedUntilEpochMs: Long,
            durationLabel: String,
        ): Intent = Intent(context, BlockedAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_LABEL, appLabel)
            putExtra(EXTRA_BLOCKED_UNTIL_MS, blockedUntilEpochMs)
            putExtra(EXTRA_DURATION_LABEL, durationLabel)
        }
    }
}
