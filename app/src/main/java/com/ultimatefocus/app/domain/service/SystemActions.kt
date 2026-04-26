/**
 * Abstraction over the Android system interactions triggered by home-screen
 * gestures: opening the configured web-search provider, expanding the
 * notification shade, and locking the device. Kept as an interface so the
 * home screen and its ViewModel can be exercised by unit/instrumented tests
 * with an in-memory fake.
 * goHome() (ULT-81) returns the user to the launcher home — used by the
 * EXIT_APP and EXIT_AND_BLOCK session-expiry behavior modes.
 * Covers: ULT-16, ULT-17, ULT-18, ULT-81
 */
package com.ultimatefocus.app.domain.service

/** An installed app that can satisfy a web search intent. */
data class WebSearchProvider(
    val packageName: String,
    val label: String,
)

/** Result of attempting to lock the screen via DevicePolicyManager. */
sealed interface LockResult {
    data object Success : LockResult
    data object MissingPermission : LockResult
}

interface SystemActions {
    /** Expand the system notification shade. */
    fun openNotificationShade()

    /**
     * Launch a web search using [providerPackage]. When null, delegates to the
     * system-configured default search handler (Intent.ACTION_WEB_SEARCH).
     */
    fun openWebSearch(providerPackage: String?)

    /** Installed apps that advertise themselves as web-search handlers. */
    fun queryWebSearchProviders(): List<WebSearchProvider>

    /** Lock the device screen. Requires active device-admin permission. */
    fun lockScreen(): LockResult

    /** Prompt the user to grant device-admin permission for screen lock. */
    fun requestLockPermission()

    /** Whether the launcher currently holds device-admin permission. */
    fun isLockPermissionGranted(): Boolean

    /**
     * Send the user back to the launcher home screen. Used by the EXIT_APP
     * and EXIT_AND_BLOCK session-expiry modes to evict the user from a
     * timed-out app (ULT-81).
     */
    fun goHome()
}
