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
import com.avas.proteinviewer.domain.model.MenuItemType
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
    
    // 아이폰과 동일: 앱 시작 시 기본 단백질 자동 로딩
    LaunchedEffect(Unit) {
        if (uiState.structure == null) {
            viewModel.loadDefaultProtein()
        }
    }
    
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 메뉴 상세 화면 상태 관리
    var selectedMenuItem by remember { mutableStateOf<MenuItemType?>(null) }
    var selectedSideMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false, // 스와이프 제스처 비활성화 (햄버거 버튼으로만 열림)
        drawerContent = {
            SideMenuContent(
                onMenuItemClick = { menuItem ->
                    when (menuItem) {
                        MenuItem.PROTEIN_LIBRARY -> {
                            viewModel.toggleProteinLibrary()
                            viewModel.loadCategoryCounts()
                            // iOS와 동일: selectedCategory를 null로 설정하여 카테고리 그리드 바로 표시
                            viewModel.selectCategory(null)
                            selectedSideMenuItem = null // Protein Library는 사이드바 메뉴에서 제외
                        }
                        else -> {
                            // 선택된 메뉴 상태 업데이트
                            selectedSideMenuItem = menuItem
                            
                            // 새로운 MenuItemType으로 변환하여 상세 화면 표시
                            val menuItemType = when (menuItem) {
                                MenuItem.ABOUT -> MenuItemType.ABOUT
                                MenuItem.USER_GUIDE -> MenuItemType.USER_GUIDE
                                MenuItem.FEATURES -> MenuItemType.FEATURES
                                MenuItem.SETTINGS -> MenuItemType.SETTINGS
                                MenuItem.HELP -> MenuItemType.HELP
                                MenuItem.PRIVACY -> MenuItemType.PRIVACY
                                MenuItem.TERMS -> MenuItemType.TERMS
                                MenuItem.LICENSE -> MenuItemType.LICENSE
                                else -> null
                            }
                            menuItemType?.let { selectedMenuItem = it }
                        }
                    }
                    scope.launch {
                        drawerState.close()
                    }
                },
                onClose = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                selectedMenuItem = selectedSideMenuItem
            )
        }
    ) {
        when {
            selectedMenuItem != null -> {
                // 메뉴 상세 화면 표시
                MenuDetailScreen(
                    menuItem = selectedMenuItem!!,
                    onNavigateBack = { 
                        selectedMenuItem = null
                        // 백 버튼 누르면 사이드바 메뉴 열기
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            }
            uiState.showProteinLibrary -> {
                ProteinLibraryScreen(
                    proteins = uiState.searchResults,
                    selectedCategory = uiState.selectedCategory,
                    showCategoryGrid = true,
                    categoryCounts = uiState.categoryProteinCounts.mapKeys { it.key.displayName },
                    favoriteIds = uiState.favorites,
                    hasMoreResults = uiState.hasMoreData,
                    isLoadingResults = uiState.isLoading && uiState.showProteinLibrary,
                    isLoadingMore = uiState.isLoadingMore,
                    loadingMessage = uiState.loadingProgress.takeIf { it.isNotBlank() },
                    onSearch = viewModel::searchProteins,
                    onSearchBasedDataLoad = viewModel::performSearchBasedDataLoad,
                    onProteinClick = viewModel::selectProtein,
                    onCategorySelect = viewModel::selectCategory,
                    onShowAllCategories = viewModel::showAllCategories,
                    onDismiss = { viewModel.toggleProteinLibrary() },
                    onToggleFavorite = { /* TODO: hook into favorites once implemented */ },
                    onLoadMore = { /* TODO: hook into pagination once implemented */ }
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
                    onHighlightAllToggle = viewModel::toggleHighlightAll,
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
