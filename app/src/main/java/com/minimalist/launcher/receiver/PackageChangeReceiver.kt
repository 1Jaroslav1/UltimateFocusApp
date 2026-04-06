/**
 * BroadcastReceiver that listens for app install, remove, and update events
 * from the Android system and triggers a refresh of the installed apps list.
 * Covers: ULT-5
 */
package com.minimalist.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.minimalist.launcher.data.repository.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appRepository: AppRepository

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_CHANGED,
            -> appRepository.refreshApps()
        }
    }
}
