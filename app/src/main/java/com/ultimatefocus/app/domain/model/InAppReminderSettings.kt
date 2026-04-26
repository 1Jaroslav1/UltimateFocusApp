/**
 * Snapshot of the global In-App Reminder configuration. Persisted via
 * UserPreferencesDataStore (DataStore Preferences). The settings page binds
 * directly against this projection so disabled-when-master-off styling is a
 * pure function of state.
 *
 * masterEnabled mirrors the top-most "In-app time reminder active" switch:
 * when false every other field is treated as inert (UI greys out, the
 * launch interceptor never asks for a duration).
 *
 * precisionMode is persisted here for the toggle itself (ULT-76 AC). The
 * actual notification-interaction filtering it implies is the job of a
 * separate parent (US-2.4.2 / ULT-31) and is left as a no-op for now.
 *
 * Covers: ULT-76
 */
package com.ultimatefocus.app.domain.model

data class InAppReminderSettings(
    val masterEnabled: Boolean = false,
    val precisionMode: Boolean = false,
    val mindfulLaunchDelay: Boolean = false,
    val expiryMode: SessionExpiryMode = SessionExpiryMode.ONLY_REMINDER,
)
