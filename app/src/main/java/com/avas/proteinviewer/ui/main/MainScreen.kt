package com.avas.proteinviewer.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
// import androidx.compose.foundation.semantics.contentDescription
// import androidx.compose.foundation.semantics.role
// import androidx.compose.foundation.semantics.semantics
// import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.avas.proteinviewer.ui.protein.ProteinViewerView
import com.avas.proteinviewer.ui.protein.BasicFilamentComposeView
import com.avas.proteinviewer.data.model.FilamentStructure
import com.avas.proteinviewer.ui.protein.ProteinInfoPanel
import com.avas.proteinviewer.ui.layout.ResponsiveMainContent
import com.avas.proteinviewer.ui.navigation.ProteinViewerNavigationDrawer
// import com.avas.proteinviewer.ui.accessibility.AccessibilityHelper
import com.avas.proteinviewer.viewmodel.ProteinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToInfo: () -> Unit,
    viewModel: ProteinViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentProteinId by viewModel.currentProteinId.collectAsState()
    val currentProteinName by viewModel.currentProteinName.collectAsState()
    val structure by viewModel.structure.collectAsState()
    val appState by viewModel.appState.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    var showDrawer by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // 다크 테마 상태 관리
    val isDarkTheme = appState.isDarkTheme
    
    // iPhone과 동일: 앱 시작 시 자동으로 1CRN 데이터 로드
    LaunchedEffect(Unit) {
        if (structure == null) {
            viewModel.loadDefaultProtein()
        }
    }
    
    // 다크 테마 변경 시 3D 뷰어 업데이트
    LaunchedEffect(isDarkTheme) {
        // 3D 뷰어에 다크 테마 설정 전달
        // TODO: ProteinSurfaceView에 다크 테마 설정 전달
    }
    
    Box {
        Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(
                                text = if (currentProteinId.isNotEmpty()) {
                                    // 이름과 ID가 같으면 중복 방지
                                    if (currentProteinName == currentProteinId || currentProteinName.isNullOrEmpty()) {
                                        currentProteinId
                                    } else {
                                        "${currentProteinName} ($currentProteinId)"
                                    }
                                } else {
                                    "Protein Viewer"
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { showDrawer = true }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = onNavigateToSearch) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = onNavigateToLibrary) {
                                Icon(Icons.Default.LibraryBooks, contentDescription = "Library")
                            }
                        }
                    )
                },
            bottomBar = {
                if (structure != null) {
                    // 스크롤 가능한 하단 네비게이션
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(7) { index ->
                            val (icon, label) = when (index) {
                                0 -> Icons.Default.Info to "Overview"
                                1 -> Icons.Default.List to "Chains"
                                2 -> Icons.Default.Science to "Residues"
                                3 -> Icons.Default.LocalPharmacy to "Ligands"
                                4 -> Icons.Default.Circle to "Pockets"
                                5 -> Icons.Default.List to "Sequence"
                                6 -> Icons.Default.Label to "Annotations"
                                else -> Icons.Default.Info to "Unknown"
                            }
                            
                            FilterChip(
                                onClick = { selectedTab = index },
                                label = { Text(label) },
                                leadingIcon = { Icon(icon, contentDescription = label) },
                                selected = selectedTab == index,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            // 반응형 레이아웃 적용
            if (structure != null) {
                ResponsiveMainContent(
                    structure = structure,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else {
                // Show loading/error screen when no protein is loaded
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Loading protein structure...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (loadingProgress.isNotEmpty()) {
                                Text(
                                    text = loadingProgress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Text(
                                text = "Welcome to Protein Viewer",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Text(
                                text = "Load a protein structure to start exploring",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            
                            Button(
                                onClick = { viewModel.loadDefaultProtein() },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Loading...")
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Load Default Protein (1CRN)")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 사이드바 표시 (오버레이)
        if (showDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showDrawer = false } // 외부 클릭 시 닫기
            ) {
                // 반투명 배경
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                )
                
                // 사이드바
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ProteinViewerNavigationDrawer(
                        onNavigateToSearch = onNavigateToSearch,
                        onNavigateToLibrary = onNavigateToLibrary,
                        onNavigateToSettings = { /* TODO: Settings 구현 */ },
                        onNavigateToAbout = onNavigateToInfo,
                        onDismiss = { showDrawer = false }
                    )
                    
                    // 나머지 공간 (클릭 시 닫기)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { showDrawer = false }
                    )
                }
            }
        }
    }
}