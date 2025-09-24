package com.avas.proteinviewer.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.avas.proteinviewer.ui.navigation.DrawerItemType
import com.avas.proteinviewer.ui.navigation.ProteinViewerNavigationDrawer
// import com.avas.proteinviewer.ui.accessibility.AccessibilityHelper
import com.avas.proteinviewer.viewmodel.ProteinViewModel
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToUserGuide: () -> Unit,
    onNavigateToFeatures: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToLicense: () -> Unit,
    shouldShowDrawer: Boolean = false,
    viewModel: ProteinViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentProteinId by viewModel.currentProteinId.collectAsState()
    val currentProteinName by viewModel.currentProteinName.collectAsState()
    val structure by viewModel.structure.collectAsState()
    val appState by viewModel.appState.collectAsState()
    
    var showDrawer by remember { mutableStateOf(shouldShowDrawer) }
    val coroutineScope = rememberCoroutineScope()
    
    // 네비게이션에서 돌아올 때 사이드바 열기
    LaunchedEffect(shouldShowDrawer) {
        if (shouldShowDrawer) {
            showDrawer = true
        }
    }
    
    // 다크 테마 상태 관리
    val isDarkTheme = appState.isDarkTheme
    
    // iPhone과 동일: 앱 시작 시 자동으로 1CRN 데이터 로드 (한 번만)
    LaunchedEffect(Unit) {
        Log.d("MainScreen", "LaunchedEffect triggered - structure: $structure, currentProteinId: $currentProteinId")
        if (structure == null && currentProteinId.isEmpty()) {
            Log.d("MainScreen", "Calling loadDefaultProtein()")
            viewModel.loadDefaultProtein()
        } else {
            Log.d("MainScreen", "Skipping loadDefaultProtein - structure: $structure, currentProteinId: $currentProteinId")
        }
    }
    
    // 다크 테마 변경 시 3D 뷰어 업데이트
    LaunchedEffect(isDarkTheme) {
        // 3D 뷰어에 다크 테마 설정 전달
        // TODO: ProteinSurfaceView에 다크 테마 설정 전달
    }
    
    Box {
        Scaffold(
            bottomBar = {}
        ) { paddingValues ->
            // 반응형 레이아웃 적용
            if (structure != null) {
                ProteinViewerView(
                    structure = structure,
                    proteinId = currentProteinId,
                    proteinName = currentProteinName,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onMenuClick = { 
                        Log.d("MainScreen", "Menu clicked")
                        showDrawer = true 
                    },
                    onLibraryClick = { 
                        Log.d("MainScreen", "Library clicked - navigating to library")
                        onNavigateToLibrary() 
                    },
                    onSwitchToViewer = { 
                        Log.d("MainScreen", "Switch to viewer clicked")
                        onNavigateToInfo() 
                    }
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
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                    ProteinViewerNavigationDrawer(
                        onItemSelected = { item ->
                            when (item) {
                                DrawerItemType.About -> onNavigateToAbout()
                                DrawerItemType.UserGuide -> onNavigateToUserGuide()
                                DrawerItemType.Features -> onNavigateToFeatures()
                                DrawerItemType.Settings -> onNavigateToSettings()
                                DrawerItemType.Help -> onNavigateToHelp()
                                DrawerItemType.Privacy -> onNavigateToPrivacy()
                                DrawerItemType.Terms -> onNavigateToTerms()
                                DrawerItemType.License -> onNavigateToLicense()
                            }
                            showDrawer = false
                        },
                        onDismiss = { showDrawer = false }
                    )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        .clickable { showDrawer = false }
                )
            }
        }
    }
}
