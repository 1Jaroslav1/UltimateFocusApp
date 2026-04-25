/**
 * Offline-first repository implementation:
 * - Queries the Android LauncherApps system service for installed apps
 * - Combines installed apps with Room-persisted favorites and active blocks
 *   (manual ULT-20 ∪ scheduled ULT-24) reactively via BlockingSourceCoordinator
 *   — blocked apps carry a non-null AppInfo.blockedUntilEpochMs so the UI can
 *   render the lock + strikethrough.
 * - launchApp enforces blocks: when the target app has an active block
 *   it returns LaunchResult.Blocked instead of starting the intent. The
 *   caller routes the user to BlockedAppActivity (ULT-21).
 * - Registers a LauncherApps callback so the list auto-refreshes on
 *   installs/removals; on removal also calls
 *   BlockingScheduleRepository.purgePackage to keep schedule junctions tidy.
 * - Resolves bottom-bar quick-access shortcuts: materializes a user override
 *   to an AppInfo, or falls back to the system default dialer/camera via
 *   PackageManager.resolveActivity (ULT-13, ULT-14)
 * Covers: ULT-5, ULT-6, ULT-7, ULT-13, ULT-14, ULT-20, ULT-21, ULT-24
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
import com.ultimatefocus.app.domain.model.ActiveBlockInfo
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.LaunchResult
import com.ultimatefocus.app.domain.model.ShortcutSlot
import com.ultimatefocus.app.domain.model.ShortcutTarget
import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import com.ultimatefocus.app.domain.repository.BlockingScheduleRepository
import com.ultimatefocus.app.domain.service.BlockingSourceCoordinator
import com.ultimatefocus.app.domain.service.Clock
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
    private val blockingScheduleRepository: BlockingScheduleRepository,
    private val coordinator: BlockingSourceCoordinator,
    private val clock: Clock,
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
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                loadApps()
                packageName?.let { pkg ->
                    coroutineScope.launch { blockingScheduleRepository.purgePackage(pkg) }
                }
            }
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
            blockingScheduleRepository.observeActiveWindowEndByPackage(),
        ) { apps, favorites, manualBlocks, scheduledUntilByPkg ->
            val manualUntilByPkg = manualBlocks.associate { it.packageName to it.blockedUntilEpochMs }
            apps.map { app ->
                app.copy(
                    isFavorite = app.packageName in favorites,
                    blockedUntilEpochMs = resolveBlockedUntil(
                        app.packageName, manualUntilByPkg, scheduledUntilByPkg,
                    ),
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
            blockingScheduleRepository.observeActiveWindowEndByPackage(),
        ) { apps, favorites, manualBlocks, scheduledUntilByPkg ->
            val favoriteMap = favorites.associateBy { it.packageName }
            val manualUntilByPkg = manualBlocks.associate { it.packageName to it.blockedUntilEpochMs }
            apps.filter { it.packageName in favoriteMap }
                .map { app ->
                    app.copy(
                        isFavorite = true,
                        favoriteOrder = favoriteMap[app.packageName]?.displayOrder ?: 0,
                        blockedUntilEpochMs = resolveBlockedUntil(
                            app.packageName, manualUntilByPkg, scheduledUntilByPkg,
                        ),
                    )
                }
                .sortedBy { it.favoriteOrder }
        }

    /**
     * Manual block wins (more binding contract). Otherwise fall back to the
     * scheduled window-end timestamp. Null when the package is not currently
     * blocked from either source.
     */
    private fun resolveBlockedUntil(
        packageName: String,
        manualUntilByPkg: Map<String, Long>,
        scheduledUntilByPkg: Map<String, Long>,
    ): Long? = manualUntilByPkg[packageName] ?: scheduledUntilByPkg[packageName]

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

    override suspend fun launchApp(appInfo: AppInfo): LaunchResult {
        val info = coordinator.activeBlockInfo(appInfo.packageName)
        if (info != null) {
            val remaining = (info.blockedUntilEpochMs - clock.now()).coerceAtLeast(0L)
            return when (info) {
                is ActiveBlockInfo.Manual -> LaunchResult.Blocked(
                    packageName = appInfo.packageName,
                    appLabel = appInfo.label,
                    remainingMs = remaining,
                    durationLabel = info.durationLabel,
                    scheduleName = null,
                )
                is ActiveBlockInfo.Scheduled -> LaunchResult.Blocked(
                    packageName = appInfo.packageName,
                    appLabel = appInfo.label,
                    remainingMs = remaining,
                    durationLabel = "",
                    scheduleName = info.scheduleName,
                )
            }
        }
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = appInfo.componentName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        context.startActivity(intent)
        return LaunchResult.Launched
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

}
