/**
 * Domain model representing a single installed application.
 * Used across the home screen favorites list and the full app drawer list.
 * blockedUntilEpochMs is non-null when the user has an active manual block
 * on this package — the UI renders it with a 🔒 prefix + strikethrough.
 * Covers: ULT-5, ULT-6, ULT-7, ULT-20
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
)
