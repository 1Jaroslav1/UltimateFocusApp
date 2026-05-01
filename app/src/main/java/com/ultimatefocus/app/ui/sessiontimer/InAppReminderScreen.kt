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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("in_app_reminder_screen"),
    ) {
        // The apps section (search + list) takes a fixed 48% of the available
        // screen height (raised from 40% in ULT-82 #4 — feedback round 1
        // asked for a roughly-20% taller list); the controls section takes
        // the remaining 52% and scrolls internally if it overflows on
        // small screens.
        val appsSectionHeight = maxHeight * 0.48f

        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(onNavigateBack = onNavigateBack)

            // Controls — scroll internally so the apps section stays pinned
            // at its 40% and is never pushed off-screen.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                CompactSettingsRow(
                    title = "In-app time reminder active",
                    trailing = {
                        Switch(
                            checked = settings.masterEnabled,
                            onCheckedChange = onSetMaster,
                            modifier = Modifier.testTag("in_app_reminder_master_switch"),
                        )
                    },
                )
                ThinDivider()

                DependentBlock(masterEnabled = settings.masterEnabled) {
                    CompactSettingsRow(
                        title = "Precision Mode",
                        subtitle = if (settings.precisionMode) "Active" else "Inactive",
                        onClick = if (settings.masterEnabled) onTogglePrecision else null,
                        modifier = Modifier.testTag("in_app_reminder_precision_row"),
                    )
                    ThinDivider()

                    CompactSettingsRow(
                        title = "Mindful Launch Delay",
                        trailing = {
                            Switch(
                                checked = settings.mindfulLaunchDelay,
                                enabled = settings.masterEnabled,
                                onCheckedChange = onSetMindfulLaunchDelay,
                                modifier = Modifier
                                    .testTag("in_app_reminder_mindful_delay_switch"),
                            )
                        },
                    )
                    ThinDivider()

                    CompactSectionHeader("When the session ends")
                    ExpiryModeRadioGroup(
                        selected = settings.expiryMode,
                        enabled = settings.masterEnabled,
                        onSelect = onSelectExpiryMode,
                    )
                }
            }

            // Apps section — fixed 40% height. Header + filter field + list
            // share this space; the list takes whatever is left after the
            // first two are laid out.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(appsSectionHeight)
                    .testTag("in_app_reminder_apps_section"),
            ) {
                ThinDivider()
                CompactSectionHeader("Apps")
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ThinDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
}

@Composable
private fun AppList(
    visibleApps: List<AppToggleRow>,
    totalAppCount: Int,
    masterEnabled: Boolean,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (totalAppCount == 0) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
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
            modifier = modifier
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
        modifier = modifier
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .testTag("in_app_reminder_app_row_${row.packageName}"),
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = com.ultimatefocus.app.util.UsageDurationFormatter
                .formatWeeklyUsage(row.weeklyUsageMillis),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .testTag("in_app_reminder_app_usage_${row.packageName}"),
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
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("in_app_reminder_mode_row_${option.mode.name}"),
        ) {
            RadioButton(
                selected = selected == option.mode,
                enabled = enabled,
                onClick = if (enabled) ({ onSelect(option.mode) }) else null,
                modifier = Modifier.testTag("in_app_reminder_mode_radio_${option.mode.name}"),
            )
            Text(
                text = option.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun CompactSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

/**
 * Compact variant of the standard settings row used elsewhere in the app.
 * Drops the multi-line subtitle support and tightens the vertical padding
 * so the In-App Reminder controls fit comfortably in the 60% above the
 * fixed-height apps section.
 */
@Composable
private fun CompactSettingsRow(
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
            .padding(horizontal = 24.dp, vertical = 8.dp),
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

private data class ReminderExpiryOption(
    val mode: SessionExpiryMode,
    val title: String,
)

private val EXPIRY_OPTIONS = listOf(
    ReminderExpiryOption(SessionExpiryMode.MINDFUL_EXTENSION_DELAY, "Mindful extension delay"),
    ReminderExpiryOption(SessionExpiryMode.EXIT_APP, "Exit app when time is over"),
    ReminderExpiryOption(SessionExpiryMode.EXIT_AND_BLOCK, "Exit app and block"),
    ReminderExpiryOption(SessionExpiryMode.ONLY_REMINDER, "Only reminder"),
)

private const val DISABLED_ALPHA = 0.4f
