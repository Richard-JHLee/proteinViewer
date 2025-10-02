package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.domain.model.*

enum class ViewerPanel {
    NONE, RENDERING, COLOR, OPTIONS
}

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
    var selectedPanel by remember { mutableStateOf(ViewerPanel.NONE) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 전체 화면 3D 뷰어 (아이폰과 동일)
        ProteinCanvas3DView(
            structure = structure,
            renderStyle = renderStyle,
            colorMode = colorMode,
            highlightedChains = highlightedChains,
            modifier = Modifier.fillMaxSize()
        )
        
        // 우상단 버튼들 (아이폰과 동일)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back 버튼
            IconButton(
                onClick = onBackToInfo,
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back to Info",
                    tint = Color.Black
                )
            }
            
            // Reset 버튼 (TODO: 카메라 리셋 기능)
            IconButton(
                onClick = { /* TODO: Reset camera */ },
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset Camera",
                    tint = Color.Black
                )
            }
            
            // Settings 버튼 (TODO: 설정 기능)
            IconButton(
                onClick = { /* TODO: Settings */ },
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.Black
                )
            }
        }
        
        // 하단 컨트롤 (아이폰과 동일)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // 2차 패널 (슬라이드 업)
            if (selectedPanel != ViewerPanel.NONE) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp
                ) {
                    when (selectedPanel) {
                        ViewerPanel.RENDERING -> {
                            RenderingStylePanel(
                                selectedStyle = renderStyle,
                                onStyleSelect = onStyleChange
                            )
                        }
                        ViewerPanel.COLOR -> {
                            ColorModePanel(
                                selectedMode = colorMode,
                                onModeSelect = onColorModeChange
                            )
                        }
                        ViewerPanel.OPTIONS -> {
                            OptionsPanel(
                                chains = structure.chains.sorted(),
                                highlightedChains = highlightedChains,
                                onChainToggle = onChainToggle
                            )
                        }
                        else -> {}
                    }
                }
            }
            
            // 메인 컨트롤 바
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ViewerControlButton(
                        icon = Icons.Default.Brush,
                        label = "Rendering",
                        isSelected = selectedPanel == ViewerPanel.RENDERING,
                        selectedColor = Color(0xFFF44336), // 빨강
                        onClick = {
                            selectedPanel = if (selectedPanel == ViewerPanel.RENDERING)
                                ViewerPanel.NONE
                            else
                                ViewerPanel.RENDERING
                        }
                    )
                    
                    ViewerControlButton(
                        icon = Icons.Default.MoreVert,
                        label = "Options",
                        isSelected = selectedPanel == ViewerPanel.OPTIONS,
                        selectedColor = Color(0xFFFF9800), // 주황
                        onClick = {
                            selectedPanel = if (selectedPanel == ViewerPanel.OPTIONS)
                                ViewerPanel.NONE
                            else
                                ViewerPanel.OPTIONS
                        }
                    )
                    
                    ViewerControlButton(
                        icon = Icons.Default.Palette,
                        label = "Colors",
                        isSelected = selectedPanel == ViewerPanel.COLOR,
                        selectedColor = Color(0xFF4CAF50), // 녹색
                        onClick = {
                            selectedPanel = if (selectedPanel == ViewerPanel.COLOR)
                                ViewerPanel.NONE
                            else
                                ViewerPanel.COLOR
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewerControlButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (isSelected) selectedColor else Color.Gray
        ),
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.size(width = 100.dp, height = 56.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun RenderingStylePanel(
    selectedStyle: RenderStyle,
    onStyleSelect: (RenderStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Rendering Style",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RenderStyle.values().forEach { style ->
                StyleOptionCard(
                    label = style.name,
                    isSelected = selectedStyle == style,
                    onClick = { onStyleSelect(style) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ColorModePanel(
    selectedMode: ColorMode,
    onModeSelect: (ColorMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Color Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ColorMode.values().forEach { mode ->
                StyleOptionCard(
                    label = mode.name,
                    isSelected = selectedMode == mode,
                    onClick = { onModeSelect(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsPanel(
    chains: List<String>,
    highlightedChains: Set<String>,
    onChainToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Chain Selection",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chains.forEach { chain ->
                FilterChip(
                    selected = highlightedChains.contains(chain),
                    onClick = { onChainToggle(chain) },
                    label = { Text("Chain $chain") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyleOptionCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label.replace("_", " "),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
