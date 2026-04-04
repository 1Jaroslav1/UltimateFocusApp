/**
 * Sealed class defining all navigation destinations (routes) in the launcher.
 * Home is the default; Drawer is the full app list; Settings holds preferences.
 * Covers: ULT-5, ULT-7
 */
package com.minimalist.launcher.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Drawer : Screen("drawer")
    data object Settings : Screen("settings")
}
