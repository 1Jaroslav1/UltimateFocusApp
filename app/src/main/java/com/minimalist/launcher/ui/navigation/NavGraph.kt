/**
 * Navigation host that maps routes to composable screens.
 * Home screen is the start destination (ULT-6).
 * Drawer and Settings routes are wired in subsequent issues.
 * Covers: ULT-5, ULT-6
 */
package com.minimalist.launcher.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.minimalist.launcher.ui.home.HomeScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                onOpenDrawer = { navController.navigate(Screen.Drawer.route) },
            )
        }
    }
}
