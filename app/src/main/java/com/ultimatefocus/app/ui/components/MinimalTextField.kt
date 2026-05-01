/**
 * Centralized minimal text field used across the launcher (drawer search,
 * schedule editor app browser, In-App Reminder app filter). Replaces
 * Material3 OutlinedTextField in those surfaces — OutlinedTextField imposes
 * a 56 dp internal min-height that prevents the user-feedback request to
 * shrink input rows by ~40%.
 *
 * Renders only a single white underline at the bottom (no top/side borders,
 * no Material outlined chrome) and clamps min-height to 32 dp so the field
 * is visibly shorter than the previous OutlinedTextField. Slots cover
 * leading/trailing icons and a placeholder; an optional FocusRequester
 * lets callers auto-focus the keyboard. Default IME action is Done; the
 * default keyboardActions clear focus so pressing Done dismisses the IME
 * even when the caller does not override.
 *
 * Stateless: caller owns the value/onValueChange. testTag is a parameter so
 * each surface can keep its existing test selector.
 *
 * Covers: ULT-82
 */
package com.ultimatefocus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

private val UnderlineColor: Color = Color.White
private val UnderlineThicknessDp = 1.dp
private const val DefaultPlaceholderText = ""

@Composable
fun MinimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = DefaultPlaceholderText,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions: KeyboardActions? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    val focusManager = LocalFocusManager.current
    val effectiveActions = remember(keyboardActions) {
        keyboardActions ?: KeyboardActions(onDone = { focusManager.clearFocus() })
    }
    val onBackground = MaterialTheme.colorScheme.onBackground
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
    val baseTextStyle = LocalTextStyle.current.copy(
        color = onBackground,
        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
    )

    val outerModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 32.dp)
        .background(Color.Transparent)
        .drawBehind {
            val strokePx = UnderlineThicknessDp.toPx()
            val y = size.height - strokePx / 2
            drawLine(
                color = UnderlineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokePx,
            )
        }
        .let { if (testTag != null) it.testTag(testTag) else it }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = baseTextStyle,
        cursorBrush = SolidColor(onBackground),
        keyboardOptions = keyboardOptions,
        keyboardActions = effectiveActions,
        modifier = outerModifier
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                if (leadingIcon != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) { leadingIcon() }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = baseTextStyle.copy(color = placeholderColor),
                        )
                    }
                    innerTextField()
                }
                if (trailingIcon != null) {
                    Box(modifier = Modifier.padding(start = 8.dp)) { trailingIcon() }
                }
            }
        },
    )
}
