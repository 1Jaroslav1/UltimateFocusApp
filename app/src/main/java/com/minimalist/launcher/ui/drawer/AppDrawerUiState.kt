/**
 * Sealed UI state for the app drawer screen.
 * Loading → initial fetch; Success → alphabetically sorted text list.
 * Covers: ULT-7
 */
package com.minimalist.launcher.ui.drawer

import com.minimalist.launcher.domain.model.AppInfo

sealed interface AppDrawerUiState {
    data object Loading : AppDrawerUiState
    data class Success(val apps: List<AppInfo>) : AppDrawerUiState
}
