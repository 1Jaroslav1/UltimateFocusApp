/**
 * Full app drawer screen. Displays all installed apps alphabetically as
 * plain text labels (no icons). Accessible via swipe-left from the home
 * screen; dismissible by swiping right or pressing back.
 * Covers: ULT-7
 */
package com.minimalist.launcher.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalist.launcher.domain.model.AppInfo
import com.minimalist.launcher.ui.drawer.components.AppListItem
import com.minimalist.launcher.ui.drawer.components.SearchBar

@Composable
fun AppDrawerScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppDrawerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showContextMenu by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var totalDragX by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { totalDragX = 0f },
                    onDragCancel = { totalDragX = 0f },
                ) { _, dragAmount ->
                    totalDragX += dragAmount
                    if (totalDragX > 120f) {
                        totalDragX = 0f
                        onNavigateBack()
                    }
                }
            }
            .testTag("app_drawer_screen"),
    ) {
        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
        )

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
