/**
 * Repository contract for the In-App Time Reminder feature. Owns:
 *  - the global feature configuration (master toggle, precision mode,
 *    mindful launch delay, default expiry mode) — backed by DataStore via
 *    UserPreferencesSource so the settings page binds against a single
 *    Flow projection.
 *  - per-app opt-in (added in ULT-77) — backed by Room so the
 *    AccessibilityService and launcher hot-paths can query a stable Set.
 *
 * isReminderActiveFor combines master + per-app to answer the single
 * question the launch interceptors care about: "should opening this
 * package surface the duration picker?"
 *
 * Covers: ULT-76, ULT-77
 */
package com.ultimatefocus.app.domain.repository

import com.ultimatefocus.app.domain.model.InAppReminderSettings
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import kotlinx.coroutines.flow.Flow

interface SessionTimerRepository {
    fun observeSettings(): Flow<InAppReminderSettings>
    suspend fun updateMasterEnabled(enabled: Boolean)
    suspend fun updatePrecisionMode(enabled: Boolean)
    suspend fun updateMindfulLaunchDelay(enabled: Boolean)
    suspend fun updateExpiryMode(mode: SessionExpiryMode)

    fun observeEnabledPackages(): Flow<Set<String>>
    suspend fun isReminderActiveFor(packageName: String): Boolean
    suspend fun setEnabled(packageName: String, enabled: Boolean)
}
