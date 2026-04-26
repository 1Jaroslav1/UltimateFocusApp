/**
 * Settings → "In-App Reminder" landing page.
 *
 * Top-down layout per the ULT-76 / ULT-77 acceptance criteria:
 *  1. Top bar with back arrow and title.
 *  2. Master switch — "In-app time reminder active". When OFF, every
 *     control below greys out (alpha 0.4) and is disabled. The greying
 *     is applied uniformly via `enabled`/`alpha` so the page reads as
 *     "everything past the master is dependent on it".
 *  3. Precision Mode row — current state ("Active"/"Inactive") shown as
 *     the subtitle; tapping toggles. (Behavior of precision mode itself
 *     is owned by US-2.4.2; here we only persist the flag.)
 *  4. Mindful Launch Delay switch.
 *  5. Behavior-mode radio group (4 options) with explanatory subtitles.
 *  6. App-list section (ULT-77): an AppFilterField (shared with
 *     AppBrowseSheet, satisfying "same component as all-apps search")
 *     plus a vertically-scrolling per-app list. Each row has the app
 *     label and a Switch whose state mirrors the per-app opt-in. When
 *     master is OFF the toggles are disabled and visually inert; the
 *     filter text field continues to function so the user can still
 *     audit the current set.
 *
 * Split into a Hilt-bound entry (`InAppReminderScreen`) and a stateless
 * `InAppReminderContent` so Compose-rule tests can drive the UI without
 * spinning up a ViewModel.
 *
 * Covers: ULT-76, ULT-77
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultimatefocus.app.domain.model.InAppReminderSettings
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.ui.components.AppFilterField

@Composable
fun InAppReminderScreen(
    onNavigateBack: () -> Unit,
    viewModel: InAppReminderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InAppReminderContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onSetMaster = viewModel::setMasterEnabled,
        onTogglePrecision = viewModel::togglePrecisionMode,
        onSetMindfulLaunchDelay = viewModel::setMindfulLaunchDelay,
        onSelectExpiryMode = viewModel::setExpiryMode,
        onQueryChange = viewModel::setQuery,
        onAppToggle = viewModel::setAppEnabled,
    )
}

@Composable
fun InAppReminderContent(
    state: InAppReminderUiState,
    onNavigateBack: () -> Unit,
    onSetMaster: (Boolean) -> Unit,
    onTogglePrecision: () -> Unit,
    onSetMindfulLaunchDelay: (Boolean) -> Unit,
    onSelectExpiryMode: (SessionExpiryMode) -> Unit,
    onQueryChange: (String) -> Unit,
    onAppToggle: (String, Boolean) -> Unit,
) {
    val settings = state.settings
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("in_app_reminder_screen"),
    ) {
        TopBar(onNavigateBack = onNavigateBack)

        SettingsRow(
            title = "In-app time reminder active",
            subtitle = "Prompt yourself to commit a duration before opening selected apps.",
            trailing = {
                Switch(
                    checked = settings.masterEnabled,
                    onCheckedChange = onSetMaster,
                    modifier = Modifier.testTag("in_app_reminder_master_switch"),
                )
            },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        DependentBlock(masterEnabled = settings.masterEnabled) {
            SettingsRow(
                title = "Precision Mode",
                subtitle = if (settings.precisionMode) "Active" else "Inactive",
                onClick = if (settings.masterEnabled) onTogglePrecision else null,
                modifier = Modifier.testTag("in_app_reminder_precision_row"),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            SettingsRow(
                title = "Mindful Launch Delay",
                subtitle = "Show a brief loading animation before each opted-in app opens.",
                trailing = {
                    Switch(
                        checked = settings.mindfulLaunchDelay,
                        enabled = settings.masterEnabled,
                        onCheckedChange = onSetMindfulLaunchDelay,
                        modifier = Modifier.testTag("in_app_reminder_mindful_delay_switch"),
                    )
                },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            SectionHeader("When the session ends")
            ExpiryModeRadioGroup(
                selected = settings.expiryMode,
                enabled = settings.masterEnabled,
                onSelect = onSelectExpiryMode,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        }

        SectionHeader("Apps")
        AppFilterField(
            query = state.query,
            onQueryChange = onQueryChange,
            testTag = "in_app_reminder_filter_field",
        )

        Spacer(modifier = Modifier.height(8.dp))

        AppList(
            visibleApps = state.visibleApps,
            totalAppCount = state.totalAppCount,
            masterEnabled = settings.masterEnabled,
            onToggle = onAppToggle,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AppList(
    visibleApps: List<AppToggleRow>,
    totalAppCount: Int,
    masterEnabled: Boolean,
    onToggle: (String, Boolean) -> Unit,
) {
    if (totalAppCount == 0) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .testTag("in_app_reminder_apps_loading"),
        ) {
            Text(
                text = "Loading apps…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    if (visibleApps.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .testTag("in_app_reminder_apps_empty"),
        ) {
            Text(
                text = "No apps match your search.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 480.dp)
            .alpha(if (masterEnabled) 1f else DISABLED_ALPHA)
            .testTag("in_app_reminder_apps_list"),
    ) {
        items(visibleApps, key = { it.packageName }) { row ->
            AppRow(row = row, masterEnabled = masterEnabled, onToggle = onToggle)
        }
    }
}

@Composable
private fun AppRow(
    row: AppToggleRow,
    masterEnabled: Boolean,
    onToggle: (String, Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag("in_app_reminder_app_row_${row.packageName}"),
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = row.enabled,
            enabled = masterEnabled,
            onCheckedChange = { onToggle(row.packageName, it) },
            modifier = Modifier.testTag("in_app_reminder_app_switch_${row.packageName}"),
        )
    }
}

@Composable
private fun TopBar(onNavigateBack: () -> Unit) {
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
            text = "In-App Reminder",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * Wraps content that should grey out when the master toggle is off. Alpha
 * keeps the rows visible (so the user can still read what they would be
 * configuring) but signals "inert" — and child controls additionally pass
 * `enabled = masterEnabled` so they cannot accept input.
 */
