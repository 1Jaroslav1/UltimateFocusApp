/**
 * Behavior the in-app session timer takes when the committed duration runs
 * out. Persisted globally in DataStore (the user's default) and overridable
 * per-session in the launch-time duration picker. The four modes mirror the
 * radio group on the In-App Reminder settings page.
 *
 * - MINDFUL_EXTENSION_DELAY: shows a fullscreen overlay with a forced
 *   countdown before the user is allowed to extend. Friction discourages
 *   reflexive extensions.
 * - EXIT_APP: closes the app and returns the user to the home screen.
 * - EXIT_AND_BLOCK: closes the app AND uses the existing manual-block
 *   pipeline (BlockedAppRepository) to block it for the same duration the
 *   user originally selected, so the existing AccessibilityService gate
 *   handles re-entry attempts.
 * - ONLY_REMINDER: surfaces a notification but takes no automatic action.
 *
 * Covers: ULT-76, ULT-81
 */
package com.ultimatefocus.app.domain.model

enum class SessionExpiryMode {
    MINDFUL_EXTENSION_DELAY,
    EXIT_APP,
    EXIT_AND_BLOCK,
    ONLY_REMINDER,
}
