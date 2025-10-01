package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
            // 3D Structure Preview Section - 화면의 35% 차지
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f) // 화면의 35%를 3D 프리뷰에 할당
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
                    // 3D Preview with actual rendering - 남은 공간 모두 사용
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // 타이틀 제외한 남은 공간 모두 사용
                            .background(
                                MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium
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
                }
            }
            
            Divider()
            
            // Scrollable Tab Content - 화면의 65% 차지
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f) // 화면의 65%를 정보 패널에 할당
                    .verticalScroll(rememberScrollState())
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

