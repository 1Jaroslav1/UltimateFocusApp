/**
 * Android implementation of SystemActions. Talks to real PackageManager,
 * StatusBarManager, and DevicePolicyManager. Kept thin so that tests exercise
 * the ViewModel + Compose layers against a fake instead of this class.
 *
 * - openWebSearch + queryWebSearchProviders (ULT-16)
 * - openNotificationShade (ULT-17)
 * - lockScreen / requestLockPermission / isLockPermissionGranted (ULT-18)
 *
 * Covers: ULT-16, ULT-17, ULT-18
 */
package com.ultimatefocus.app.data.system

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.ultimatefocus.app.domain.service.LockResult
import com.ultimatefocus.app.domain.service.SystemActions
import com.ultimatefocus.app.domain.service.WebSearchProvider
import com.ultimatefocus.app.receiver.LockDeviceAdminReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemActionsImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SystemActions {

    override fun openNotificationShade() {
        // The API is hidden, so reflection is the standard approach used by
        // minimal launchers. `StatusBarManager.expandNotificationsPanel()` is
        // intentionally non-public but reliably present on stock Android.
        runCatching {
            val service = context.getSystemService("statusbar")
            val clazz = Class.forName("android.app.StatusBarManager")
            val method = clazz.getMethod("expandNotificationsPanel")
            method.invoke(service)
        }
    }

    override fun openWebSearch(providerPackage: String?) {
        val pm = context.packageManager

        val webSearch = Intent(Intent.ACTION_WEB_SEARCH).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (providerPackage != null) setPackage(providerPackage)
        }
        if (webSearch.resolveActivity(pm) != null) {
            runCatching { context.startActivity(webSearch) }
                .onSuccess { return }
        }

        // Fallback: open the provider's web home page (or Google's) via ACTION_VIEW.
        val fallback = Intent(Intent.ACTION_VIEW, FALLBACK_URL.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (providerPackage != null) setPackage(providerPackage)
        }
        runCatching { context.startActivity(fallback) }
    }

    override fun queryWebSearchProviders(): List<WebSearchProvider> {
        val pm = context.packageManager

        val searchHandlers = pm.queryIntentActivities(
            Intent(Intent.ACTION_WEB_SEARCH),
            0,
        )
        val httpsHandlers = pm.queryIntentActivities(
            Intent(Intent.ACTION_VIEW, "https://example.com".toUri()),
            0,
        )

        return (searchHandlers + httpsHandlers)
            .asSequence()
            .map { it.activityInfo }
            .distinctBy { it.packageName }
            .map { info ->
                WebSearchProvider(
                    packageName = info.packageName,
                    label = info.loadLabel(pm).toString(),
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    override fun lockScreen(): LockResult {
        val dpm = devicePolicyManager()
        val admin = adminComponent()
        if (!dpm.isAdminActive(admin)) return LockResult.MissingPermission
        return runCatching {
            dpm.lockNow()
            LockResult.Success
        }.getOrElse {
            Toast.makeText(
                context,
                "Screen lock failed: ${it.message ?: it.javaClass.simpleName}",
                Toast.LENGTH_LONG,
            ).show()
            LockResult.MissingPermission
        }
    }

    override fun requestLockPermission() {
        // Short, visible confirmation that the gesture reached the VM. If the
        // admin prompt itself fails to appear (some OEM launchers block
        // startActivity from a non-activity context), the toast still confirms
        // the two-finger tap was detected.
        Toast.makeText(
            context,
            "Grant screen-lock permission to use two-finger tap.",
            Toast.LENGTH_LONG,
        ).show()

        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent())
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Grant screen-lock permission so the two-finger tap on the " +
                    "home screen can lock the device.",
            )
        }
        runCatching { context.startActivity(intent) }
    }

    override fun isLockPermissionGranted(): Boolean =
        devicePolicyManager().isAdminActive(adminComponent())

    private fun devicePolicyManager(): DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private fun adminComponent(): ComponentName =
        ComponentName(context, LockDeviceAdminReceiver::class.java)

    private companion object {
        const val FALLBACK_URL = "https://www.google.com"
    }
}
