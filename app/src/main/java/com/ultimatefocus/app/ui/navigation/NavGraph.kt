/**
 * Navigation host that maps routes to composable screens.
 * Home and Drawer live inside a HorizontalPager for smooth, finger-following
 * swipe transitions. Settings is pushed on top via standard NavHost navigation.
 * Covers: ULT-5, ULT-6, ULT-7, ULT-8
 */
package com.ultimatefocus.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ultimatefocus.app.ui.drawer.AppDrawerScreen
import com.ultimatefocus.app.ui.home.HomeScreen
import com.ultimatefocus.app.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun NavGraph(navController: NavHostController) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = "pager",
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        composable(route = "pager") {
            BackHandler(enabled = pagerState.currentPage == 1) {
                coroutineScope.launch { pagerState.animateScrollToPage(0) }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        onOpenDrawer = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                    )
                    1 -> AppDrawerScreen(
                        isActive = pagerState.currentPage == 1,
                        onNavigateBack = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onOpenSettings = {
                            navController.navigate(Screen.Settings.route) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
