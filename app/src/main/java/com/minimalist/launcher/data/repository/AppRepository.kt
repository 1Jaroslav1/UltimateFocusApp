/**
 * Repository interface defining the contract for app data operations:
 * querying installed apps, managing favorites, and launching apps.
 * Covers: ULT-5, ULT-6, ULT-7
 */
package com.minimalist.launcher.data.repository

import com.minimalist.launcher.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getInstalledApps(): Flow<List<AppInfo>>
    fun searchApps(query: String): Flow<List<AppInfo>>
    fun getFavoriteApps(): Flow<List<AppInfo>>
    suspend fun toggleFavorite(appInfo: AppInfo)
    suspend fun launchApp(appInfo: AppInfo)
    fun refreshApps()
}
