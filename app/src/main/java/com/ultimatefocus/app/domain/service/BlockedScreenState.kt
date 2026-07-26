/**
 * Tracks which package's block screen (BlockedAppActivity) is currently
 * visible, if any. Replaces a time-based debounce for gating re-shows of
 * the block screen: BlockedAppForegroundHandler only suppresses ShowBlocked
 * while this holds the same package, so rapidly closing/reopening a
 * blocked app can never slip past the block (ULT-86) — any genuine reopen
 * always finds [visiblePackage] cleared and re-triggers the block screen.
 *
 * Covers: ULT-86
 */
package com.ultimatefocus.app.domain.service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedScreenState @Inject constructor() {

    @Volatile
    var visiblePackage: String? = null
        private set

    fun onShown(packageName: String) {
        visiblePackage = packageName
    }

    fun onHidden(packageName: String) {
        if (visiblePackage == packageName) {
            visiblePackage = null
        }
    }
}