@Composable
private fun DependentBlock(
    masterEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (masterEnabled) 1f else DISABLED_ALPHA),
    ) { content() }
}

@Composable
private fun ExpiryModeRadioGroup(
    selected: SessionExpiryMode,
    enabled: Boolean,
    onSelect: (SessionExpiryMode) -> Unit,
) {
    EXPIRY_OPTIONS.forEach { option ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (enabled) Modifier.clickable { onSelect(option.mode) } else Modifier,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("in_app_reminder_mode_row_${option.mode.name}"),
        ) {
            RadioButton(
                selected = selected == option.mode,
                enabled = enabled,
                onClick = if (enabled) ({ onSelect(option.mode) }) else null,
                modifier = Modifier.testTag("in_app_reminder_mode_radio_${option.mode.name}"),
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        trailing?.invoke()
    }
}

private data class ReminderExpiryOption(
    val mode: SessionExpiryMode,
    val title: String,
    val subtitle: String,
)

private val EXPIRY_OPTIONS = listOf(
    ReminderExpiryOption(
        mode = SessionExpiryMode.MINDFUL_EXTENSION_DELAY,
        title = "Mindful extension delay",
        subtitle = "Force a short wait before allowing a session extension.",
    ),
    ReminderExpiryOption(
        mode = SessionExpiryMode.EXIT_APP,
        title = "Exit app when time is over",
        subtitle = "Automatically return to the home screen when the timer hits zero.",
    ),
    ReminderExpiryOption(
        mode = SessionExpiryMode.EXIT_AND_BLOCK,
        title = "Exit app and block",
        subtitle = "Close the app and block it for the same duration just used.",
    ),
    ReminderExpiryOption(
        mode = SessionExpiryMode.ONLY_REMINDER,
        title = "Only reminder",
        subtitle = "Show a notification — take no automatic action.",
    ),
)

private const val DISABLED_ALPHA = 0.4f
