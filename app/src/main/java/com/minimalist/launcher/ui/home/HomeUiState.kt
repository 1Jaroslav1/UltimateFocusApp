/**
 * Immutable UI state for the home screen: current time and battery info
 * that are updated every second by the ViewModel clock loop.
 * Covers: ULT-6
 */
package com.minimalist.launcher.ui.home

import java.time.LocalDateTime

data class HomeUiState(
    val currentTime: LocalDateTime = LocalDateTime.now(),
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
)
