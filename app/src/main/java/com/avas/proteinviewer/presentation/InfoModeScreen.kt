package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.domain.model.*

enum class ViewMode {
    INFO, VIEWER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoModeScreen(
    uiState: ProteinUiState,
    viewModel: ProteinViewModel,
    onOpenDrawer: () -> Unit
) {
    // Info 모드 전용 로딩 상태
    var isInfoModeUpdating by remember { mutableStateOf(false) }
    var is3DRendering by remember { mutableStateOf(false) }
    var is3DRenderingCompleted by remember { mutableStateOf(false) }
    
    // 로딩 상태 관리 함수들
    fun startInfoUpdating() {
        isInfoModeUpdating = true
    }
    
    fun stopInfoUpdating() {
        isInfoModeUpdating = false
    }
    
    // 3D 렌더링 상태 관리
    fun start3DRendering() {
        is3DRendering = true
        is3DRenderingCompleted = false
        android.util.Log.d("InfoModeScreen", "3D rendering started - loading bar should show")
    }
    
    fun stop3DRendering() {
        is3DRendering = false
        is3DRenderingCompleted = true
        android.util.Log.d("InfoModeScreen", "3D rendering stopped - loading bar should hide")
    }
    
    // Viewer 모드에서 돌아오는 경우: 이미 렌더링된 이미지 재사용
    if (uiState.currentProteinId.isNotEmpty() && 
        uiState.structure != null && 
        uiState.previousViewMode == ViewMode.VIEWER &&
        !is3DRenderingCompleted) {
        android.util.Log.d("InfoModeScreen", "Reusing rendered image from Viewer mode")
        is3DRenderingCompleted = true
    }
    
    // 새로운 단백질 로드 시: 3D 렌더링 시작
    if (uiState.currentProteinId.isNotEmpty() && 
        uiState.structure != null && 
        uiState.previousViewMode != ViewMode.VIEWER &&
        !is3DRendering && 
        !is3DRenderingCompleted) {
        android.util.Log.d("InfoModeScreen", "Starting 3D rendering - new protein loaded")
        start3DRendering()
    }
    
    // 구조가 로드되었지만 3D 렌더링이 시작되지 않은 경우 강제 시작
    if (uiState.currentProteinId.isNotEmpty() && 
        uiState.structure != null && 
        !is3DRendering && 
        !is3DRenderingCompleted) {
        android.util.Log.d("InfoModeScreen", "Force starting 3D rendering - structure loaded but not rendering")
        start3DRendering()
    }
    Scaffold(
        bottomBar = {
            // 아이폰과 동일: 하단 고정 탭바 (7개 탭)
            if (uiState.structure != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                            InfoTab.values().forEach { tab ->
                                val isSelected = uiState.selectedInfoTab == tab
                                val tabIcon = when (tab) {
                                    InfoTab.OVERVIEW -> Icons.Default.Info
                                    InfoTab.CHAINS -> Icons.Default.Link
                                    InfoTab.RESIDUES -> Icons.Default.Circle
                                    InfoTab.LIGANDS -> Icons.Default.Science
                                    InfoTab.POCKETS -> Icons.Default.Place
                                    InfoTab.SEQUENCE -> Icons.AutoMirrored.Filled.List
                                    InfoTab.ANNOTATIONS -> Icons.AutoMirrored.Filled.Note
                                }
                                
                                // 아이폰 스타일: 아이콘 위, 텍스트 아래
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setInfoTab(tab) },
                                    label = {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            // 아이콘 (위쪽)
                                            Icon(
                                                imageVector = tabIcon,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = if (isSelected) Color.White else Color(0xFF2196F3)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            // 텍스트 (아래쪽)
                                            Text(
                                                text = tab.name.replace("_", " ")
                                                    .lowercase()
                                                    .replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color(0xFF2196F3),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF2196F3), // 파란색
                                        selectedLabelColor = Color.Transparent, // 투명하게 설정 (내부에서 색상 제어)
                                        containerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.height(70.dp) // 아이콘과 텍스트를 위한 높이 증가
                                )
                            }
                    }
                }
            }
        },
        topBar = {
            Column {
                // Top Bar
                TopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (uiState.currentProteinId.isNotEmpty()) {
                                Text(
                                    text = uiState.currentProteinId,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (uiState.currentProteinName.isNotEmpty()) {
                                Text(
                                    text = uiState.currentProteinName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleProteinLibrary() }) {
                            Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Library")
                        }
                        IconButton(onClick = { viewModel.setViewMode(ViewMode.VIEWER) }) {
                            Icon(Icons.Default.Visibility, contentDescription = "3D Viewer")
                        }
                    }
                )
                
                // Focus/Clear 영역 - 아이폰과 동일 (헤더 아래 고정)
                if (uiState.focusedElement != null || uiState.highlightedChains.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Focus 인디케이터 (녹색 배지)
                        if (uiState.focusedElement != null) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        Color(0xFF4CAF50).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Focused: ${uiState.focusedElement}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                        
                        // Clear 버튼 (빨간색 배지)
                        if (uiState.highlightedChains.isNotEmpty() || uiState.focusedElement != null) {
                            Button(
                                onClick = { 
                                    startInfoUpdating()
                                    viewModel.clearHighlights()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF44336)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 3D Structure Preview Section - 아이폰과 동일: 220dp 고정 높이
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp) // 제목(30dp) + 3D 뷰어(220dp) + padding(20dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text(
                    text = "3D Structure Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                if (uiState.structure != null) {
                    // 3D Preview - 아이폰과 동일: 220dp 고정 높이
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp) // 아이폰과 동일
                            .background(
                                Color(0xFFEEEEEE), // systemGray6와 유사한 연한 회색
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                    ) {
                        // OpenGL ES 3.0 렌더러 사용 (아이폰 SceneKit과 동일)
                        ProteinOpenGLView(
                            structure = uiState.structure,
                            renderStyle = uiState.renderStyle,
                            colorMode = uiState.colorMode,
                            highlightedChains = uiState.highlightedChains,
                            focusedElement = uiState.focusedElement, // 포커스 요소 전달
                            isInfoMode = true, // Info 모드로 설정
                            onRenderingStart = { 
                                // 렌더링 시작 콜백 제거 - 조건문에서 자동 처리
                            },
                            onRenderingComplete = { 
                                stopInfoUpdating() // Info 모드 로딩 해제
                                stop3DRendering() // 3D 렌더링 완료
                                viewModel.onRenderingComplete() // ViewModel 렌더링 완료 알림
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // 이미지 위에 "Loading..." 오버레이 표시
                        if (!is3DRenderingCompleted || isInfoModeUpdating) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)), // 반투명 배경
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Loading...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 초기 화면 - 아이폰과 동일
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            // 로딩 중
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
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                if (uiState.loadingProgress.isNotEmpty()) {
                                    Text(
                                        text = uiState.loadingProgress,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        } else {
                            // 초기 대기 화면
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Science,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                
                                Text(
                                    text = "Loading...",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Text(
                                    text = "Loading default protein structure...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // Scrollable Tab Content - 아이폰과 동일: 남은 공간 모두 사용
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 3D 프리뷰 제외한 남은 공간 모두 사용
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp) // 아이폰과 동일
            ) {
                if (uiState.structure != null) {
                            InfoPanel(
                                selectedTab = uiState.selectedInfoTab,
                                structure = uiState.structure,
                                proteinInfo = uiState.currentProteinInfo,
                                viewModel = viewModel,
                                uiState = uiState,
                                onStartUpdating = { startInfoUpdating() }
                            )
                    
                    // 메인화면에서는 View 3D Structure 버튼 제거 (이미 3D 구조를 보고 있음)
                    
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

