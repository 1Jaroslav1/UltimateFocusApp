/**
 * In-memory test double for SystemActions. Records invocations so tests
 * can assert which gesture triggered which system action, without touching
 * the real Android APIs (Intents, DevicePolicyManager, PackageManager).
 * Covers: ULT-16, ULT-17, ULT-18
 */
package com.ultimatefocus.app.domain.service

class FakeSystemActions(
    private var adminGranted: Boolean = false,
    private val providers: List<WebSearchProvider> = emptyList(),
) : SystemActions {

    var notificationShadeOpenCount: Int = 0
        private set
    var lastOpenedSearchPackage: String? = null
        private set
    var searchOpenCount: Int = 0
        private set
    var lockScreenCalls: Int = 0
        private set
    var requestLockPermissionCalls: Int = 0
        private set

    override fun openNotificationShade() {
        notificationShadeOpenCount++
    }

    override fun openWebSearch(providerPackage: String?) {
        lastOpenedSearchPackage = providerPackage
        searchOpenCount++
    }

    override fun queryWebSearchProviders(): List<WebSearchProvider> = providers

    override fun lockScreen(): LockResult {
        lockScreenCalls++
        return if (adminGranted) LockResult.Success else LockResult.MissingPermission
    }

    override fun requestLockPermission() {
        requestLockPermissionCalls++
    }

    override fun isLockPermissionGranted(): Boolean = adminGranted

    fun setAdminGranted(granted: Boolean) {
        adminGranted = granted
    }
}
