package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
    
    // 로딩 상태 관리 함수들
    fun startInfoUpdating() {
        isInfoModeUpdating = true
    }
    
    fun stopInfoUpdating() {
        isInfoModeUpdating = false
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
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setInfoTab(tab) },
                                    label = {
                                        Text(
                                            text = tab.name.replace("_", " ")
                                                .lowercase()
                                                .replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF2196F3), // 파란색
                                        selectedLabelColor = Color.White
                                    )
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
                            Icon(Icons.Default.LibraryBooks, contentDescription = "Library")
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
                                onClick = { viewModel.clearHighlights() },
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
                    Divider()
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
                            isInfoMode = true, // Info 모드로 설정
                            onRenderingComplete = { stopInfoUpdating() }, // 렌더링 완료 시 로딩 해제
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Info 모드 업데이트 중 로딩 표시
                        if (isInfoModeUpdating) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Updating structure...",
                                        style = MaterialTheme.typography.bodyMedium,
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
            
            Divider()
            
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
                                structure = uiState.structure,
                                selectedTab = uiState.selectedInfoTab,
                                onTabChange = { tab -> viewModel.setInfoTab(tab) },
                                onClose = {},
                                proteinInfo = uiState.currentProteinInfo, // API 데이터 전달
                                viewModel = viewModel, // ViewModel 전달
                                uiState = uiState, // UI State 전달
                                onStartUpdating = { startInfoUpdating() }, // 로딩 시작
                                onStopUpdating = { stopInfoUpdating() } // 로딩 종료
                            )
                }
            }
        }
    }
}

