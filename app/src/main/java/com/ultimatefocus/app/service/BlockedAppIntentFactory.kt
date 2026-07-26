/**
 * Builds the BlockedAppActivity intent for a blocked package, resolving the
 * active block source (manual / scheduled / tag) into the source-specific
 * copy fragment the screen renders.
 *
 * Extracted so the two independent enforcement paths — the immediate
 * AccessibilityService path and the accessibility-free ForegroundPoller
 * fallback (ULT-86) — construct the intent identically and cannot drift.
 *
 * [launchContext] is the caller's own Context rather than the application
 * context: the AccessibilityService must launch from its own context to
 * retain background-activity-launch privileges on Android 10+.
 *
 * Covers: ULT-21, ULT-24, ULT-75, ULT-86
 */
package com.ultimatefocus.app.service

import android.content.Context
import android.content.Intent
import com.ultimatefocus.app.domain.model.ActiveBlockInfo
import com.ultimatefocus.app.domain.service.BlockingSourceCoordinator
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedAppIntentFactory @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val blockingSourceCoordinator: BlockingSourceCoordinator,
) {

    /** Null when [packageName] has no active block from any source. */
    suspend fun intentFor(launchContext: Context, packageName: String): Intent? {
        val info = blockingSourceCoordinator.activeBlockInfo(packageName) ?: return null
        val label = appLabel(packageName)
        val intent = when (info) {
            is ActiveBlockInfo.Manual -> BlockedAppActivity.intent(
                context = launchContext,
                packageName = packageName,
                appLabel = label,
                blockedUntilEpochMs = info.blockedUntilEpochMs,
                durationLabel = info.durationLabel,
                scheduleName = null,
            )
            is ActiveBlockInfo.Scheduled -> BlockedAppActivity.intent(
                context = launchContext,
                packageName = packageName,
                appLabel = label,
                blockedUntilEpochMs = info.blockedUntilEpochMs,
                durationLabel = "",
                scheduleName = info.scheduleName,
            )
            is ActiveBlockInfo.Tag -> BlockedAppActivity.intent(
                context = launchContext,
                packageName = packageName,
                appLabel = label,
                blockedUntilEpochMs = info.blockedUntilEpochMs,
                durationLabel = info.durationLabel,
                scheduleName = null,
            )
        }
        return intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    fun appLabel(packageName: String): String = runCatching {
        val pm = appContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)
}
