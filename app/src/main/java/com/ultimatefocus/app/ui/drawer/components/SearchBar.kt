/**
 * Search field shown at the top of the app drawer. Now built on the
 * centralized MinimalTextField (ULT-82 #1) so it shares the slim
 * bottom-only-underline visual with the schedule editor's app filter and
 * the In-App Reminder app filter. Accepts a FocusRequester so the caller
 * can auto-focus the keyboard when the auto-keyboard setting is on.
 * Covers: ULT-7, ULT-8, ULT-82
 */
package com.ultimatefocus.app.ui.drawer.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ultimatefocus.app.ui.components.MinimalTextField

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester = FocusRequester(),
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    MinimalTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = "Search apps…",
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = Color.White,
                    )
                }
            }
        },
        focusRequester = focusRequester,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        testTag = "search_bar",
        modifier = modifier.padding(start = 16.dp, end = 0.dp, top = 8.dp, bottom = 8.dp),
    )
}
