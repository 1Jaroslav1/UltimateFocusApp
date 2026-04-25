/**
 * Helpers for checking whether PACKAGE_USAGE_STATS access has been granted
 * to the launcher and for deep-linking the user to the system Usage-Access
 * settings page where the toggle lives. Mirrors AccessibilityPermission's
 * shape so prompt UI can be authored in the same idiom.
 *
 * Granted state comes from AppOpsManager.checkOpNoThrow with the
 * GET_USAGE_STATS op — the canonical way Android reports the per-package
 * decision. Without it the schedule editor's app picker falls back to
 * alphabetical sorting.
 *
 * Covers: ULT-24 (iteration)
 */
package com.ultimatefocus.app.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

object UsageStatsPermission {

    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager?
            ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
