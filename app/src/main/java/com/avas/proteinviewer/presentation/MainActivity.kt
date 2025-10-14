package com.avas.proteinviewer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.avas.proteinviewer.domain.model.MenuItemType
import com.avas.proteinviewer.domain.model.ResearchDetailType
import com.avas.proteinviewer.presentation.theme.ProteinViewerTheme
import com.avas.proteinviewer.presentation.ResearchDetailScreen
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
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
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
                    categoryDataSource = uiState.categoryDataSource,
                    favoriteIds = uiState.favorites,
                    hasMoreResults = uiState.hasMoreData,
                    isLoadingResults = uiState.isLoading && uiState.showProteinLibrary,
                    isLoadingMore = uiState.isLoadingMore,
                    loadingMessage = uiState.loadingProgress.takeIf { it.isNotBlank() },
                    onSearch = viewModel::searchProteins,
                    onSearchBasedDataLoad = viewModel::performSearchBasedDataLoad,
                    onProteinClick = { proteinId ->
                        // 아이폰과 동일: 단백질 정보를 찾아서 Detail 모달 표시
                        val protein = uiState.searchResults.find { it.id == proteinId }
                        if (protein != null) {
                            viewModel.selectProteinFromLibrary(protein)
                        }
                    },
                    onCategorySelect = viewModel::selectCategory,
                    onShowAllCategories = viewModel::showAllCategories,
                    onDismiss = { viewModel.toggleProteinLibrary() },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onLoadMore = viewModel::loadMore
                )
                
                // 아이폰과 동일: Protein Detail 모달 (InfoSheet)
                if (uiState.showProteinDetail && uiState.selectedProteinForDetail != null && !uiState.showResearchDetail) {
                    ProteinDetailSheet(
                        protein = uiState.selectedProteinForDetail!!,
                        uiState = uiState,
                        onDismiss = { viewModel.dismissProteinDetail() },
                        onView3D = { proteinId ->
                            viewModel.loadProteinFor3DViewing(proteinId)
                        },
                        onLoadDiseaseAssociations = { proteinId ->
                            viewModel.loadDiseaseAssociations(proteinId)
                            viewModel.loadExperimentalDetails(proteinId) // Additional Information도 로드
                        },
                        onLoadResearchStatus = { proteinId ->
                            viewModel.loadResearchStatus(proteinId)
                        },
                        onLoadFunctionDetails = { proteinId, description ->
                            viewModel.loadFunctionDetails(proteinId, description)
                        },
                        onLoadPrimaryStructure = { proteinId ->
                            viewModel.loadPrimaryStructure(proteinId)
                        },
                        onLoadSecondaryStructure = { proteinId ->
                            viewModel.loadSecondaryStructure(proteinId)
                        },
                        onLoadTertiaryStructure = { proteinId ->
                            viewModel.loadTertiaryStructure(proteinId)
                        },
                        onLoadQuaternaryStructure = { proteinId ->
                            viewModel.loadQuaternaryStructure(proteinId)
                        },
           onLoadRelatedProteins = { proteinId ->
               viewModel.loadRelatedProteins(proteinId)
           },
           onShowResearchDetail = { researchType ->
               viewModel.showResearchDetail(researchType)
           }
                    )
                }
                
                // 아이폰과 동일: Function Details 모달
                if (uiState.showFunctionDetails && uiState.selectedProteinForDetail != null) {
                    FunctionDetailsScreen(
                        protein = uiState.selectedProteinForDetail!!,
                        uiState = uiState,
                        onDismiss = { viewModel.dismissFunctionDetails() }
                    )
                }
                
                // 아이폰과 동일: Primary Structure 모달
                if (uiState.showPrimaryStructure && uiState.selectedProteinForDetail != null) {
                    PrimaryStructureScreen(
                        protein = uiState.selectedProteinForDetail!!,
                        uiState = uiState,
                        onDismiss = { viewModel.dismissPrimaryStructure() }
                    )
                }
                
                // 아이폰과 동일: Secondary Structure 모달
                if (uiState.showSecondaryStructure && uiState.selectedProteinForDetail != null) {
                    SecondaryStructureScreen(
                        protein = uiState.selectedProteinForDetail!!,
                        uiState = uiState,
                        onDismiss = { viewModel.dismissSecondaryStructure() }
                    )
                }
                
                // 아이폰과 동일: Tertiary Structure 모달
                if (uiState.showTertiaryStructure && uiState.selectedProteinForDetail != null) {
                    TertiaryStructureScreen(
                        protein = uiState.selectedProteinForDetail!!,
                        uiState = uiState,
                        onDismiss = { viewModel.dismissTertiaryStructure() }
                    )
                }
                
                // 아이폰과 동일: Quaternary Structure 모달
                if (uiState.showQuaternaryStructure && uiState.selectedProteinForDetail != null) {
                    QuaternaryStructureScreen(
                        protein = uiState.selectedProteinForDetail!!,
                        uiState = uiState,
                        onDismiss = { viewModel.dismissQuaternaryStructure() }
                    )
                }
                
                // 아이폰과 동일: Related Proteins 모달
                if (uiState.showRelatedProteins && uiState.selectedProteinForDetail != null) {
                    RelatedProteinsScreen(
                        protein = uiState.selectedProteinForDetail!!,
                        uiState = uiState,
                        onDismiss = { viewModel.dismissRelatedProteins() }
                    )
                }
                
                // 아이폰과 동일: Research Detail 모달
                if (uiState.showResearchDetail && uiState.selectedProteinForDetail != null && uiState.researchDetailType != null) {
                    ResearchDetailScreen(
                        protein = uiState.selectedProteinForDetail!!,
                        researchType = uiState.researchDetailType!!,
                        onDismiss = { viewModel.dismissResearchDetail() }
                    )
                }
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
