/**
 * ViewModel behind BlockedAppScreen. Reads launch extras from
 * SavedStateHandle, then ticks a Clock-driven countdown each second.
 * When the block expires, shouldFinish flips so the Screen can close
 * the Activity and let the user retry launching the app.
 * Covers: ULT-21
 */
package com.ultimatefocus.app.ui.blocked

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity.Companion.EXTRA_APP_LABEL
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity.Companion.EXTRA_BLOCKED_UNTIL_MS
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity.Companion.EXTRA_DURATION_LABEL
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity.Companion.EXTRA_PACKAGE_NAME
import com.ultimatefocus.app.util.BlockedTimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockedAppViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clock: Clock,
) : ViewModel() {

    private val packageName: String = savedStateHandle[EXTRA_PACKAGE_NAME] ?: ""
    private val appLabel: String = savedStateHandle[EXTRA_APP_LABEL] ?: ""
    private val blockedUntilEpochMs: Long =
        savedStateHandle.get<Long>(EXTRA_BLOCKED_UNTIL_MS) ?: 0L
    private val durationLabel: String = savedStateHandle[EXTRA_DURATION_LABEL] ?: ""

    // Declared BEFORE _uiState because compute() reads this field on first
    // invocation from the _uiState initializer. Property initializers run in
    // declaration order; placing this below _uiState left the backing field
    // null when compute() first ran and crashed the activity on startup.
    private val unblockDateTimeLabel: String =
        if (blockedUntilEpochMs > 0L) BlockedTimeFormatter.formatUnblockDateTime(blockedUntilEpochMs)
        else ""

    private val _uiState = MutableStateFlow(compute())
    val uiState: StateFlow<BlockedAppUiState> = _uiState.asStateFlow()

    init {
        startCountdownLoop()
    }

    fun refresh() {
        _uiState.value = compute()
    }

    private fun compute(): BlockedAppUiState {
        val remaining = (blockedUntilEpochMs - clock.now()).coerceAtLeast(0L)
        return BlockedAppUiState(
            packageName = packageName,
            appLabel = appLabel,
            remainingMs = remaining,
            durationLabel = durationLabel,
            unblockDateTimeLabel = unblockDateTimeLabel,
            shouldFinish = remaining <= 0L,
        )
    }

    private fun startCountdownLoop() {
        viewModelScope.launch {
            while (isActive) {
                _uiState.value = compute()
                if (_uiState.value.shouldFinish) break
                delay(1_000L)
            }
        }
    }
}
