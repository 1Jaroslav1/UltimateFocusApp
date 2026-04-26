/**
 * Implementation of SessionTimerRepository.
 *
 * Global settings (master / precision mode / mindful launch delay / default
 * expiry mode) are persisted via UserPreferencesSource (DataStore Preferences)
 * and projected into an InAppReminderSettings snapshot. Mirrors the existing
 * pattern of binding feature-flag UI directly against the Flow rather than
 * caching values inside the ViewModel.
 *
 * Per-app opt-in (ULT-77) is backed by Room (SessionTimerAppDao). The
 * AccessibilityService caches the package set in memory so foreground
 * transitions stay on the hot path; this repo is the single writer.
 *
 * isReminderActiveFor combines master + per-app — the launch interceptor
 * uses it to decide whether to surface the duration picker.
 *
 * Covers: ULT-76, ULT-77
 */
package com.ultimatefocus.app.data.repository

import com.ultimatefocus.app.data.local.dao.SessionTimerAppDao
import com.ultimatefocus.app.data.local.entity.SessionTimerAppEntity
import com.ultimatefocus.app.domain.model.InAppReminderSettings
import com.ultimatefocus.app.domain.model.SessionExpiryMode
import com.ultimatefocus.app.domain.repository.SessionTimerRepository
import com.ultimatefocus.app.domain.repository.UserPreferencesSource
import com.ultimatefocus.app.domain.service.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionTimerRepositoryImpl @Inject constructor(
    private val preferencesSource: UserPreferencesSource,
    private val dao: SessionTimerAppDao,
    private val clock: Clock,
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

    override fun observeEnabledPackages(): Flow<Set<String>> =
        dao.observePackages().map { it.toSet() }

    override suspend fun isReminderActiveFor(packageName: String): Boolean {
        if (!preferencesSource.preferences.first().inAppReminderMasterEnabled) return false
        return dao.isEnabled(packageName)
    }

    override suspend fun setEnabled(packageName: String, enabled: Boolean) {
        if (enabled) {
            dao.upsert(
                SessionTimerAppEntity(
                    packageName = packageName,
                    addedEpochMs = clock.now(),
                ),
            )
        } else {
            dao.deleteByPackage(packageName)
        }
    }
}
