/**
 * Domain model representing a single installed application.
 * Used across the home screen favorites list and the full app drawer list.
 * Covers: ULT-5, ULT-6, ULT-7
 */
package com.minimalist.launcher.domain.model

import android.content.ComponentName

data class AppInfo(
    val packageName: String,
    val label: String,
    val componentName: ComponentName,
    val isFavorite: Boolean = false,
    val favoriteOrder: Int = 0,
)
