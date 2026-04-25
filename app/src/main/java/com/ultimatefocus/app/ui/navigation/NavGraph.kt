/**
 * Navigation host that maps routes to composable screens.
 * Home and Drawer live inside a HorizontalPager for smooth, finger-following
 * swipe transitions. Settings, BlockingSchedules, BlockingScheduleEditor, and
 * ManageTags are pushed on top via standard NavHost navigation with horizontal
 * slide transitions for visual consistency.
 *
 * Transitions: each pushed route uses the standard "push" pattern — on
 * forward navigation the outgoing screen slides off to the LEFT while the
 * incoming screen slides in from the RIGHT (the two halves meet in the
 * middle, no grey gap). On pop the directions are reversed: the outgoing
 * screen slides to the right while the returning screen slides in from
 * the left. The pager destination keeps the NavHost-default None
 * transitions so pushing on top of it / popping back to it just slides
 * the pushed screen in or out without disturbing the pager underneath.
 *
 * Back-press handling:
 *   - On the home pager page (page 0): a BackHandler consumes the back press
 *     and does nothing — the launcher's home is intentionally non-exitable
 *     since it IS the device home screen.
 *   - On the drawer pager page (page 1): a BackHandler animates the pager
 *     back to home.
 *   - On any pushed route (Settings / BlockingSchedules / Editor / ManageTags):
 *     NavHost's own BackHandler pops the back stack, so the OS edge-swipe-back
 *     gesture navigates back exactly like in other Android apps.
 *
 * Covers: ULT-5, ULT-6, ULT-7, ULT-8, ULT-23, ULT-24, ULT-25, ULT-72
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
import com.ultimatefocus.app.ui.tags.ManageTagsScreen
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
            // Home page (0) consumes back so the launcher does not finish.
            BackHandler(enabled = pagerState.currentPage == 0) { /* no-op */ }
            // Drawer page (1) routes back to home.
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
                        onOpenManageTags = {
                            navController.navigate(Screen.ManageTags.route) {
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
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBlockingSchedules = {
                    navController.navigate(Screen.BlockingSchedules.route) {
                        launchSingleTop = true
                    }
                },
                onOpenManageTags = {
                    navController.navigate(Screen.ManageTags.route) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Screen.BlockingSchedules.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
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
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            BlockingScheduleEditorScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.ManageTags.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            ManageTagsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
