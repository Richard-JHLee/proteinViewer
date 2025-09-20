package com.avas.proteinviewer.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.avas.proteinviewer.ui.main.MainScreen
import com.avas.proteinviewer.ui.search.SearchScreen
import com.avas.proteinviewer.ui.library.LibraryScreen
import com.avas.proteinviewer.ui.info.InfoScreen

@Composable
fun ProteinViewerNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToLibrary = {
                    navController.navigate(Screen.Library.route)
                },
                onNavigateToInfo = {
                    navController.navigate(Screen.Info.route)
                }
            )
        }
        
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Info.route) {
            InfoScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Search : Screen("search")
    object Library : Screen("library")
    object Info : Screen("info")
}
