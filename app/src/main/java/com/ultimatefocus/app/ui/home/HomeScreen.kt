/**
 * Home screen composable — the launcher's primary surface.
 * Displays a text clock with battery arc (ULT-6) and a scrollable list of
 * favorite apps as plain text labels with no icons (ULT-6).
 * Swipe-left opens the full app drawer (ULT-7).
 * Swipe-up launches the configured web-search provider (ULT-16).
 * Swipe-down opens the notification shade (ULT-17).
 * A two-finger double-tap locks the device when the preference is enabled (ULT-18).
 * Pins a two-slot quick-access shortcut bar at the bottom that remains
 * visible even when favorites overflow (ULT-13).
 * Long-pressing a favorite opens a context menu that routes a "Block app…"
 * tap to the duration picker + confirmation flow (ULT-20) and a "Manage
 * tags…" tap to the TagPickerDialog scoped to that single app (ULT-73).
 * A tiny build-hash label in the bottom-right corner identifies the
 * currently installed build during on-device iteration.
 *
 * ULT-83 #2: hides the IME and clears focus on every ON_RESUME so the
 * keyboard does not linger after returning from a launched app.
 * ON_RESUME on HomeScreen only fires when Home itself resumes (returning
 * from another app), not on intra-pager swipes — so this does not race
 * the drawer's auto-keyboard requestFocus.
 *
 * Covers: ULT-6, ULT-7, ULT-13, ULT-16, ULT-17, ULT-18, ULT-20, ULT-73, ULT-83
 */
