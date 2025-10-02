package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        // 전체 화면 3D 뷰어 - OpenGL ES 3.0 (아이폰 SceneKit과 동일)
        ProteinOpenGLView(
            structure = structure,
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
        
        // 하단 컨트롤 (iPhone과 동일, 깔끔한 디자인)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Secondary 패널 (슬라이드 업)
            if (selectedPanel != ViewerPanel.NONE) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 0.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    tonalElevation = 12.dp,
                    shadowElevation = 8.dp
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
                                onChainToggle = onChainToggle,
                                renderStyle = renderStyle
                            )
                        }
                        else -> {}
                    }
                }
            }
            
            // Primary 컨트롤 바 (깔끔한 디자인)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .navigationBarsPadding(), // 안전 영역 확보
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewerControlButton(
                        icon = Icons.Default.Brush,
                        label = "Rendering",
                        isSelected = selectedPanel == ViewerPanel.RENDERING,
                        selectedColor = Color(0xFF2196F3), // Blue
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
                        selectedColor = Color(0xFFFF9800), // Orange
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
                        selectedColor = Color(0xFF4CAF50), // Green
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
    // iPhone과 동일한 깔끔한 버튼 디자인
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(100.dp)
            .height(60.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.12f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
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
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            "Rendering Style",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            "Color Mode",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
    onChainToggle: (String) -> Unit,
    renderStyle: RenderStyle = RenderStyle.RIBBON
) {
    // iPhone과 동일: Options Secondary Bar
    // Rotation, Zoom, Opacity, Size, Ribbon Width/Flatness, Reset
    var rotationEnabled by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var transparency by remember { mutableStateOf(0.7f) }
    var atomSize by remember { mutableStateOf(1.0f) }
    var ribbonWidth by remember { mutableStateOf(3.0f) }
    var ribbonFlatness by remember { mutableStateOf(0.5f) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            "Options",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
        // 1. Rotation Toggle
        OptionToggleButton(
            icon = if (rotationEnabled) Icons.Default.AutoMode else Icons.Default.RadioButtonUnchecked,
            label = "Rotate",
            isEnabled = rotationEnabled,
            color = Color(0xFFFF9800), // Orange
            onToggle = {
                rotationEnabled = !rotationEnabled
                // TODO: 실제 rotation 적용
            }
        )
        
        // 2. Zoom Level
        OptionSliderControl(
            label = "Zoom",
            value = zoomLevel,
            onValueChange = { zoomLevel = it },
            range = 0.5f..3.0f,
            color = Color(0xFF2196F3), // Blue
            startIcon = Icons.Default.Remove,
            endIcon = Icons.Default.Add
        )
        
        // 3. Transparency (Opacity)
        OptionSliderControl(
            label = "Opacity",
            value = transparency,
            onValueChange = { transparency = it },
            range = 0.1f..1.0f,
            color = Color(0xFF9C27B0), // Purple
            startIcon = Icons.Default.VisibilityOff,
            endIcon = Icons.Default.Visibility
        )
        
        // 4. Atom Size
        OptionSliderControl(
            label = "Size",
            value = atomSize,
            onValueChange = { atomSize = it },
            range = 0.5f..2.0f,
            color = Color(0xFF4CAF50), // Green
            startIcon = Icons.Default.Circle,
            endIcon = Icons.Default.Circle
        )
        
        // 5. Ribbon Width (Ribbon 모드일 때만)
        if (renderStyle == RenderStyle.RIBBON) {
            OptionSliderControl(
                label = "Width",
                value = ribbonWidth,
                onValueChange = { ribbonWidth = it },
                range = 1.0f..8.0f,
                color = Color(0xFF9C27B0), // Purple
                startIcon = Icons.Default.UnfoldLess,
                endIcon = Icons.Default.UnfoldMore
            )
        }
        
        // 6. Ribbon Flatness (Ribbon 모드일 때만)
        if (renderStyle == RenderStyle.RIBBON) {
            OptionSliderControl(
                label = "Flat",
                value = ribbonFlatness,
                onValueChange = { ribbonFlatness = it },
                range = 0.0f..1.0f,
                color = Color(0xFFFF5722), // Deep Orange
                startIcon = Icons.Default.CropSquare,
                endIcon = Icons.Default.CropSquare
            )
        }
        
        // 7. Reset Button
        OptionToggleButton(
            icon = Icons.Default.RestartAlt,
            label = "Reset",
            isEnabled = false,
            color = Color(0xFFF44336), // Red
            onToggle = {
                rotationEnabled = false
                zoomLevel = 1.0f
                transparency = 0.7f
                atomSize = 1.0f
                ribbonWidth = 3.0f
                ribbonFlatness = 0.5f
            }
        )
        }
    }
}

@Composable
private fun OptionToggleButton(
    icon: ImageVector,
    label: String,
    isEnabled: Boolean,
    color: Color,
    onToggle: () -> Unit
) {
    Button(
        onClick = onToggle,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEnabled) color.copy(alpha = 0.1f) else Color.Transparent
        ),
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .heightIn(max = 52.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isEnabled) color else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = if (isEnabled) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OptionSliderControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    color: Color,
    startIcon: ImageVector,
    endIcon: ImageVector
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .heightIn(max = 52.dp),
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(60.dp)
            ) {
                Icon(
                    imageVector = startIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = endIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(12.dp)
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier.width(60.dp)
            )
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
