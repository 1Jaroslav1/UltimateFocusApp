/**
 * Offline-first repository implementation:
 * - Queries the Android LauncherApps system service for installed apps
 * - Combines installed apps with Room-persisted favorites and active
 *   manual blocks reactively (ULT-20 — blocked apps carry a non-null
 *   AppInfo.blockedUntilEpochMs so the UI can render the 🔒 + strikethrough)
 * - Registers a LauncherApps callback so the list auto-refreshes on
 *   installs/removals
 * - Resolves bottom-bar quick-access shortcuts: materializes a user override
 *   to an AppInfo, or falls back to the system default dialer/camera via
 *   PackageManager.resolveActivity (ULT-13, ULT-14)
 * Covers: ULT-5, ULT-6, ULT-7, ULT-13, ULT-14, ULT-20
 */
package com.ultimatefocus.app.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
import com.ultimatefocus.app.data.local.dao.FavoriteDao
import com.ultimatefocus.app.data.local.entity.FavoriteEntity
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.BlockedApp
import com.ultimatefocus.app.domain.model.ShortcutSlot
import com.ultimatefocus.app.domain.model.ShortcutTarget
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val favoriteDao: FavoriteDao,
    private val blockedAppRepository: BlockedAppRepository,
) : AppRepository {

    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userHandle: UserHandle = Process.myUserHandle()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())

    init {
        loadApps()
        registerCallback()
    }

    private fun loadApps() {
        coroutineScope.launch {
            _installedApps.value = queryInstalledApps()
        }
    }

    private fun queryInstalledApps(): List<AppInfo> =
        launcherApps.getActivityList(null, userHandle)
            .filter { it.componentName.packageName != context.packageName }
            .map { info ->
                AppInfo(
                    packageName = info.componentName.packageName,
                    label = info.label.toString(),
                    componentName = info.componentName,
                )
            }
            .sortedBy { it.label.lowercase() }

    private fun registerCallback() {
        launcherApps.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = loadApps()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = loadApps()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = loadApps()
            override fun onPackagesAvailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean,
            ) = loadApps()
            override fun onPackagesUnavailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean,
            ) = loadApps()
        })
    }

    override fun getInstalledApps(): Flow<List<AppInfo>> =
        combine(
            _installedApps,
            favoriteDao.getFavoritePackageNames(),
            blockedAppRepository.observeActiveBlocks(),
        ) { apps, favorites, blocks ->
            val blockMap = blocks.toBlockMap()
            apps.map { app ->
                app.copy(
                    isFavorite = app.packageName in favorites,
                    blockedUntilEpochMs = blockMap[app.packageName],
                )
            }
        }

    override fun searchApps(query: String): Flow<List<AppInfo>> =
        getInstalledApps().map { apps ->
            if (query.isBlank()) apps
            else apps.filter { it.label.contains(query, ignoreCase = true) }
        }

    override fun getFavoriteApps(): Flow<List<AppInfo>> =
        combine(
            _installedApps,
            favoriteDao.getAllFavorites(),
            blockedAppRepository.observeActiveBlocks(),
        ) { apps, favorites, blocks ->
            val favoriteMap = favorites.associateBy { it.packageName }
            val blockMap = blocks.toBlockMap()
            apps.filter { it.packageName in favoriteMap }
                .map { app ->
                    app.copy(
                        isFavorite = true,
                        favoriteOrder = favoriteMap[app.packageName]?.displayOrder ?: 0,
                        blockedUntilEpochMs = blockMap[app.packageName],
                    )
                }
                .sortedBy { it.favoriteOrder }
        }

    override suspend fun toggleFavorite(appInfo: AppInfo) {
        if (favoriteDao.isFavorite(appInfo.packageName)) {
            favoriteDao.deleteByPackageName(appInfo.packageName)
        } else {
            val nextOrder = favoriteDao.getNextOrder()
            favoriteDao.insertFavorite(
                FavoriteEntity(
                    packageName = appInfo.packageName,
                    componentName = appInfo.componentName.flattenToString(),
                    displayOrder = nextOrder,
                ),
            )
        }
    }

    override suspend fun launchApp(appInfo: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = appInfo.componentName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        context.startActivity(intent)
    }

    override fun refreshApps() = loadApps()

    override suspend fun resolveShortcut(
        slot: ShortcutSlot,
        override: ShortcutTarget?,
    ): AppInfo? {
        if (override != null) {
            val match = _installedApps.value.firstOrNull {
                it.componentName.flattenToString() == override.componentName
            }
            if (match != null) return match
        }
        return resolveDefault(slot)
    }

    private fun resolveDefault(slot: ShortcutSlot): AppInfo? {
        val intent = when (slot) {
            ShortcutSlot.LEFT -> Intent(Intent.ACTION_DIAL)
            ShortcutSlot.RIGHT -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        }
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ) ?: return null
        val activity = resolveInfo.activityInfo ?: return null
        val componentName = ComponentName(activity.packageName, activity.name)
        // Prefer an existing AppInfo from the installed list so label/order stay consistent.
        _installedApps.value.firstOrNull { it.componentName == componentName }?.let { return it }
        val pm = context.packageManager
        val label = runCatching { activity.loadLabel(pm).toString() }.getOrNull()
            ?: activity.packageName
        return AppInfo(
            packageName = activity.packageName,
            label = label,
            componentName = componentName,
        )
    }

    private fun List<BlockedApp>.toBlockMap(): Map<String, Long> =
        associate { it.packageName to it.blockedUntilEpochMs }
}
