/**
 * Sealed UI state for the app drawer screen.
 * Loading → initial fetch; Success → alphabetically sorted text list.
 * Covers: ULT-7
 */
package com.ultimatefocus.app.ui.drawer

import com.ultimatefocus.app.domain.model.AppInfo

sealed interface AppDrawerUiState {
    data object Loading : AppDrawerUiState
    data class Success(val apps: List<AppInfo>) : AppDrawerUiState
}
