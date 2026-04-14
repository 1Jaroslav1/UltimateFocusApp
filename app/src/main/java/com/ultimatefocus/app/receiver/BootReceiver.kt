/**
 * BroadcastReceiver that fires when the device finishes booting.
 * Placeholder for future background service restarts on reboot.
 * Covers: ULT-5
 */
package com.ultimatefocus.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Future: restart any background services needed after device boot
    }
}
