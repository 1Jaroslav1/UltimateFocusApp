/**
 * Pure-logic collaborator used by BlockedAppNotificationListenerService to
 * decide whether to suppress an incoming notification. Defers entirely to
 * BlockingSourceCoordinator, which unifies all three block sources —
 * manual (ULT-22), recurring schedule (ULT-24), and tag-derived (ULT-75).
 * Factored out so the suppression rule can be unit-tested without standing
 * up the rigid NotificationListenerService lifecycle.
 * Covers: ULT-22, ULT-24, ULT-75
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
