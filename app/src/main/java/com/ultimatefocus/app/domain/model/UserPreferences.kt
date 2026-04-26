/**
 * Domain model for user-configurable preferences persisted via DataStore.
 * autoKeyboard (ULT-8): when true the keyboard opens automatically on app drawer entry.
 * searchProviderPackage (ULT-16): package name of the app handling swipe-up search;
 *   null means delegate to the system default (Intent.ACTION_WEB_SEARCH).
 * twoFingerDoubleTapToLockEnabled (ULT-18): when true, two consecutive brief
 *   two-finger taps on the home screen lock the device. Chosen after a
 *   single-finger double-tap, a quick two-finger tap, and a two-finger
 *   long-press all either collided with the HorizontalPager's multi-touch
 *   arbitration or were swallowed higher up in the gesture stack.
 * leftShortcut / rightShortcut (ULT-13, ULT-14): optional user overrides for
 *   the bottom-bar quick-access shortcut slots. Null means "use the system default".
 * inAppReminder* (ULT-76): the global In-App Reminder configuration. When
 *   masterEnabled is false the entire feature is dormant regardless of any
 *   per-app opt-ins. precisionMode is persisted here as a togglable flag;
 *   actual notification-interaction filtering it implies is owned by a
 *   separate parent (US-2.4.2) and is a no-op for now.
 * Covers: ULT-6, ULT-8, ULT-13, ULT-14, ULT-16, ULT-18, ULT-76
 */
package com.ultimatefocus.app.domain.model

data class UserPreferences(
    val isDarkTheme: Boolean = true,
    val fontSize: FontSize = FontSize.MEDIUM,
    val homeAlignment: HomeAlignment = HomeAlignment.LEFT,
    val autoKeyboard: Boolean = true,
    val searchProviderPackage: String? = null,
    val twoFingerDoubleTapToLockEnabled: Boolean = false,
    val leftShortcut: ShortcutTarget? = null,
    val rightShortcut: ShortcutTarget? = null,
    val inAppReminderMasterEnabled: Boolean = false,
    val inAppReminderPrecisionMode: Boolean = false,
    val inAppReminderMindfulLaunchDelay: Boolean = false,
    val inAppReminderExpiryMode: SessionExpiryMode = SessionExpiryMode.ONLY_REMINDER,
)

enum class FontSize(val scaleFactor: Float) {
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f),
}

enum class HomeAlignment { LEFT, CENTER, RIGHT }
