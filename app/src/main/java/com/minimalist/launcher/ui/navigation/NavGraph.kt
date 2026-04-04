/**
 * Navigation host that maps routes to composable screens.
 * Drawer slides in from the right on swipe-left and slides out to the right
 * when the user swipes right or presses back (ULT-7).
 * Settings is accessible from within the drawer (ULT-8).
 * Covers: ULT-5, ULT-6, ULT-7
 */
package com.minimalist.launcher.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.minimalist.launcher.ui.drawer.AppDrawerScreen
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

        composable(
            route = Screen.Drawer.route,
            // Slide in from the right when opening
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            // Slide out to the right when closing
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            AppDrawerScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
