/**
 * Home screen composable — the launcher's primary surface.
 * Displays a text clock with battery arc (ULT-6) and a scrollable list of
 * favorite apps as plain text labels with no icons (ULT-6).
 * Swipe-left opens the full app drawer (ULT-7).
 * Swipe-up launches the configured web-search provider (ULT-16).
 * Swipe-down opens the notification shade (ULT-17).
 * A two-finger long-press locks the device when the preference is enabled (ULT-18).
 * A tiny build-hash label in the bottom-right corner identifies the
 * currently installed build during on-device iteration.
 * Covers: ULT-6, ULT-7, ULT-16, ULT-17, ULT-18
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.provider.Settings
import com.ultimatefocus.app.BuildConfig
import com.ultimatefocus.app.domain.model.AppInfo
import com.ultimatefocus.app.domain.model.HomeAlignment
import com.ultimatefocus.app.ui.components.FavoriteContextMenuDialog
import com.ultimatefocus.app.ui.home.components.ClockWidget
import com.ultimatefocus.app.ui.home.components.FavoritesList
import com.ultimatefocus.app.ui.home.components.homeMultiTouchDebug
import com.ultimatefocus.app.ui.home.components.homeSwipeGestures
import com.ultimatefocus.app.ui.home.components.homeTwoFingerLongPress
import androidx.core.net.toUri

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteApps.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showContextMenu by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .homeSwipeGestures(
                onSwipeUp = { viewModel.onSwipeUp() },
                onSwipeDown = { viewModel.onSwipeDown() },
            )
            .homeTwoFingerLongPress(
                enabled = preferences.twoFingerLongPressToLockEnabled,
                onTwoFingerLongPress = { viewModel.onTwoFingerLongPress() },
            )
            // Diagnostic: temporary multi-touch visibility while we debug
            // ULT-18. Remove once the long-press is verified working.
            .homeMultiTouchDebug(context)
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
            )

            Spacer(modifier = Modifier.weight(1f))

            if (favorites.isEmpty()) {
                Text(
                    text = "Swipe left for all apps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 32.dp)
                        .testTag("swipe_hint"),
                )
            }
        }

        if (showContextMenu && selectedApp != null) {
            FavoriteContextMenuDialog(
                app = selectedApp!!,
                onToggleFavorite = { viewModel.toggleFavorite(it) },
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
