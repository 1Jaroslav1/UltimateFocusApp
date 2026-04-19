/**
 * Pure-logic helper behind BlockedAppAccessibilityService. Given a foreground
 * package name, the current clock, and the set of actively blocked packages,
 * decides whether to (a) push the user to the blocked reminder or (b) ignore.
 * Tracks a per-package debounce window so the same package doesn't re-fire
 * on every transient child window during dismissal.
 * Factored out of the AccessibilityService so the decision rule is
 * unit-testable without booting the system-bound service lifecycle.
 * Covers: ULT-20, ULT-21
 */
package com.ultimatefocus.app.domain.service

import javax.inject.Inject
import javax.inject.Singleton

sealed interface ForegroundAction {
    data class ShowBlocked(val packageName: String) : ForegroundAction
    data object Ignore : ForegroundAction
}

@Singleton
class BlockedAppForegroundHandler @Inject constructor() {

    private val lastFiredAt: MutableMap<String, Long> = mutableMapOf()

    fun onForegroundPackage(
        packageName: String?,
        nowMs: Long,
        activeBlocked: Set<String>,
        ownPackage: String,
        debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    ): ForegroundAction {
        if (packageName.isNullOrBlank()) return ForegroundAction.Ignore
        if (packageName == ownPackage) return ForegroundAction.Ignore
        if (packageName in SYSTEM_PACKAGES) return ForegroundAction.Ignore
        if (packageName !in activeBlocked) return ForegroundAction.Ignore

        val last = lastFiredAt[packageName]
        if (last != null && nowMs - last < debounceMs) return ForegroundAction.Ignore

        lastFiredAt[packageName] = nowMs
        return ForegroundAction.ShowBlocked(packageName)
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MS: Long = 1_500L

        // Excluded: the core system UI, keyboard, and telephony/system dialer
        // surfaces users should never be blocked from reaching.
        private val SYSTEM_PACKAGES: Set<String> = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.inputmethod.latin",
            "com.android.inputmethod.latin",
        )
    }
}
