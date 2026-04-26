/**
 * Sealed class defining all navigation destinations (routes) in the launcher.
 * Home is the default; Drawer is the full app list; Settings holds preferences.
 * BlockingSchedules + BlockingScheduleEditor are pushed from Settings (ULT-24/25)
 * — the editor route accepts an optional `id` query param: absent / -1 means
 * "create a new schedule"; any other value loads an existing schedule for edit.
 * ManageTags is pushed from Settings (ULT-72) for tag CRUD.
 * Covers: ULT-5, ULT-7, ULT-24, ULT-25, ULT-72
 */
package com.ultimatefocus.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Drawer : Screen("drawer")
    data object Settings : Screen("settings")
    data object BlockingSchedules : Screen("blocking_schedules")
    data object BlockingScheduleEditor : Screen("blocking_schedule_editor?id={id}") {
        const val ARG_ID = "id"
        const val NEW_ID = -1L
        fun routeFor(id: Long?): String = "blocking_schedule_editor?id=${id ?: NEW_ID}"
    }
    data object ManageTags : Screen("manage_tags")
}
