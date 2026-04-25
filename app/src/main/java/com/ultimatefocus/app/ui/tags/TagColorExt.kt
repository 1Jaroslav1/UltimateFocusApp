/**
 * Compose helpers for the TagColor enum. Centralizes hex→Compose Color
 * parsing so the rest of the UI never imports android.graphics.
 * Covers: ULT-72, ULT-74
 */
package com.ultimatefocus.app.ui.tags

import androidx.compose.ui.graphics.Color
import com.ultimatefocus.app.domain.model.TagColor

fun TagColor.toComposeColor(): Color = Color(android.graphics.Color.parseColor(hex))
