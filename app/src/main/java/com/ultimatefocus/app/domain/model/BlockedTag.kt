/**
 * Domain model representing an active block on a tag. Mirrors BlockedApp
 * but keyed by tagId. blockedUntilEpochMs is the strict-greater-than
 * threshold used everywhere to decide whether the block is still in
 * effect.
 * Covers: ULT-75
 */
package com.ultimatefocus.app.domain.model

data class BlockedTag(
    val tagId: Long,
    val blockedUntilEpochMs: Long,
    val blockedSinceEpochMs: Long,
    val durationLabel: String,
)
