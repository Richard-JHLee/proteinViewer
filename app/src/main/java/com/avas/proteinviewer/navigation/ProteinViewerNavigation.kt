package com.avas.proteinviewer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.util.Log
import com.avas.proteinviewer.ui.info.AboutScreen
import com.avas.proteinviewer.ui.main.MainScreen
import com.avas.proteinviewer.ui.search.SearchScreen
import com.avas.proteinviewer.ui.library.LibraryScreen
import com.avas.proteinviewer.ui.info.InfoScreen
import com.avas.proteinviewer.ui.info.UserGuideScreen
import com.avas.proteinviewer.ui.info.FeaturesScreen
import com.avas.proteinviewer.ui.info.SettingsScreen
import com.avas.proteinviewer.ui.info.HelpScreen
import com.avas.proteinviewer.ui.info.PrivacyScreen
import com.avas.proteinviewer.ui.info.TermsScreen
import com.avas.proteinviewer.ui.info.LicenseScreen

@Composable
fun ProteinViewerNavigation(
    navController: NavHostController = rememberNavController()
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    var shouldShowDrawer by remember { mutableStateOf(false) }
    
    // 네비게이션 상태 감지
    LaunchedEffect(currentBackStackEntry) {
        val currentRoute = currentBackStackEntry?.destination?.route
        // 정보 화면에서 메인 화면으로 돌아왔을 때 사이드바 열기
        if (currentRoute == Screen.Main.route && shouldShowDrawer) {
            // 사이드바를 열도록 플래그 설정
        }
    }
    
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
                    Log.d("Navigation", "Navigating to Library screen")
                    navController.navigate(Screen.Library.route)
                },
                onNavigateToInfo = {
                    shouldShowDrawer = false
                    navController.navigate(Screen.Info.route)
                },
                onNavigateToAbout = {
                    shouldShowDrawer = false
                    navController.navigate(Screen.About.route)
                },
                onNavigateToUserGuide = {
                    shouldShowDrawer = false
                    navController.navigate(Screen.UserGuide.route)
                },
                onNavigateToFeatures = {
                    shouldShowDrawer = false
                    navController.navigate(Screen.Features.route)
                },
                onNavigateToSettings = {
                    shouldShowDrawer = false
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToHelp = {
                    shouldShowDrawer = false
                    navController.navigate(Screen.Help.route)
                },
                onNavigateToPrivacy = {
                    shouldShowDrawer = false
                    navController.navigate(Screen.Privacy.route)
                },
                onNavigateToTerms = {
                    shouldShowDrawer = false
                    navController.navigate(Screen.Terms.route)
                },
                onNavigateToLicense = {
                    shouldShowDrawer = false
                    navController.navigate(Screen.License.route)
                },
                shouldShowDrawer = shouldShowDrawer
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
                    shouldShowDrawer = true
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.UserGuide.route) {
            UserGuideScreen(
                onNavigateBack = {
                    shouldShowDrawer = true
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = {
                    shouldShowDrawer = true
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Features.route) {
            FeaturesScreen(
                onNavigateBack = {
                    shouldShowDrawer = true
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    shouldShowDrawer = true
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Help.route) {
            HelpScreen(
                onNavigateBack = {
                    shouldShowDrawer = true
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Privacy.route) {
            PrivacyScreen(
                onNavigateBack = {
                    shouldShowDrawer = true
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Terms.route) {
            TermsScreen(
                onNavigateBack = {
                    shouldShowDrawer = true
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.License.route) {
            LicenseScreen(
                onNavigateBack = {
                    shouldShowDrawer = true
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
    object UserGuide : Screen("user_guide")
    object Settings : Screen("settings")
    object About : Screen("about")
    object Features : Screen("features")
    object Help : Screen("help")
    object Privacy : Screen("privacy")
    object Terms : Screen("terms")
    object License : Screen("license")
}
