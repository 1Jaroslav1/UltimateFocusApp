/**
 * Pure-logic collaborator used by BlockedAppNotificationListenerService to
 * decide whether to suppress an incoming notification. Reads from
 * BlockingSourceCoordinator so notifications from packages blocked by EITHER
 * a manual block (ULT-22) or a recurring schedule (ULT-24) are silenced, and
 * additionally consults BlockedTagRepository so apps indirectly blocked via
 * one of their tags (ULT-75) are silenced for the same window.
 * Factored out so the suppression rule can be unit-tested without standing up
 * the rigid NotificationListenerService lifecycle.
 * Covers: ULT-22, ULT-24, ULT-75
 */
package com.ultimatefocus.app.domain.service

import com.ultimatefocus.app.domain.repository.BlockedTagRepository
import com.ultimatefocus.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedAppChecker @Inject constructor(
    private val coordinator: BlockingSourceCoordinator,
    private val blockedTagRepository: BlockedTagRepository,
    private val tagRepository: TagRepository,
) {
    suspend fun shouldSuppress(packageName: String): Boolean {
        if (coordinator.isBlocked(packageName)) return true
        // Covered by any active tag-block? Walk the app's assigned tags and
        // check each — keeps the call cheap when the app has 0/few tags.
        val tags = tagRepository.observeTagsForApp(packageName).first()
        return tags.any { blockedTagRepository.isTagBlocked(it.id) }
    }
}
