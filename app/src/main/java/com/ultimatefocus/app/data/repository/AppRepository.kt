/**
 * Repository interface defining the contract for app data operations:
 * querying installed apps, managing favorites, and launching apps.
 * launchApp returns a sealed LaunchResult so callers can route a Blocked
 * attempt to the reminder screen without needing a Context (ULT-21).
 * Also resolves bottom-bar quick-access shortcut targets, materializing either
 * the user's override or the system default handler for Phone/Camera.
 * Covers: ULT-5, ULT-6, ULT-7, ULT-13, ULT-14, ULT-20, ULT-21
 */
package com.ultimatefocus.app.data.repository

import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.LaunchResult
import com.ultimatefocus.app.domain.model.ShortcutSlot
import com.ultimatefocus.app.domain.model.ShortcutTarget
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getInstalledApps(): Flow<List<AppInfo>>
    fun searchApps(query: String): Flow<List<AppInfo>>
    fun getFavoriteApps(): Flow<List<AppInfo>>
    suspend fun toggleFavorite(appInfo: AppInfo)
    suspend fun launchApp(appInfo: AppInfo): LaunchResult
    fun refreshApps()
    suspend fun resolveShortcut(slot: ShortcutSlot, override: ShortcutTarget?): AppInfo?
}
