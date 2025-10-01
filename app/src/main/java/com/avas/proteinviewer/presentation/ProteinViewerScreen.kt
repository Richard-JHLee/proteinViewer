package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProteinViewerScreen(
    structure: PDBStructure,
    renderStyle: RenderStyle,
    colorMode: ColorMode,
    highlightedChains: Set<String>,
    onStyleChange: (RenderStyle) -> Unit,
    onColorModeChange: (ColorMode) -> Unit,
    onChainToggle: (String) -> Unit,
    onBackToInfo: () -> Unit
) {
    var showInfoPanel by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(InfoTab.OVERVIEW) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("3D Viewer") 
                },
                navigationIcon = {
                    IconButton(onClick = onBackToInfo) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Info")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
                        // 3D Viewer with actual rendering
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface) // iOS처럼 투명/시스템 배경
                        ) {
                ProteinCanvas3DView(
                    structure = structure,
                    renderStyle = renderStyle,
                    colorMode = colorMode,
                    highlightedChains = highlightedChains,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Bottom Control Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // Info Panel (if shown)
                if (showInfoPanel) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        InfoPanel(
                            structure = structure,
                            selectedTab = selectedTab,
                            onTabChange = { selectedTab = it },
                            onClose = { showInfoPanel = false },
                            proteinInfo = null // TODO: 필요시 proteinInfo 전달
                        )
                    }
                }
                
                // Control Buttons
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        // Render Style Selection
                        Text(
                            text = "Render Style",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(RenderStyle.values()) { style ->
                                FilterChip(
                                    selected = renderStyle == style,
                                    onClick = { onStyleChange(style) },
                                    label = { Text(style.displayName) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Color Mode Selection
                        Text(
                            text = "Color Mode",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(ColorMode.values()) { mode ->
                                FilterChip(
                                    selected = colorMode == mode,
                                    onClick = { onColorModeChange(mode) },
                                    label = { Text(mode.displayName) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            // Floating Action Button for Info
            FloatingActionButton(
                onClick = { showInfoPanel = !showInfoPanel },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 160.dp)
            ) {
                Icon(
                    imageVector = if (showInfoPanel) Icons.Default.Close else Icons.Default.Info,
                    contentDescription = "Info"
                )
            }
        }
    }
}

@Composable
fun Protein3DView(
    structure: PDBStructure,
    renderStyle: RenderStyle,
    colorMode: ColorMode,
    highlightedChains: Set<String>,
    modifier: Modifier = Modifier
) {
    // Placeholder for 3D rendering
    // In a complete implementation, this would use OpenGL ES or SceneForm
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Science,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
            
            Text(
                text = "3D Protein Viewer",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            
            Text(
                text = "${structure.atomCount} atoms, ${structure.chainCount} chains",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Text(
                text = "Render: ${renderStyle.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            
            Text(
                text = "Color: ${colorMode.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            
            if (highlightedChains.isNotEmpty()) {
                Text(
                    text = "Highlighted: ${highlightedChains.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Green.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
