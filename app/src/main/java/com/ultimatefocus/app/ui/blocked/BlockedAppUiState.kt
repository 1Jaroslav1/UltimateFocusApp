/**
 * UI state for BlockedAppScreen. remainingMs is recomputed each tick from the
 * injected Clock and reaches 0 when the block expires, at which point the
 * activity auto-finishes.
 * Covers: ULT-21
 */
package com.ultimatefocus.app.ui.blocked

data class BlockedAppUiState(
    val packageName: String = "",
    val appLabel: String = "",
    val remainingMs: Long = 0L,
    val durationLabel: String = "",
    val shouldFinish: Boolean = false,
)
