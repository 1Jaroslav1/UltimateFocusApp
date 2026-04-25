/**
 * Settings → "Blocking schedules" landing page. Renders an LazyColumn of
 * ScheduleCards plus an "Add" icon button in the top bar. Empty state
 * directs the user to the add affordance.
 *
 * In commit 1 (ULT-24) cards are non-clickable — the user can only create
 * via the top-right "+" button. Commit 2 (ULT-25) layers edit/delete by
 * making cards clickable so they navigate to the editor in edit mode.
 *
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.ui.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultimatefocus.app.ui.blocking.components.ScheduleCard

@Composable
fun BlockingSchedulesListScreen(
    onNavigateBack: () -> Unit,
    onAddSchedule: () -> Unit,
    onEditSchedule: ((Long) -> Unit)? = null,
    viewModel: BlockingSchedulesListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BlockingSchedulesListContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAddSchedule = onAddSchedule,
        onEditSchedule = onEditSchedule,
    )
}

@Composable
fun BlockingSchedulesListContent(
    uiState: BlockingSchedulesListUiState,
    onNavigateBack: () -> Unit,
    onAddSchedule: () -> Unit,
    onEditSchedule: ((Long) -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("blocking_schedules_screen"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 16.dp),
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Blocking schedules",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            IconButton(
                onClick = onAddSchedule,
                modifier = Modifier.testTag("add_schedule_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add schedule",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        if (uiState.schedules.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    .testTag("blocking_schedules_empty_state"),
            ) {
                Text(
                    text = "No schedules yet. Tap + to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.testTag("blocking_schedules_list"),
            ) {
                items(uiState.schedules, key = { it.schedule.id }) { row ->
                    ScheduleCard(
                        schedule = row.schedule,
                        isCurrentlyActive = row.isCurrentlyActive,
                        onClick = onEditSchedule?.let { handler -> { handler(row.schedule.id) } },
                    )
                }
            }
        }
    }
}
