/**
 * Domain model for user-configurable preferences persisted via DataStore.
 * autoKeyboard (ULT-8): when true the keyboard opens automatically on app drawer entry.
 * searchProviderPackage (ULT-16): package name of the app handling swipe-up search;
 *   null means delegate to the system default (Intent.ACTION_WEB_SEARCH).
 * twoFingerLongPressToLockEnabled (ULT-18): when true, holding two fingers on
 *   the home screen for the system long-press duration (~500 ms) locks the
 *   device. Replaces earlier single-finger double-tap and quick two-finger-tap
 *   variants that collided with the HorizontalPager's multi-touch handling on
 *   some devices.
 * Covers: ULT-6, ULT-8, ULT-16, ULT-18
 */
package com.ultimatefocus.app.domain.model

data class UserPreferences(
    val isDarkTheme: Boolean = true,
    val fontSize: FontSize = FontSize.MEDIUM,
    val homeAlignment: HomeAlignment = HomeAlignment.LEFT,
    val autoKeyboard: Boolean = true,
    val searchProviderPackage: String? = null,
    val twoFingerLongPressToLockEnabled: Boolean = false,
)

enum class FontSize(val scaleFactor: Float) {
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f),
}

enum class HomeAlignment { LEFT, CENTER, RIGHT }
