/**
 * Composable for the session-expired overlay (ULT-81 / Mode 1 — mindful
 * extension delay). Renders a determinate circular loader that fills
 * 0% → 100% over the forced wait, mirroring the mindful-launch-delay
 * overlay so both surfaces feel consistent. There is intentionally NO
 * "Extend" button: when the loader hits 100% the host activity
 * automatically launches the duration picker for a fresh session
 * commitment. The friction is the wait, not an extra tap.
 *
 * Stateless `SessionExpiredOverlayContent` lets the Compose-rule test
 * exercise the UI directly without a ViewModel or actual countdown.
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
fun SessionExpiredOverlayScreen(viewModel: SessionExpiredOverlayViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SessionExpiredOverlayContent(state = state)
}

@Composable
fun SessionExpiredOverlayContent(state: SessionExpiredOverlayUiState) {
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
            // Determinate progress: dot at start (0%), full ring at 100%.
            CircularProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .size(96.dp)
                    .testTag("session_expired_countdown_indicator"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                strokeWidth = 6.dp,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Take a moment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val target = state.appLabel.ifBlank { "this app" }
            Text(
                text = "Your session in $target ended. You'll be asked again in a moment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("session_expired_subtitle"),
            )
        }
    }
}
