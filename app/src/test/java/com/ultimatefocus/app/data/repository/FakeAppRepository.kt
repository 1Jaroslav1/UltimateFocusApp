/**
 * In-memory test double for AppRepository. Used in ViewModel unit tests to avoid
 * Android system dependencies (LauncherApps, Room). Covers all AppRepository operations
 * including shortcut resolution for bottom-bar quick-access slots.
 * Covers: ULT-6, ULT-7, ULT-8, ULT-13, ULT-14
 */
package com.ultimatefocus.app.data.repository

import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.ShortcutSlot
import com.ultimatefocus.app.domain.model.ShortcutTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAppRepository : AppRepository {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _favorites = MutableStateFlow<List<AppInfo>>(emptyList())
    var launchedApp: AppInfo? = null
    var refreshCallCount = 0

    /** Defaults returned by resolveShortcut when the override is null or cannot be matched. */
    val defaultShortcuts: MutableMap<ShortcutSlot, AppInfo?> =
        mutableMapOf(ShortcutSlot.LEFT to null, ShortcutSlot.RIGHT to null)

    fun setApps(apps: List<AppInfo>) { _apps.value = apps }
    fun setFavorites(favorites: List<AppInfo>) { _favorites.value = favorites }

    override fun getInstalledApps(): Flow<List<AppInfo>> = _apps

    override fun searchApps(query: String): Flow<List<AppInfo>> = _apps.map { apps ->
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    override fun getFavoriteApps(): Flow<List<AppInfo>> = _favorites

    override suspend fun toggleFavorite(appInfo: AppInfo) {
        val current = _favorites.value.toMutableList()
        if (current.any { it.packageName == appInfo.packageName }) {
            _favorites.value = current.filter { it.packageName != appInfo.packageName }
        } else {
            _favorites.value = current + appInfo.copy(isFavorite = true)
        }
    }

    override suspend fun launchApp(appInfo: AppInfo) {
        launchedApp = appInfo
    }

    override fun refreshApps() { refreshCallCount++ }

    override suspend fun resolveShortcut(
        slot: ShortcutSlot,
        override: ShortcutTarget?,
    ): AppInfo? {
        if (override != null) {
            val match = _apps.value.firstOrNull {
                it.componentName.flattenToString() == override.componentName
            }
            if (match != null) return match
        }
        return defaultShortcuts[slot]
    }
}
