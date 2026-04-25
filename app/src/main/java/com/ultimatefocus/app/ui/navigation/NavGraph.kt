/**
 * Navigation host that maps routes to composable screens.
 * Home and Drawer live inside a HorizontalPager for smooth, finger-following
 * swipe transitions. Settings, BlockingSchedules, and BlockingScheduleEditor
 * are pushed on top via standard NavHost navigation with horizontal slide
 * transitions for visual consistency.
 * Covers: ULT-5, ULT-6, ULT-7, ULT-8, ULT-24, ULT-25
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ultimatefocus.app.ui.blocking.BlockingScheduleEditorScreen
import com.ultimatefocus.app.ui.blocking.BlockingSchedulesListScreen
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
                onNavigateToBlockingSchedules = {
                    navController.navigate(Screen.BlockingSchedules.route) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Screen.BlockingSchedules.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            BlockingSchedulesListScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddSchedule = {
                    navController.navigate(
                        Screen.BlockingScheduleEditor.routeFor(null),
                    ) { launchSingleTop = true }
                },
                onEditSchedule = { id ->
                    navController.navigate(
                        Screen.BlockingScheduleEditor.routeFor(id),
                    ) { launchSingleTop = true }
                },
            )
        }

        composable(
            route = Screen.BlockingScheduleEditor.route,
            arguments = listOf(
                navArgument(Screen.BlockingScheduleEditor.ARG_ID) {
                    type = NavType.LongType
                    defaultValue = Screen.BlockingScheduleEditor.NEW_ID
                },
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            BlockingScheduleEditorScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
