/**
 * Single-line text input used to filter app lists by label. Same visual
 * treatment used by the schedule-editor app browser (ULT-24) and the new
 * In-App Reminder app list (ULT-77) — extracted into a shared component
 * so both surfaces stay in lockstep visually and behaviorally and the
 * "same component as all-apps search" requirement of ULT-77 is satisfied
 * by an actual shared symbol rather than copy-paste.
 *
 * Stateless: caller owns the query string. testTag is parameterized so
 * different surfaces can target distinct test selectors.
 *
 * Covers: ULT-24, ULT-77
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun AppFilterField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Filter apps",
    testTag: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag(testTag),
    )
}
