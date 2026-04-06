/**
 * Domain model for user-configurable preferences persisted via DataStore.
 * autoKeyboard (ULT-8): when true the keyboard opens automatically on app drawer entry.
 * Covers: ULT-6, ULT-8
 */
package com.minimalist.launcher.domain.model

data class UserPreferences(
    val isDarkTheme: Boolean = true,
    val fontSize: FontSize = FontSize.MEDIUM,
    val homeAlignment: HomeAlignment = HomeAlignment.LEFT,
    val autoKeyboard: Boolean = true,
)

enum class FontSize(val scaleFactor: Float) {
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f),
}

enum class HomeAlignment { LEFT, CENTER, RIGHT }
