/**
 * Plain data class that mirrors the extras carried by BlockedAppActivity's
 * launching Intent. Hoisted out of the activity into Compose state so the
 * screen re-renders the correct app label / countdown when a NEW intent
 * arrives at the existing singleTask BlockedAppActivity instance via
 * onNewIntent — the previous design captured these into private vals on
 * a SavedStateHandle-backed ViewModel and never updated, which caused
 * the bug fixed by ULT-84: opening app2 while the reminder for app1 is
 * still on screen kept showing app1's name.
 *
 * Exactly one of (durationLabel, scheduleName) is non-empty, mirroring
 * the historical contract of BlockedAppViewModel.
 *
 * Covers: ULT-21, ULT-24, ULT-84
 */
package com.ultimatefocus.app.ui.blocked

import android.content.Intent

data class BlockedAppExtras(
    val packageName: String = "",
    val appLabel: String = "",
    val blockedUntilEpochMs: Long = 0L,
    val durationLabel: String = "",
    val scheduleName: String = "",
)

internal fun Intent.readBlockedAppExtras(): BlockedAppExtras = BlockedAppExtras(
    packageName = getStringExtra(BlockedAppActivity.EXTRA_PACKAGE_NAME).orEmpty(),
    appLabel = getStringExtra(BlockedAppActivity.EXTRA_APP_LABEL).orEmpty(),
    blockedUntilEpochMs = getLongExtra(BlockedAppActivity.EXTRA_BLOCKED_UNTIL_MS, 0L),
    durationLabel = getStringExtra(BlockedAppActivity.EXTRA_DURATION_LABEL).orEmpty(),
    scheduleName = getStringExtra(BlockedAppActivity.EXTRA_SCHEDULE_NAME).orEmpty(),
)
