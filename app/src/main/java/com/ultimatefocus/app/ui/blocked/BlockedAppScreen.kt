/**
 * Full-screen reminder shown when the user taps a blocked app. Renders the
 * app label, a commitment-reinforcing message, a live-updating remaining
 * time, and a single "Return home" button — no unblock / override / skip
 * affordance. Auto-dismisses when the block expires.
 * Split into a stateful entry point (`BlockedAppScreen`) and a stateless
 * content composable (`BlockedAppContent`) so Compose tests can render the
 * UI without a Hilt-injected ViewModel.
 * Covers: ULT-21
 */
package com.ultimatefocus.app.ui.blocked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultimatefocus.app.util.BlockedTimeFormatter

@Composable
fun BlockedAppScreen(
    onDismiss: () -> Unit,
    viewModel: BlockedAppViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldFinish) {
        if (uiState.shouldFinish) onDismiss()
    }

    BlockedAppContent(
        appLabel = uiState.appLabel,
        remainingLabel = BlockedTimeFormatter.formatRemaining(uiState.remainingMs),
        onDismiss = onDismiss,
    )
}

@Composable
fun BlockedAppContent(
    appLabel: String,
    remainingLabel: String,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .testTag("blocked_app_screen"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "\uD83D\uDD12 $appLabel",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("blocked_app_label"),
            )

            Text(
                text = "This app is blocked.",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Time remaining: $remainingLabel",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("blocked_app_remaining"),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "You committed to staying focused. Return to what matters.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("blocked_return_home"),
            ) {
                Text(
                    text = "Return home",
                    fontSize = 18.sp,
                    color = Color.White,
                )
            }
        }
    }
}
