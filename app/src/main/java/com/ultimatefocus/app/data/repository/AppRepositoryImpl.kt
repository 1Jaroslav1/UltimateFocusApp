/**
 * Offline-first repository implementation:
 * - Queries the Android LauncherApps system service for installed apps
 * - Combines installed apps with Room-persisted favorites, active blocks
 *   from three sources reactively — manual (ULT-20) ∪ scheduled (ULT-24)
 *   via BlockingSourceCoordinator and tag-derived (ULT-75) via
 *   BlockedTagRepository — and tag assignments (ULT-73) so AppInfo carries
 *   both the displayed Tag list and a non-null blockedUntilEpochMs whenever
 *   any source covers the package.
 * - Priority for AppInfo.blockedUntilEpochMs is manual → scheduled → tag.
 *   Manual wins because it is the most binding contract; scheduled wins
 *   over tag because it has a concrete window-end timestamp tied to a
 *   user-named schedule.
 * - launchApp enforces blocks: when the target app has an active block
 *   it returns LaunchResult.Blocked instead of starting the intent. The
 *   caller routes the user to BlockedAppActivity (ULT-21). All three
 *   block sources (manual / scheduled / tag-derived) are routed through
 *   BlockingSourceCoordinator.activeBlockInfo so that bottom-bar
 *   shortcut launches — whose AppInfo comes from resolveDefault() and
 *   has no tags populated — are still tag-block-gated.
 * - Registers a LauncherApps callback so the list auto-refreshes on
 *   installs/removals; on removal also calls
 *   BlockingScheduleRepository.purgePackage to keep schedule junctions tidy.
 * - Resolves bottom-bar quick-access shortcuts: materializes a user override
 *   to an AppInfo, or falls back to the system default dialer/camera via
 *   PackageManager.resolveActivity (ULT-13, ULT-14)
 * - In-App Time Reminder pre-check (ULT-78): launchApp also consults
 *   SessionTimerRepository.isReminderActiveFor and SessionTimerCoordinator.
 *   When a reminder-enabled package is launched without an in-flight
 *   session it returns LaunchResult.SessionDurationRequired so the caller
 *   routes the user to the duration picker; once the picker starts a
 *   session and re-enters launchApp, isSessionActiveFor short-circuits
 *   this branch.
 * Covers: ULT-5, ULT-6, ULT-7, ULT-13, ULT-14, ULT-20, ULT-21, ULT-24, ULT-73, ULT-75, ULT-78
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
import com.ultimatefocus.app.domain.repository.BlockedTagRepository
import com.ultimatefocus.app.domain.repository.BlockingScheduleRepository
import com.ultimatefocus.app.domain.repository.SessionTimerRepository
import com.ultimatefocus.app.domain.repository.TagRepository
import com.ultimatefocus.app.domain.service.BlockingSourceCoordinator
import com.ultimatefocus.app.domain.service.Clock
import com.ultimatefocus.app.domain.service.SessionTimerCoordinator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val blockedTagRepository: BlockedTagRepository,
    private val coordinator: BlockingSourceCoordinator,
    private val tagRepository: TagRepository,
    private val clock: Clock,
    private val sessionTimerRepository: SessionTimerRepository,
    private val sessionTimerCoordinator: SessionTimerCoordinator,
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

    override fun getInstalledApps(): Flow<List<AppInfo>> {
        // Compose two 4-arg combines (max of the typed combine overload is 5)
        // into a single emission so the UI sees consistent state.
        val baseFlow = combine(
            _installedApps,
            favoriteDao.getFavoritePackageNames(),
            blockedAppRepository.observeActiveBlocks(),
            blockingScheduleRepository.observeActiveWindowEndByPackage(),
        ) { apps, favorites, manualBlocks, scheduledUntilByPkg ->
            BaseSnapshot(apps, favorites, manualBlocks, scheduledUntilByPkg)
        }
        val tagFlow = combine(
            tagRepository.observeAppToTagsMap(),
            blockedTagRepository.observeActiveBlockedPackagesByTags(),
        ) { tagsByApp, tagBlocksByApp -> tagsByApp to tagBlocksByApp }
        return combine(baseFlow, tagFlow) { base, (tagsByApp, tagBlocksByApp) ->
            val manualUntilByPkg =
                base.manualBlocks.associate { it.packageName to it.blockedUntilEpochMs }
            base.apps.map { app ->
                app.copy(
                    isFavorite = app.packageName in base.favorites,
                    blockedUntilEpochMs = resolveBlockedUntil(
                        app.packageName,
                        manualUntilByPkg,
                        base.scheduledUntilByPkg,
                        tagBlocksByApp,
                    ),
                    tags = tagsByApp[app.packageName] ?: emptyList(),
                )
            }
        }
    }

    private data class BaseSnapshot(
        val apps: List<AppInfo>,
        val favorites: List<String>,
        val manualBlocks: List<com.ultimatefocus.app.domain.model.BlockedApp>,
        val scheduledUntilByPkg: Map<String, Long>,
    )

    override fun searchApps(query: String): Flow<List<AppInfo>> =
        getInstalledApps().map { apps ->
            if (query.isBlank()) apps
            else apps.filter { it.label.contains(query, ignoreCase = true) }
        }

    override fun getFavoriteApps(): Flow<List<AppInfo>> {
        val baseFlow = combine(
            _installedApps,
            favoriteDao.getAllFavorites(),
            blockedAppRepository.observeActiveBlocks(),
            blockingScheduleRepository.observeActiveWindowEndByPackage(),
        ) { apps, favorites, manualBlocks, scheduledUntilByPkg ->
            FavoriteBaseSnapshot(apps, favorites, manualBlocks, scheduledUntilByPkg)
        }
        val tagFlow = combine(
            tagRepository.observeAppToTagsMap(),
            blockedTagRepository.observeActiveBlockedPackagesByTags(),
        ) { tagsByApp, tagBlocksByApp -> tagsByApp to tagBlocksByApp }
        return combine(baseFlow, tagFlow) { base, (tagsByApp, tagBlocksByApp) ->
            val favoriteMap = base.favorites.associateBy { it.packageName }
            val manualUntilByPkg =
                base.manualBlocks.associate { it.packageName to it.blockedUntilEpochMs }
            base.apps.filter { it.packageName in favoriteMap }
                .map { app ->
                    app.copy(
                        isFavorite = true,
                        favoriteOrder = favoriteMap[app.packageName]?.displayOrder ?: 0,
                        blockedUntilEpochMs = resolveBlockedUntil(
                            app.packageName,
                            manualUntilByPkg,
                            base.scheduledUntilByPkg,
                            tagBlocksByApp,
                        ),
                        tags = tagsByApp[app.packageName] ?: emptyList(),
                    )
                }
                .sortedBy { it.favoriteOrder }
        }
    }

    private data class FavoriteBaseSnapshot(
        val apps: List<AppInfo>,
        val favorites: List<FavoriteEntity>,
        val manualBlocks: List<com.ultimatefocus.app.domain.model.BlockedApp>,
        val scheduledUntilByPkg: Map<String, Long>,
    )

    /**
     * Resolution priority: manual > scheduled > tag-derived. Manual is the
     * most binding contract; scheduled is named/structured; tag is the
     * loosest. Null when the package is not currently blocked from any
     * source. ULT-75 tag-blocks already aggregate as MAX expiry across the
     * app's blocked tags inside observeActiveBlockedPackagesByTags.
     */
    private fun resolveBlockedUntil(
        packageName: String,
        manualUntilByPkg: Map<String, Long>,
        scheduledUntilByPkg: Map<String, Long>,
        tagBlocksByApp: Map<String, Long>,
    ): Long? = manualUntilByPkg[packageName]
        ?: scheduledUntilByPkg[packageName]
        ?: tagBlocksByApp[packageName]

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
        // Block check FIRST so a blocked app always surfaces the
        // BlockedAppActivity, never the session-duration picker. This
        // matters in particular for the EXIT_AND_BLOCK expiry mode: the
        // session that just ended writes a block back into
        // BlockedAppRepository, and the very next launch attempt must
        // see that block — not a fresh duration picker.
        //
        // Single block source — coordinator unifies manual + scheduled +
        // tag-derived blocks. Importantly the coordinator looks tag-blocks
        // up by packageName (not appInfo.tags), so shortcut launches whose
        // AppInfo comes from resolveDefault() — which doesn't populate
        // tags — are still gated correctly.
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
                is ActiveBlockInfo.Tag -> LaunchResult.Blocked(
                    packageName = appInfo.packageName,
                    appLabel = appInfo.label,
                    remainingMs = remaining,
                    durationLabel = info.durationLabel,
                    scheduleName = null,
                )
            }
        }

        // Not blocked. Session-timer pre-check (ULT-78): if the package has
        // the in-app reminder enabled and there is no session in progress,
        // route the user to the duration picker instead of launching. The
        // picker calls launchApp again after the session is started;
        // isSessionActiveFor short-circuits this branch on that second
        // call so the launch proceeds.
        if (
            !sessionTimerCoordinator.isSessionActiveFor(appInfo.packageName) &&
            sessionTimerRepository.isReminderActiveFor(appInfo.packageName)
        ) {
            // Read the freshest default mode here (suspending into DataStore)
            // so the picker is never bound with a stale Compose-state value.
            val defaultMode = sessionTimerRepository.observeSettings().first().expiryMode
            return LaunchResult.SessionDurationRequired(
                packageName = appInfo.packageName,
                appLabel = appInfo.label,
                defaultMode = defaultMode,
            )
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
