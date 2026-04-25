/**
 * UI state for BlockedAppScreen. remainingMs is recomputed each tick from the
 * injected Clock and reaches 0 when the block expires, at which point the
 * activity auto-finishes.
 * unblockDateTimeLabel is the human-readable absolute timestamp ("Apr 19,
 * 18:45") shown alongside the live countdown (ULT-20/ULT-21).
 * Covers: ULT-21
 */
package com.ultimatefocus.app.ui.blocked

data class BlockedAppUiState(
    val packageName: String = "",
    val appLabel: String = "",
    val remainingMs: Long = 0L,
    val durationLabel: String = "",
    val unblockDateTimeLabel: String = "",
    val shouldFinish: Boolean = false,
)
