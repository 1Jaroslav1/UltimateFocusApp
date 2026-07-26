/**
 * Pure-logic helper behind BlockedAppAccessibilityService. Given a foreground
 * package name, the current clock, the set of actively blocked packages,
 * and the set of reminder-enabled packages, decides whether to (a) push the
 * user to the blocked reminder, (b) push them through the session-duration
 * picker (ULT-78), or (c) ignore.
 *
 * The blocked-reminder branch is gated on [BlockedScreenState] (whether the
 * block screen is genuinely still visible for this package) rather than a
 * timer, so rapidly closing/reopening a blocked app can never slip past the
 * block (ULT-86) — a fixed debounce window is not used there. The picker
 * branch keeps a per-package debounce window so the same package doesn't
 * re-prompt on every transient child window during its own dismissal;
 * there's no bypass risk there since a reminder-enabled app isn't actually
 * blocked.
 *
 * Factored out of the AccessibilityService so the decision rule is
 * unit-testable without booting the system-bound service lifecycle.
 *
 * Manual/scheduled/tag blocks take priority over the session picker — if a
 * package is BOTH actively blocked AND opted-in to the reminder, we want
 * the user to see the block, not be allowed to commit a session.
 *
 * Covers: ULT-20, ULT-21, ULT-78, ULT-86
 */
package com.ultimatefocus.app.domain.service

import javax.inject.Inject
import javax.inject.Singleton

sealed interface ForegroundAction {
    data class ShowBlocked(val packageName: String) : ForegroundAction
    data class RequireSessionDuration(val packageName: String) : ForegroundAction
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
        reminderEnabled: Set<String> = emptySet(),
        sessionActiveFor: Set<String> = emptySet(),
        blockScreenVisiblePackage: String? = null,
    ): ForegroundAction {
        if (packageName.isNullOrBlank()) return ForegroundAction.Ignore
        if (packageName == ownPackage) return ForegroundAction.Ignore
        if (packageName in SYSTEM_PACKAGES) return ForegroundAction.Ignore

        val isBlocked = packageName in activeBlocked
        val needsPicker = packageName in reminderEnabled && packageName !in sessionActiveFor

        if (!isBlocked && !needsPicker) return ForegroundAction.Ignore

        // Block always wins so the user can't bypass an active block by
        // racing through the duration picker.
        if (isBlocked) {
            // No time-based debounce here: only suppress re-showing the
            // block screen when it is demonstrably still on screen for
            // this exact package. Any genuine reopen of the blocked app
            // must always redirect back to the block screen (ULT-86).
            return if (blockScreenVisiblePackage == packageName) {
                ForegroundAction.Ignore
            } else {
                ForegroundAction.ShowBlocked(packageName)
            }
        }

        val last = lastFiredAt[packageName]
        if (last != null && nowMs - last < debounceMs) return ForegroundAction.Ignore
        lastFiredAt[packageName] = nowMs
        return ForegroundAction.RequireSessionDuration(packageName)
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MS: Long = 1_500L

        // Excluded: the core system UI, keyboard, and telephony/system dialer
        // surfaces users should never be blocked from reaching. Also reused
        // by SessionTimerCoordinator so these surfaces don't pause an
        // in-progress session (ULT-86).
        internal val SYSTEM_PACKAGES: Set<String> = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.inputmethod.latin",
            "com.android.inputmethod.latin",
        )
    }
}
