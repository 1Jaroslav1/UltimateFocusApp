/**
 * Composable for the session-duration picker (ULT-78). Layout per AC:
 *  - title: "How much time do you want to spend in [App Label]?"
 *  - 4 preset buttons: 1 / 5 / 10 / 15 minutes.
 *  - downward arrow at the bottom; tapping expands the modal to reveal
 *    a custom-minutes input field (clamped to 1..240) and a per-session
 *    expiry-mode radio override of the global default.
 *  - Tapping a preset OR confirming a valid custom value commits the
 *    duration. Anything else (back press, dismiss) cancels.
 *
 * Split into a Hilt-bound entry (`SessionDurationPickerScreen`) and a
 * stateless `SessionDurationPickerContent` so Compose-rule tests can
 * exercise the UI directly without a ViewModel.
 *
 * Covers: ULT-78
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultimatefocus.app.domain.model.SessionDurationMinutes
import com.ultimatefocus.app.domain.model.SessionExpiryMode

@Composable
fun SessionDurationPickerScreen(
    viewModel: SessionDurationPickerViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SessionDurationPickerContent(
        state = state,
        onDismiss = onDismiss,
        onConfirmPreset = viewModel::confirmPreset,
        onCustomMinutesChange = viewModel::onCustomMinutesChange,
        onConfirmCustom = viewModel::confirmCustom,
        onSelectMode = viewModel::onSelectExpiryMode,
        onToggleExpanded = viewModel::toggleExpanded,
    )
}

@Composable
fun SessionDurationPickerContent(
    state: SessionDurationPickerUiState,
    onDismiss: () -> Unit,
    onConfirmPreset: (Int) -> Unit,
    onCustomMinutesChange: (String) -> Unit,
    onConfirmCustom: () -> Unit,
    onSelectMode: (SessionExpiryMode) -> Unit,
    onToggleExpanded: () -> Unit,
) {
    BackHandler(enabled = true) { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("session_duration_picker"),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "How much time do you want to spend in ${state.appLabel.ifBlank { "this app" }}?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Surface the configured default expiry mode here so the
                // user knows what will happen at session end without
                // expanding the panel. The expanded panel still allows a
                // per-session override.
                Text(
                    text = "At session end: ${labelFor(state.selectedMode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_picker_default_mode_label"),
                )
                Spacer(modifier = Modifier.height(20.dp))

                PresetGrid(onConfirm = onConfirmPreset)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier.testTag("session_picker_expand_arrow"),
                    ) {
                        Icon(
                            imageVector = if (state.expanded) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                            contentDescription = if (state.expanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                if (state.expanded) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomMinutesField(
                        value = state.customMinutes,
                        valid = state.customMinutesValid,
                        onValueChange = onCustomMinutesChange,
                        onConfirm = onConfirmCustom,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Behavior when this session ends",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ExpiryModeRadioGroup(
                        selected = state.selectedMode,
                        onSelect = onSelectMode,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("session_picker_cancel_button"),
                    ) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun PresetGrid(onConfirm: (Int) -> Unit) {
    // 2 rows × 2 buttons: row 1 = 1 min / 5 min, row 2 = 10 min / 15 min.
    // Wider buttons read better than a single 4-up strip and match the
    // user's preferred layout.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SessionDurationMinutes.PRESETS.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowItems.forEach { minutes ->
                    PresetButton(
                        minutes = minutes,
                        onClick = { onConfirm(minutes) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PresetButton(minutes: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
        border = BorderStroke(1.dp, Color.White),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
        ),
        modifier = modifier
            .height(40.dp)
            .testTag("session_picker_preset_${minutes}"),
    ) {
        Text(text = "$minutes min", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CustomMinutesField(
    value: String,
    valid: Boolean,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    // Single-row compact input. The OutlinedTextField is constrained to
    // 48dp tall so it stops looking out of proportion to the Start
    // button next to it; a short placeholder ("min") keeps the
    // narrower field readable.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = "min",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            singleLine = true,
            isError = value.isNotEmpty() && !valid,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("session_picker_custom_field"),
        )
        OutlinedButton(
            onClick = onConfirm,
            enabled = valid,
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
            border = BorderStroke(1.dp, Color.White),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .height(40.dp)
                .testTag("session_picker_custom_confirm"),
        ) { Text(text = "Start", style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun ExpiryModeRadioGroup(
    selected: SessionExpiryMode,
    onSelect: (SessionExpiryMode) -> Unit,
) {
    PICKER_OPTIONS.forEach { option ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(option.mode) }
                .padding(vertical = 6.dp)
                .testTag("session_picker_mode_${option.mode.name}"),
        ) {
            RadioButton(
                selected = selected == option.mode,
                onClick = { onSelect(option.mode) },
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

private fun labelFor(mode: SessionExpiryMode): String =
    PICKER_OPTIONS.firstOrNull { it.mode == mode }?.title ?: mode.name

private data class PickerExpiryOption(val mode: SessionExpiryMode, val title: String)

private val PICKER_OPTIONS = listOf(
    PickerExpiryOption(SessionExpiryMode.MINDFUL_EXTENSION_DELAY, "Mindful extension delay"),
    PickerExpiryOption(SessionExpiryMode.EXIT_APP, "Exit app when time is over"),
    PickerExpiryOption(SessionExpiryMode.EXIT_AND_BLOCK, "Exit app and block"),
    PickerExpiryOption(SessionExpiryMode.ONLY_REMINDER, "Only reminder"),
)
