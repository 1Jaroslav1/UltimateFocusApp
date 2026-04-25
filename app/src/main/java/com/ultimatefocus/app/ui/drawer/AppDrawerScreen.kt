/**
 * Full app drawer screen. Displays all installed apps alphabetically as
 * plain text labels (no icons). Accessible via swipe-left from the home
 * screen; dismissible by swiping right or pressing back (ULT-7).
 * When the auto-keyboard setting is enabled the search field gains focus
 * automatically on entry, prompting intentional typing (ULT-8).
 * Long-pressing a row routes through the block-aware context menu +
 * duration picker (ULT-20).
 * Covers: ULT-7, ULT-8, ULT-20
 */
package com.ultimatefocus.app.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.BlockDuration
import com.ultimatefocus.app.ui.blocked.BlockedAppActivity
import com.ultimatefocus.app.ui.components.AccessibilityPermissionPrompt
import com.ultimatefocus.app.ui.components.AppContextMenuDialog
import com.ultimatefocus.app.ui.components.BlockDurationPickerDialog
import com.ultimatefocus.app.ui.components.NotificationPermissionPrompt
import com.ultimatefocus.app.ui.drawer.components.AppListItem
import com.ultimatefocus.app.ui.drawer.components.SearchBar
import com.ultimatefocus.app.ui.home.BlockPromptStep
import com.ultimatefocus.app.ui.home.nextBlockPromptStep
import com.ultimatefocus.app.util.AccessibilityPermission
import com.ultimatefocus.app.util.NotificationListenerPermission

@Composable
fun AppDrawerScreen(
    isActive: Boolean = true,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: AppDrawerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val autoKeyboard by viewModel.autoKeyboard.collectAsStateWithLifecycle()

    var showContextMenu by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showBlockPicker by remember { mutableStateOf(false) }
    var pendingBlock by remember {
        mutableStateOf<Triple<AppInfo, BlockDuration, BlockPromptStep>?>(null)
    }
    val focusRequester = remember { FocusRequester() }
    val launchContext = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.blockedAttempts.collect { blocked ->
            val blockedUntilMs = System.currentTimeMillis() + blocked.remainingMs
            launchContext.startActivity(
                BlockedAppActivity.intent(
                    context = launchContext,
                    packageName = blocked.packageName,
                    appLabel = blocked.appLabel,
                    blockedUntilEpochMs = blockedUntilMs,
                    durationLabel = blocked.durationLabel,
                    scheduleName = blocked.scheduleName,
                ),
            )
        }
    }

    LaunchedEffect(autoKeyboard, isActive) {
        if (autoKeyboard && isActive) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("app_drawer_screen"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                focusRequester = focusRequester,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = androidx.compose.ui.graphics.Color.White,
                )
            }
        }

        when (val state = uiState) {
            is AppDrawerUiState.Loading -> {
                Text(
                    text = "Loading apps\u2026",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("loading_text"),
                )
            }
            is AppDrawerUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("app_list"),
                ) {
                    items(
                        items = state.apps,
                        key = { it.packageName },
                    ) { app ->
                        AppListItem(
                            appInfo = app,
                            onClick = { viewModel.launchApp(app) },
                            onLongClick = {
                                selectedApp = app
                                showContextMenu = true
                            },
                        )
                    }

                    if (state.apps.isEmpty()) {
                        item {
                            Text(
                                text = "No apps found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("empty_text"),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showContextMenu && selectedApp != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AppContextMenuDialog(
            app = selectedApp!!,
            nowEpochMs = System.currentTimeMillis(),
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onBlockApp = {
                showContextMenu = false
                showBlockPicker = true
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

    if (showBlockPicker && selectedApp != null) {
        BlockDurationPickerDialog(
            appLabel = selectedApp!!.label,
            onConfirm = { duration ->
                val app = selectedApp!!
                val step = nextBlockPromptStep(launchContext, from = BlockPromptStep.Accessibility)
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
                onGrantAccess = { AccessibilityPermission.openSettings(launchContext) },
                onContinueWithout = { /* fall through via onDismiss */ },
                onDismiss = {
                    val next = nextBlockPromptStep(launchContext, from = BlockPromptStep.Notification)
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
                onGrantAccess = { NotificationListenerPermission.openSettings(launchContext) },
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
}
