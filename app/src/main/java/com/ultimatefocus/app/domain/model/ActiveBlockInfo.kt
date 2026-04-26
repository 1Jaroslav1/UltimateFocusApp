/**
 * Why a given package is currently blocked. Returned by
 * BlockingSourceCoordinator so consumers (BlockedAppActivity, the launcher
 * blocked-launch routing, the accessibility-service redirect) can render
 * the right copy: "Blocked for 1h" for manual blocks, "Blocked by Work
 * Focus until 17:00" for scheduled, "Blocked via tag" for tag-derived.
 *
 * When multiple sources apply to the same package the coordinator picks
 * the most binding: Manual > Scheduled > Tag. Manual blocks have no
 * self-unblock per ULT-20/21; scheduled blocks have a named, structured
 * window; tag-blocks are a category-level fallback.
 * Covers: ULT-24, ULT-75
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

    /**
     * Block derived from one of the package's tags being category-blocked
     * (ULT-75). When the package belongs to multiple blocked tags the
     * coordinator picks the latest expiry across them.
     */
    data class Tag(
        override val packageName: String,
        override val blockedUntilEpochMs: Long,
        val durationLabel: String,
    ) : ActiveBlockInfo
}
