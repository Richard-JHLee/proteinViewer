package com.avas.proteinviewer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.avas.proteinviewer.presentation.theme.ProteinViewerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProteinViewerTheme {
                ProteinViewerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProteinViewerApp() {
    val viewModel: ProteinViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideMenuContent(
                onMenuItemClick = { menuItem ->
                    // Handle menu item clicks
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        when {
            uiState.showProteinLibrary -> {
                ProteinLibraryScreen(
                    proteins = uiState.searchResults,
                    onSearch = viewModel::searchProteins,
                    onProteinClick = viewModel::selectProtein,
                    onDismiss = { viewModel.toggleProteinLibrary() }
                )
            }
            uiState.isLoading -> {
                LoadingScreen(progress = uiState.loadingProgress)
            }
            uiState.error != null -> {
                ErrorScreen(
                    error = uiState.error ?: "",
                    onRetry = { viewModel.loadDefaultProtein() },
                    onDismiss = { viewModel.clearError() }
                )
            }
            uiState.viewMode == ViewMode.VIEWER -> {
                // Viewer Mode - Full 3D view
                ProteinViewerScreen(
                    structure = uiState.structure!!,
                    renderStyle = uiState.renderStyle,
                    colorMode = uiState.colorMode,
                    highlightedChains = uiState.highlightedChains,
                    onStyleChange = viewModel::setRenderStyle,
                    onColorModeChange = viewModel::setColorMode,
                    onChainToggle = viewModel::toggleChainHighlight,
                    onBackToInfo = { viewModel.setViewMode(ViewMode.INFO) }
                )
            }
            else -> {
                // Info Mode - Default view with 3D preview and tabs
                InfoModeScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onOpenDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            }
        }
    }
}
