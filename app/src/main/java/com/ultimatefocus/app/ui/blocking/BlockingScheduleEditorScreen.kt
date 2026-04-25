/**
 * Editor for creating or modifying a BlockingSchedule. The editor is reused
 * for both flows; ULT-24 covers the create branch (no Delete button) while
 * ULT-25 layers the edit/delete branch and the "currently active → buttons
 * disabled" guard.
 *
 * Sections (top → bottom):
 *  - Name field
 *  - Start time row (Material3 TimePicker dialog)
 *  - End time row
 *  - DayChipRow (7 day filter chips)
 *  - "Select apps (N selected)" row → AppMultiSelectSheet
 *  - Save button (and Delete in edit mode)
 *  - Helper subtitle when isCurrentlyActive flips Save/Delete to disabled.
 *
 * Uses Material3 `TimePickerDialog` style scaffolding (AlertDialog wrapper)
 * since Compose Material3 doesn't ship a turn-key TimePickerDialog yet.
 *
 * Covers: ULT-24, ULT-25
 */
package com.ultimatefocus.app.ui.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultimatefocus.app.ui.blocking.components.AppMultiSelectSheet
import com.ultimatefocus.app.ui.blocking.components.DayChipRow
import com.ultimatefocus.app.ui.blocking.components.DeleteScheduleConfirmDialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockingScheduleEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: BlockingScheduleEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.Saved, EditorEvent.Deleted -> onNavigateBack()
                is EditorEvent.Error -> snackbarHostState.showSnackbar(event.message)
                is EditorEvent.LoadError -> {
                    snackbarHostState.showSnackbar(event.message)
                    onNavigateBack()
                }
            }
        }
    }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showAppSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .testTag("blocking_schedule_editor_screen"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = if (uiState.isEditMode) "Edit schedule" else "New schedule",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::setName,
            label = { Text("Name") },
            singleLine = true,
            isError = uiState.nameError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .testTag("schedule_name_field"),
        )

        SectionHeader("Time")
        TimeRow(
            label = "Start",
            time = uiState.start,
            onClick = { showStartPicker = true },
            testTag = "schedule_start_time_row",
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        TimeRow(
            label = "End",
            time = uiState.end,
            onClick = { showEndPicker = true },
            testTag = "schedule_end_time_row",
        )
        if (uiState.timeError) {
            Text(
                text = "Start and end time must differ.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        SectionHeader("Days of week")
        DayChipRow(
            selectedDays = uiState.days,
            onToggleDay = viewModel::toggleDay,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        if (uiState.daysError) {
            Text(
                text = "Pick at least one day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        SectionHeader("Apps")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAppSheet = true }
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .testTag("schedule_apps_row"),
        ) {
            Text(
                text = if (uiState.packageNames.isEmpty()) "Select apps"
                else "${uiState.packageNames.size} selected",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (uiState.appsError) {
            Text(
                text = "Pick at least one app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        if (uiState.isCurrentlyActive) {
            Text(
                text = "This schedule is currently active. Save and Delete are disabled until the window ends.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .testTag("schedule_active_subtitle"),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Button(
                onClick = viewModel::onSave,
                enabled = uiState.canSave,
                modifier = Modifier
                    .weight(1f)
                    .testTag("schedule_save_button"),
            ) {
                Text(text = "Save")
            }
            if (uiState.isEditMode) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = uiState.canDelete,
                    modifier = Modifier.testTag("schedule_delete_button"),
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        SnackbarHost(hostState = snackbarHostState)
    }

    if (showStartPicker) {
        TimePickerDialog(
            initial = uiState.start,
            onConfirm = { time ->
                viewModel.setStart(time)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false },
            tag = "schedule_start_time_picker",
        )
    }
    if (showEndPicker) {
        TimePickerDialog(
            initial = uiState.end,
            onConfirm = { time ->
                viewModel.setEnd(time)
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false },
            tag = "schedule_end_time_picker",
        )
    }
    if (showAppSheet) {
        AppMultiSelectSheet(
            apps = installedApps,
            selected = uiState.packageNames,
            onToggle = viewModel::togglePackage,
            onDismiss = { showAppSheet = false },
        )
    }
    if (showDeleteConfirm) {
        DeleteScheduleConfirmDialog(
            scheduleName = uiState.name,
            onConfirm = {
                viewModel.onDelete()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

@Composable
private fun TimeRow(
    label: String,
    time: LocalTime,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(testTag),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = time.format(TIME_FORMATTER),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    tag: String,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(tag),
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = { TimePicker(state = state) },
    )
}
