/**
 * Implementation of SessionTimerRepository.
 *
 * Global settings (master / precision mode / mindful launch delay / default
 * expiry mode) are persisted via UserPreferencesSource (DataStore Preferences)
 * and projected into an InAppReminderSettings snapshot. Mirrors the existing
 * pattern of binding feature-flag UI directly against the Flow rather than
 * caching values inside the ViewModel.
 *
 * Per-app opt-in is added by ULT-77 (Room-backed). Until that commit lands,
 * the per-app surface is a no-op so the settings page can render the global
 * controls without forcing an empty-set Room query path.
 *
 * Covers: ULT-76
 */
package com.ultimatefocus.app.data.repository

import com.ultimatefocus.app.domain.model.InAppReminderSettings
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.repository.SessionTimerRepository
import com.ultimatefocus.app.domain.repository.UserPreferencesSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionTimerRepositoryImpl @Inject constructor(
    private val preferencesSource: UserPreferencesSource,
) : SessionTimerRepository {

    override fun observeSettings(): Flow<InAppReminderSettings> =
        preferencesSource.preferences.map { prefs ->
            InAppReminderSettings(
                masterEnabled = prefs.inAppReminderMasterEnabled,
                precisionMode = prefs.inAppReminderPrecisionMode,
                mindfulLaunchDelay = prefs.inAppReminderMindfulLaunchDelay,
                expiryMode = prefs.inAppReminderExpiryMode,
            )
        }

    override suspend fun updateMasterEnabled(enabled: Boolean) {
        preferencesSource.updateInAppReminderMasterEnabled(enabled)
    }

    override suspend fun updatePrecisionMode(enabled: Boolean) {
        preferencesSource.updateInAppReminderPrecisionMode(enabled)
    }

    override suspend fun updateMindfulLaunchDelay(enabled: Boolean) {
        preferencesSource.updateInAppReminderMindfulLaunchDelay(enabled)
    }

    override suspend fun updateExpiryMode(mode: SessionExpiryMode) {
        preferencesSource.updateInAppReminderExpiryMode(mode)
    }

    // Per-app surface is wired up in ULT-77. Stubs below keep the feature
    // graph coherent so the global-only settings page (ULT-76) compiles and
    // ships independently of the per-app list rollout.
    override fun observeEnabledPackages(): Flow<Set<String>> = flowOf(emptySet())

    override suspend fun isReminderActiveFor(packageName: String): Boolean = false

    override suspend fun setEnabled(packageName: String, enabled: Boolean) = Unit
}
