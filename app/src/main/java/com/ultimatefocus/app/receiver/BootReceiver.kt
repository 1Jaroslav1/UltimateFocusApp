/**
 * BroadcastReceiver that fires when the device finishes booting.
 * Best-effort prune of expired app blocks so the notification listener
 * service wakes up with an accurate view of "what is currently blocked".
 * A reboot is the one way a block can silently expire without the launcher
 * process being alive to observe it — we catch up here.
 * Covers: ULT-5, ULT-22
 */
package com.ultimatefocus.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun blockedAppRepository(): BlockedAppRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootReceiverEntryPoint::class.java,
        )
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                entryPoint.blockedAppRepository().pruneExpired()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
