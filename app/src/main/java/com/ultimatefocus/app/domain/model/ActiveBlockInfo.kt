/**
 * Why a given package is currently blocked. Returned by
 * BlockingSourceCoordinator so consumers (BlockedAppActivity, the launcher
 * blocked-launch routing) can render the right copy: "Blocked for 1h" for
 * manual blocks vs. "Blocked by Work Focus until 17:00" for scheduled ones.
 *
 * When both a manual and a scheduled block apply to the same package the
 * coordinator returns Manual (the more binding source — manual blocks have
 * no self-unblock per ULT-20/21).
 * Covers: ULT-24
 */
package com.ultimatefocus.app.domain.model

sealed interface ActiveBlockInfo {
    val packageName: String
    val blockedUntilEpochMs: Long

    data class Manual(
        override val packageName: String,
        override val blockedUntilEpochMs: Long,
        val durationLabel: String,
    ) : ActiveBlockInfo

    data class Scheduled(
        override val packageName: String,
        override val blockedUntilEpochMs: Long,
        val scheduleName: String,
    ) : ActiveBlockInfo
}
