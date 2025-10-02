package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
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
    Scaffold(
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
                
                // Clear highlights button
                if (uiState.highlightedChains.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { viewModel.clearHighlights() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
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
                        ProteinCanvas3DView(
                            structure = uiState.structure,
                            renderStyle = uiState.renderStyle,
                            colorMode = uiState.colorMode,
                            highlightedChains = uiState.highlightedChains,
                            modifier = Modifier.fillMaxSize()
                        )
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
                        proteinInfo = uiState.currentProteinInfo // API 데이터 전달
                    )
                }
            }
        }
    }
}

