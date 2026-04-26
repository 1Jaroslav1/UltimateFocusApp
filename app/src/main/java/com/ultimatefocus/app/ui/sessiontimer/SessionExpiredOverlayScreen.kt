/**
 * Composable for the session-expired overlay (ULT-81 / Mode 1). Renders
 * the "Time's up" headline, a circular countdown of secondsRemaining,
 * and an Extend button that activates after the forced wait. Stateless
 * `SessionExpiredOverlayContent` lets the Compose-rule test drive the
 * UI directly without spinning up a ViewModel or running the timer.
 *
 * Covers: ULT-81
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SessionExpiredOverlayScreen(
    viewModel: SessionExpiredOverlayViewModel,
    onCancel: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SessionExpiredOverlayContent(
        state = state,
        onExtend = viewModel::extend,
        onCancel = onCancel,
    )
}

@Composable
fun SessionExpiredOverlayContent(
    state: SessionExpiredOverlayUiState,
    onExtend: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("session_expired_overlay"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Time's up",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val target = state.appLabel.ifBlank { "this app" }
            Text(
                text = "Your session in $target has ended.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(40.dp))

            CircularProgressIndicator(
                modifier = Modifier
                    .size(96.dp)
                    .testTag("session_expired_countdown_indicator"),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (state.canExtend) {
                    "You can extend now."
                } else {
                    "${state.secondsRemaining}s before extension is allowed"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("session_expired_countdown_label"),
            )
            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onExtend,
                enabled = state.canExtend,
                modifier = Modifier.testTag("session_expired_extend_button"),
            ) { Text("Extend session") }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.testTag("session_expired_cancel_button"),
            ) { Text("Done") }
        }
    }
}
