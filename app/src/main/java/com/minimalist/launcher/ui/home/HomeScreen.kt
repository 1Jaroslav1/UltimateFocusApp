/**
 * Home screen composable — the launcher's primary surface.
 * Displays a text clock with battery arc (ULT-6) and a scrollable list of
 * favorite apps as plain text labels with no icons (ULT-6).
 * Swipe-down opens the notification shade; swipe-left navigates to the app
 * drawer (wired in ULT-7).
 * Covers: ULT-6
 */
package com.minimalist.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalist.launcher.domain.model.AppInfo
import com.minimalist.launcher.domain.model.HomeAlignment
import com.minimalist.launcher.ui.home.components.ClockWidget
import com.minimalist.launcher.ui.home.components.FavoritesList
import java.lang.reflect.Method

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
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 40) {
                        // Swipe down → expand notification shade
                        try {
                            val service = context.getSystemService("statusbar")
                            val clazz = Class.forName("android.app.StatusBarManager")
                            val method: Method = clazz.getMethod("expandNotificationsPanel")
                            method.invoke(service)
                        } catch (_: Exception) { /* not available on all devices */ }
                    }
                }
            }
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

            ClockWidget(
                currentTime = uiState.currentTime,
                batteryLevel = uiState.batteryLevel,
                isCharging = uiState.isCharging,
            )

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
            AlertDialog(
                onDismissRequest = { showContextMenu = false },
                title = { Text(selectedApp!!.label) },
                text = null,
                confirmButton = {
                    TextButton(onClick = {
                        selectedApp?.let { viewModel.toggleFavorite(it) }
                        showContextMenu = false
                    }) {
                        Text(
                            if (selectedApp?.isFavorite == true) "Remove from Favorites"
                            else "Add to Favorites",
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showContextMenu = false }) { Text("Cancel") }
                },
            )
        }
    }
}
