/**
 * Domain model representing a single installed application.
 * Used across the home screen favorites list and the full app drawer list.
 * blockedUntilEpochMs is non-null when the user has an active manual block
 * on this package — the UI renders it with a 🔒 prefix + strikethrough.
 * tags carries the user-assigned tags (ULT-73), shown as colored dots
 * to the right of the label (ULT-74).
 * Covers: ULT-5, ULT-6, ULT-7, ULT-20, ULT-73, ULT-74
 */
package com.ultimatefocus.app.domain.model

import android.content.ComponentName

data class AppInfo(
    val packageName: String,
    val label: String,
    val componentName: ComponentName,
    val isFavorite: Boolean = false,
    val favoriteOrder: Int = 0,
    val blockedUntilEpochMs: Long? = null,
    val tags: List<Tag> = emptyList(),
)
