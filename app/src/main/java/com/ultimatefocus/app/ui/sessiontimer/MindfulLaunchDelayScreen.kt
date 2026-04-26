/**
 * Composable for the mindful-launch-delay overlay (ULT-79). Renders a
 * full-screen card with a circular progress indicator and a calm
 * "Take a breath…" message above the target app's label. Non-cancellable
 * — the back press is suppressed by the host activity.
 *
 * Stateless `MindfulLaunchDelayContent` is split out so Compose-rule
 * tests can drive it without spinning up a ViewModel or running the
 * actual delay coroutine.
 *
 * Covers: ULT-79
 */
package com.ultimatefocus.app.ui.sessiontimer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
fun MindfulLaunchDelayScreen(viewModel: MindfulLaunchDelayViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MindfulLaunchDelayContent(state = state)
}

@Composable
fun MindfulLaunchDelayContent(state: MindfulLaunchDelayUiState) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("mindful_launch_delay_overlay"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(96.dp)
                    .testTag("mindful_launch_delay_indicator"),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Take a breath",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val target = state.appLabel.ifBlank { "this app" }
            Text(
                text = "$target will open in a moment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("mindful_launch_delay_subtitle"),
            )
        }
    }
}
