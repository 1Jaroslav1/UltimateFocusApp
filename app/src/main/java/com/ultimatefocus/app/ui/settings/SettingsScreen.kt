/**
 * Settings screen. Exposes toggles for dark theme and auto-keyboard on app drawer
 * entry, a shortcut to the system launcher selector, and a Gestures section
 * controlling the swipe-up web-search provider (ULT-16) and the
 * two-finger-double-tap-to-lock preference (ULT-18). Provides entry points to
 * the recurring blocking-schedule list (ULT-24) and the Manage Tags screen
 * (ULT-72).
 * Covers: ULT-8, ULT-16, ULT-18, ULT-24, ULT-72
 */
package com.ultimatefocus.app.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import com.ultimatefocus.app.receiver.LockDeviceAdminReceiver
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultimatefocus.app.domain.service.WebSearchProvider

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBlockingSchedules: () -> Unit = {},
    onOpenManageTags: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
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
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        SectionHeader("Appearance")

        SettingsRow(
            title = "Dark Theme",
            trailing = {
                Switch(
                    checked = preferences.isDarkTheme,
                    onCheckedChange = { viewModel.toggleDarkTheme() },
                    modifier = Modifier.testTag("dark_theme_switch"),
                )
            },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        SectionHeader("App Drawer")

        SettingsRow(
            title = "Auto-Keyboard on Open",
            subtitle = "Keyboard appears automatically when you open the app list, " +
                "encouraging you to search intentionally rather than browse casually.",
            trailing = {
                Switch(
                    checked = preferences.autoKeyboard,
                    onCheckedChange = { viewModel.toggleAutoKeyboard() },
                    modifier = Modifier.testTag("auto_keyboard_switch"),
                )
            },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        SectionHeader("Gestures")

        SettingsRow(
            title = "Two-finger double-tap to lock",
            subtitle = "Tap the home screen twice in quick succession with " +
                "two fingers to lock the device. Requires device-admin " +
                "permission on first enable.",
            trailing = {
                Switch(
                    checked = preferences.twoFingerDoubleTapToLockEnabled,
                    onCheckedChange = { viewModel.setTwoFingerDoubleTapToLock(it) },
                    modifier = Modifier.testTag("two_finger_double_tap_to_lock_switch"),
                )
            },
        )

        // A direct "grant permission" row, always tappable. Needed because on
        // newer Android / some OEMs the startActivity call SettingsViewModel
        // fires when the switch flips is silently blocked (non-Activity
        // context), so the user never sees the admin dialog and has no way
        // to grant it. Launching from here uses LocalContext.current which
        // is the Activity, so the dialog comes up reliably.
        // Subtitle re-reads isLockPermissionGranted() on each composition so
        // the row shows ✓ Granted / ✗ Not granted without a manual reload.
        val adminGranted = viewModel.isLockPermissionGranted()
        SettingsRow(
            title = "Grant screen-lock permission",
            subtitle = if (adminGranted) {
                "✓ Granted. The two-finger double-tap will now lock the device."
            } else {
                "✗ Not granted — tap here to open the device-admin prompt. " +
                    "Required before the two-finger double-tap can lock the screen."
            },
            onClick = {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(
                        DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        ComponentName(context, LockDeviceAdminReceiver::class.java),
                    )
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Grant screen-lock permission so the two-finger " +
                            "double-tap on the home screen can lock the device.",
                    )
                }
                context.startActivity(intent)
            },
            modifier = Modifier.testTag("grant_lock_permission_row"),
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        var showProviderDialog by remember { mutableStateOf(false) }
        val providerLabel = providerLabelFor(preferences.searchProviderPackage, viewModel)

        SettingsRow(
            title = "Swipe-up search provider",
            subtitle = providerLabel,
            onClick = { showProviderDialog = true },
            modifier = Modifier.testTag("search_provider_row"),
        )

        if (showProviderDialog) {
            SearchProviderPickerDialog(
                currentSelection = preferences.searchProviderPackage,
                providers = remember { viewModel.availableSearchProviders() },
                onSelect = { pkg ->
                    viewModel.updateSearchProviderPackage(pkg)
                    showProviderDialog = false
                },
                onDismiss = { showProviderDialog = false },
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        SectionHeader("Blocking")

        SettingsRow(
            title = "Blocking schedules",
            subtitle = "Automatically block selected apps during recurring time windows.",
            onClick = onNavigateToBlockingSchedules,
            modifier = Modifier.testTag("blocking_schedules_row"),
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        SectionHeader("Tags")

        SettingsRow(
            title = "Manage tags",
            subtitle = "Create up to 10 color-coded tags to categorize your apps.",
            onClick = onOpenManageTags,
            modifier = Modifier.testTag("manage_tags_settings_row"),
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        SectionHeader("Launcher")

        SettingsRow(
            title = "Set as Default Launcher",
            subtitle = "Open system settings to change your default home screen app.",
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_HOME_SETTINGS)
                        .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
                )
            },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        SectionHeader("About")

        SettingsRow(title = "Minimalist Launcher", subtitle = "Version 1.0.0")

        Spacer(modifier = Modifier.height(32.dp))
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

/**
 * Subtitle shown under the "Swipe-up search provider" row — either
 * "System default" or the label of the currently selected installed app (ULT-16).
 */
@Composable
private fun providerLabelFor(
    selectedPackage: String?,
    viewModel: SettingsViewModel,
): String {
    if (selectedPackage == null) return "System default"
    val match = remember(selectedPackage) {
        viewModel.availableSearchProviders().firstOrNull { it.packageName == selectedPackage }
    }
    return match?.label ?: selectedPackage
}

/**
 * Picker for the swipe-up search provider rendered in the launcher's
 * default app modal style. A "System default" entry is prepended so the
 * user can opt out of a specific app (ULT-16). The currently-selected
 * row is prefixed with "✓ " so the dark, text-only modal can express
 * selection without a Material RadioButton.
 */
@Composable
private fun SearchProviderPickerDialog(
    currentSelection: String?,
    providers: List<WebSearchProvider>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    com.ultimatefocus.app.ui.components.DefaultAppModal(
        header = "Search provider",
        onDismiss = onDismiss,
        modifier = Modifier.testTag("search_provider_dialog"),
    ) {
        com.ultimatefocus.app.ui.components.DefaultModalAction(
            text = providerActionLabel("System default", currentSelection == null),
            onClick = {
                onSelect(null)
                onDismiss()
            },
            modifier = Modifier.testTag("provider_option_system_default"),
        )
        providers.forEach { provider ->
            com.ultimatefocus.app.ui.components.DefaultModalAction(
                text = providerActionLabel(
                    provider.label,
                    currentSelection == provider.packageName,
                ),
                onClick = {
                    onSelect(provider.packageName)
                    onDismiss()
                },
                modifier = Modifier.testTag("provider_option_${provider.packageName}"),
            )
        }

        androidx.compose.material3.HorizontalDivider(
            color = androidx.compose.ui.graphics.Color.White,
            thickness = 1.dp,
        )

        com.ultimatefocus.app.ui.components.DefaultModalAction(
            text = "Close",
            onClick = onDismiss,
            modifier = Modifier.testTag("provider_option_close"),
        )
    }
}

private fun providerActionLabel(label: String, selected: Boolean): String =
    if (selected) "✓  $label" else "    $label"
