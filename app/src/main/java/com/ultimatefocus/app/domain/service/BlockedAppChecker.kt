/**
 * Pure-logic collaborator used by BlockedAppNotificationListenerService to
 * decide whether to suppress an incoming notification. Reads from
 * BlockingSourceCoordinator so notifications from packages blocked by EITHER
 * a manual block (ULT-22) or a recurring schedule (ULT-24) are silenced.
 * Factored out so the suppression rule can be unit-tested without standing up
 * the rigid NotificationListenerService lifecycle.
 * Covers: ULT-22, ULT-24
 */
package com.ultimatefocus.app.domain.service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedAppChecker @Inject constructor(
    private val coordinator: BlockingSourceCoordinator,
) {
    suspend fun shouldSuppress(packageName: String): Boolean =
        coordinator.isBlocked(packageName)
}
