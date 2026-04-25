/**
 * UI state for BlockedAppScreen. remainingMs is recomputed each tick from the
 * injected Clock and reaches 0 when the block expires, at which point the
 * activity auto-finishes.
 *
 * Exactly one source-specific field carries copy:
 *   - durationLabel  populated for manual blocks ("1h", "24h"; ULT-20/21)
 *   - scheduleName   populated for scheduled blocks (ULT-24)
 *
 * unblockDateTimeLabel is the human-readable absolute timestamp ("Apr 19,
 * 18:45") shown alongside the live countdown.
 *
 * Covers: ULT-21, ULT-24
 */
package com.ultimatefocus.app.ui.blocked

data class BlockedAppUiState(
    val packageName: String = "",
    val appLabel: String = "",
    val remainingMs: Long = 0L,
    val durationLabel: String = "",
    val scheduleName: String = "",
    val unblockDateTimeLabel: String = "",
    val shouldFinish: Boolean = false,
)