package com.ultimatefocus.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.LaunchedEffect
import com.ultimatefocus.app.BuildConfig
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.domain.model.HomeAlignment
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity
import com.ultimatefocus.app.ui.components.AccessibilityPermissionPrompt
import com.ultimatefocus.app.ui.components.AppContextMenuDialog
import com.ultimatefocus.app.ui.components.BlockDurationPickerDialog
import com.ultimatefocus.app.ui.components.NotificationPermissionPrompt
import com.ultimatefocus.app.ui.home.components.ClockWidget
import com.ultimatefocus.app.util.AccessibilityPermission
import com.ultimatefocus.app.util.NotificationListenerPermission
import com.ultimatefocus.app.ui.home.components.FavoritesList
import com.ultimatefocus.app.ui.home.components.QuickShortcutsBar
import com.ultimatefocus.app.ui.home.components.homeSwipeGestures
import com.ultimatefocus.app.ui.home.components.homeTwoFingerDoubleTap
import com.ultimatefocus.app.ui.tags.TagPickerDialog
import androidx.core.net.toUri

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onOpenManageTags: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteApps.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val leftShortcut by viewModel.leftShortcut.collectAsStateWithLifecycle()
    val rightShortcut by viewModel.rightShortcut.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    var showContextMenu by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showBlockPicker by remember { mutableStateOf(false) }
    var tagPickerPackages by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingBlock by remember {
        mutableStateOf<Triple<AppInfo, BlockDuration, BlockPromptStep>?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.blockedAttempts.collect { blocked ->
            val blockedUntilMs = System.currentTimeMillis() + blocked.remainingMs
            context.startActivity(
                BlockedAppActivity.intent(
                    context = context,
                    packageName = blocked.packageName,
                    appLabel = blocked.appLabel,
                    blockedUntilEpochMs = blockedUntilMs,
                    durationLabel = blocked.durationLabel,
                    scheduleName = blocked.scheduleName,
                ),
            )
        }
    }
    LaunchedEffect(Unit) {
        viewModel.sessionPicker.collect { event ->
            context.startActivity(
                com.ultimatefocus.app.ui.sessiontimer.SessionDurationPickerActivity.intent(
                    context = context,
                    packageName = event.packageName,
                    appLabel = event.appLabel,
                    defaultMode = event.defaultMode,
                ),
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .homeSwipeGestures(
                onSwipeUp = { viewModel.onSwipeUp() },
                onSwipeDown = { viewModel.onSwipeDown() },
            )
            .homeTwoFingerDoubleTap(
                enabled = preferences.twoFingerDoubleTapToLockEnabled,
                onTwoFingerDoubleTap = { viewModel.onTwoFingerDoubleTap() },
            )
            .testTag("home_screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = when (preferences.homeAlignment) {
                HomeAlignment.LEFT -> Alignment.Start
                HomeAlignment.CENTER -> Alignment.CenterHorizontally
                HomeAlignment.RIGHT -> Alignment.End
            },
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ClockWidget(
                    currentTime = uiState.currentTime,
                    batteryLevel = uiState.batteryLevel,
                    isCharging = uiState.isCharging,
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            FavoritesList(
                favorites = favorites,
                alignment = preferences.homeAlignment,
                onAppClick = { viewModel.launchApp(it) },
                onAppLongClick = { app ->
                    selectedApp = app
                    showContextMenu = true
                },
                modifier = Modifier.weight(1f),
            )

            if (favorites.isEmpty()) {
                Text(
                    text = "Swipe left for all apps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                        .testTag("swipe_hint"),
                )
            }

            QuickShortcutsBar(
                leftApp = leftShortcut,
                rightApp = rightShortcut,
                onTap = { viewModel.launchShortcut(it) },
                onLongPress = { /* wired in ULT-14 */ },
            )
        }

        if (showContextMenu && selectedApp != null) {
            AppContextMenuDialog(
                app = selectedApp!!,
                nowEpochMs = uiState.currentTime
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onBlockApp = {
                    showContextMenu = false
                    showBlockPicker = true
                },
                onManageTags = { app ->
                    showContextMenu = false
                    tagPickerPackages = listOf(app.packageName)
                },
                onAppInfo = { app ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${app.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                onDismiss = { showContextMenu = false },
            )
        }

        if (tagPickerPackages.isNotEmpty()) {
            TagPickerDialog(
                packageNames = tagPickerPackages,
                onManageTags = {
                    tagPickerPackages = emptyList()
                    onOpenManageTags()
                },
                onDismiss = {
                    tagPickerPackages = emptyList()
                    selectedApp = null
                },
            )
        }

        if (showBlockPicker && selectedApp != null) {
            BlockDurationPickerDialog(
                appLabel = selectedApp!!.label,
                onConfirm = { duration ->
                    val app = selectedApp!!
                    val step = nextBlockPromptStep(context, from = BlockPromptStep.Accessibility)
                    if (step == BlockPromptStep.None) {
                        viewModel.blockApp(app.packageName, duration)
                    } else {
                        pendingBlock = Triple(app, duration, step)
                    }
                },
                onDismiss = {
                    showBlockPicker = false
                    if (pendingBlock == null) selectedApp = null
                },
            )
        }

        val pending = pendingBlock
        if (pending != null) {
            when (pending.third) {
                BlockPromptStep.Accessibility -> AccessibilityPermissionPrompt(
                    appLabel = pending.first.label,
                    onGrantAccess = { AccessibilityPermission.openSettings(context) },
                    onContinueWithout = { /* fall through via onDismiss */ },
                    onDismiss = {
                        val next = nextBlockPromptStep(context, from = BlockPromptStep.Notification)
                        if (next == BlockPromptStep.None) {
                            viewModel.blockApp(pending.first.packageName, pending.second)
                            pendingBlock = null
                            selectedApp = null
                        } else {
                            pendingBlock = pending.copy(third = next)
                        }
                    },
                )
                BlockPromptStep.Notification -> NotificationPermissionPrompt(
                    appLabel = pending.first.label,
                    onGrantAccess = { NotificationListenerPermission.openSettings(context) },
                    onContinueWithout = { /* fall through via onDismiss */ },
                    onDismiss = {
                        viewModel.blockApp(pending.first.packageName, pending.second)
                        pendingBlock = null
                        selectedApp = null
                    },
                )
                BlockPromptStep.None -> Unit
            }
        }

        // Build-version tag. Shows the git HEAD short hash (or "dev" when
        // built outside git) so it's obvious which build is installed while
        // iterating on device-specific bugs. Opaque on purpose (alpha 0.35)
        // so it doesn't distract from the minimal layout.
        Text(
            text = BuildConfig.GIT_HASH,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("build_hash"),
        )
    }
}

/**
 * Sequence of permission prompts to show when the user commits a block:
 * Accessibility first (primary enforcement mechanism), then Notification
 * Listener (notification suppression). Either may be skipped when already
 * granted; the block itself always commits after the flow completes.
 */
enum class BlockPromptStep { Accessibility, Notification, None }

internal fun nextBlockPromptStep(
    context: android.content.Context,
    from: BlockPromptStep,
): BlockPromptStep {
    if (from <= BlockPromptStep.Accessibility &&
        !AccessibilityPermission.isGranted(context)
    ) return BlockPromptStep.Accessibility
    if (from <= BlockPromptStep.Notification &&
        !NotificationListenerPermission.isGranted(context)
    ) return BlockPromptStep.Notification
    return BlockPromptStep.None
}
