/**
 * Pure-logic collaborator used by BlockedAppNotificationListenerService to
 * decide whether to suppress an incoming notification. Factored out so the
 * suppression rule can be unit-tested without standing up the rigid
 * NotificationListenerService lifecycle.
 * Covers: ULT-22
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.repository.BlockedAppRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedAppChecker @Inject constructor(
    private val blockedAppRepository: BlockedAppRepository,
) {
    suspend fun shouldSuppress(packageName: String): Boolean =
        blockedAppRepository.isBlocked(packageName)
}
