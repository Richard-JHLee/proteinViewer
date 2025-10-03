package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class ViewerPanel {
    NONE, STYLE, OPTIONS, COLORS
}

enum class SecondaryPanelType {
    NONE, RENDERING_STYLES, OPTIONS_MENU, COLOR_MODES
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
    onHighlightAllToggle: () -> Unit,
    onBackToInfo: () -> Unit
) {
    var selectedSecondaryPanel by remember { mutableStateOf(SecondaryPanelType.NONE) }
    
    // Options values
    var rotationEnabled by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var transparency by remember { mutableStateOf(1.0f) }
    var atomSize by remember { mutableStateOf(1.0f) }
    var ribbonWidth by remember { mutableStateOf(1.2f) }
    var ribbonFlatness by remember { mutableStateOf(0.5f) }
    
    // 로딩 상태
    var isUpdating by remember { mutableStateOf(false) }
    
    // 구조가 변경될 때마다 로딩 시작
    LaunchedEffect(structure) {
        isUpdating = true
    }
    
    // 로딩 상태 관리 함수들
    fun startUpdating() {
        isUpdating = true
    }
    
    fun stopUpdating() {
        isUpdating = false
    }
    
    // Highlight All 상태 계산 (모든 체인이 하이라이트되어 있는지)
    val allChains = structure.chains.map { "chain:$it" }.toSet()
    val highlightAllChains = allChains.isNotEmpty() && allChains.all { it in highlightedChains }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 전체 화면 3D 뷰어 - OpenGL ES 3.0 (아이폰 SceneKit과 동일)
        // Secondary 패널과 관계없이 항상 전체 화면 크기 유지
        ProteinOpenGLView(
            structure = structure,
            renderStyle = renderStyle,
            colorMode = colorMode,
            highlightedChains = highlightedChains,
            rotationEnabled = rotationEnabled,
            zoomLevel = zoomLevel,
            transparency = transparency,
            atomSize = atomSize,
            ribbonWidth = ribbonWidth,
            ribbonFlatness = ribbonFlatness,
            onRenderingComplete = { stopUpdating() }, // 렌더링 완료 시 로딩 해제
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        )
        
        // 로딩 인디케이터 (3D 뷰 위에 표시)
        if (isUpdating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .align(Alignment.Center)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    strokeWidth = 4.dp
                )
            }
        }
        
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
        
        // 하단 컨트롤 (iPhone과 동일: Primary=6개 Style 버튼, Secondary=3개 카테고리)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Secondary 패널 (슬라이드 업) - iPhone처럼 3개 카테고리
            if (selectedSecondaryPanel != SecondaryPanelType.NONE) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 0.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    tonalElevation = 12.dp,
                    shadowElevation = 8.dp
                ) {
                    when (selectedSecondaryPanel) {
                        SecondaryPanelType.RENDERING_STYLES -> {
                            // iPhone처럼: 5개 Rendering Style + Highlight All
                            SecondaryRenderingStyleBar(
                                selectedStyle = renderStyle,
                                onStyleSelect = { style ->
                                    startUpdating()
                                    onStyleChange(style)
                                    // 렌더링 완료 콜백에서 자동으로 stopUpdating() 호출됨
                                    // Secondary bar 유지 (iPhone과 동일)
                                },
                                highlightAllChains = highlightAllChains,
                                onHighlightAllToggle = onHighlightAllToggle
                            )
                        }
                        SecondaryPanelType.COLOR_MODES -> {
                            // iPhone처럼: 4개 Color Mode
                            SecondaryColorModeBar(
                                selectedMode = colorMode,
                                onModeSelect = { mode ->
                                    startUpdating()
                                    onColorModeChange(mode)
                                    // 렌더링 완료 콜백에서 자동으로 stopUpdating() 호출됨
                                }
                            )
                        }
                        SecondaryPanelType.OPTIONS_MENU -> {
                            // iPhone처럼: Sliders (Rotation, Zoom, Opacity, Size, Ribbon Width/Flatness)
                            SecondaryOptionsBar(
                                renderStyle = renderStyle,
                                rotationEnabled = rotationEnabled,
                                onRotationEnabledChange = { 
                                    rotationEnabled = it
                                    startUpdating()
                                    // 렌더링 완료 콜백에서 자동으로 stopUpdating() 호출됨
                                },
                                zoomLevel = zoomLevel,
                                onZoomLevelChange = { 
                                    zoomLevel = it
                                    startUpdating()
                                    // 렌더링 완료 콜백에서 자동으로 stopUpdating() 호출됨
                                },
                                transparency = transparency,
                                onTransparencyChange = { 
                                    transparency = it
                                    startUpdating()
                                    // 렌더링 완료 콜백에서 자동으로 stopUpdating() 호출됨
                                },
                                atomSize = atomSize,
                                onAtomSizeChange = { 
                                    atomSize = it
                                    startUpdating()
                                    // 렌더링 완료 콜백에서 자동으로 stopUpdating() 호출됨
                                },
                                ribbonWidth = ribbonWidth,
                                onRibbonWidthChange = { 
                                    ribbonWidth = it
                                    startUpdating()
                                    // 렌더링 완료 콜백에서 자동으로 stopUpdating() 호출됨
                                },
                                ribbonFlatness = ribbonFlatness,
                                onRibbonFlatnessChange = { 
                                    ribbonFlatness = it
                                    startUpdating()
                                    // 렌더링 완료 콜백에서 자동으로 stopUpdating() 호출됨
                                },
                                onReset = {
                                    startUpdating()
                                    rotationEnabled = false
                                    zoomLevel = 1.0f
                                    transparency = 0.7f
                                    atomSize = 1.0f
                                    ribbonWidth = 3.0f
                                    ribbonFlatness = 0.5f
                                    // 렌더링 완료 콜백에서 자동으로 stopUpdating() 호출됨
                                }
                            )
                        }
                        else -> {}
                    }
                }
            }
            
            // Primary 컨트롤 바 - iPhone처럼 3개 카테고리 버튼으로 변경
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
                    // iPhone과 동일: 3개 Secondary 카테고리 버튼
                    PrimarySecondaryButton(
                        icon = Icons.Default.GridOn, // 4 dots icon
                        label = "Style",
                        isSelected = selectedSecondaryPanel == SecondaryPanelType.RENDERING_STYLES,
                        selectedColor = Color(0xFF2196F3), // Blue
                        onClick = {
                            selectedSecondaryPanel = if (selectedSecondaryPanel == SecondaryPanelType.RENDERING_STYLES)
                                SecondaryPanelType.NONE
                            else
                                SecondaryPanelType.RENDERING_STYLES
                        }
                    )
                    
                    PrimarySecondaryButton(
                        icon = Icons.Default.MoreHoriz, // 3 dots icon
                        label = "Options",
                        isSelected = selectedSecondaryPanel == SecondaryPanelType.OPTIONS_MENU,
                        selectedColor = Color(0xFFFF9800), // Orange
                        onClick = {
                            selectedSecondaryPanel = if (selectedSecondaryPanel == SecondaryPanelType.OPTIONS_MENU)
                                SecondaryPanelType.NONE
                            else
                                SecondaryPanelType.OPTIONS_MENU
                        }
                    )
                    
                    PrimarySecondaryButton(
                        icon = Icons.Default.Public, // Globe icon
                        label = "Colors",
                        isSelected = selectedSecondaryPanel == SecondaryPanelType.COLOR_MODES,
                        selectedColor = Color(0xFF4CAF50), // Green
                        onClick = {
                            selectedSecondaryPanel = if (selectedSecondaryPanel == SecondaryPanelType.COLOR_MODES)
                                SecondaryPanelType.NONE
                            else
                                SecondaryPanelType.COLOR_MODES
                        }
                    )
                }
            }
        }
    }
}

// iPhone 스타일: Primary 버튼 (하단 3개 카테고리 버튼)
@Composable
private fun PrimarySecondaryButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .width(100.dp)
            .height(44.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                // 선택 시: 파란색, 미선택 시: 검정색
                tint = if (isSelected) Color(0xFF2196F3) else Color.Black,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                // 항상 검정색
                color = Color.Black,
                // 선택 시: Bold, 미선택 시: Normal
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// iPhone 스타일: Secondary 버튼 (Rendering Style, Color Mode 등)
@Composable
private fun RowScope.SecondaryStyleButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    selectedColor: Color = Color(0xFF2196F3), // Default Blue (배경색으로만 사용)
    onClick: () -> Unit
) {
    // 선택 시: 파란색 배경 + 파란색 아이콘 + 검은색 Bold 텍스트
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF2196F3).copy(alpha = 0.1f) else Color.Transparent
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.weight(1f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                // 선택 시: 파란색, 미선택 시: 검정색
                tint = if (isSelected) Color(0xFF2196F3) else Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                maxLines = 1,
                // 항상 검정색
                color = Color.Black,
                // 선택 시: Bold, 미선택 시: Medium
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// iPhone 스타일: Secondary Rendering Style Bar (5개 + Highlight All)
@Composable
private fun SecondaryRenderingStyleBar(
    selectedStyle: RenderStyle,
    onStyleSelect: (RenderStyle) -> Unit,
    highlightAllChains: Boolean,
    onHighlightAllToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(60.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 5개 Rendering Style 버튼
        RenderStyle.values().forEach { style ->
            SecondaryStyleButton(
                icon = getStyleIcon(style),
                label = style.displayName,
                isSelected = selectedStyle == style,
                selectedColor = Color(0xFF2196F3), // Blue
                onClick = { onStyleSelect(style) }
            )
        }
        
        // Highlight All 버튼 (배경/아이콘: 파란색, 텍스트: 검정 Bold)
        SecondaryStyleButton(
            icon = Icons.Default.Lightbulb,
            label = "Highlight All",
            isSelected = highlightAllChains,
            selectedColor = Color(0xFF2196F3), // Blue (노랑색 대신)
            onClick = onHighlightAllToggle
        )
    }
}

// iPhone 스타일: Secondary Color Mode Bar (4개)
@Composable
private fun SecondaryColorModeBar(
    selectedMode: ColorMode,
    onModeSelect: (ColorMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(60.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ColorMode.values().forEach { mode ->
            SecondaryStyleButton(
                icon = getColorModeIcon(mode),
                label = mode.displayName,
                isSelected = selectedMode == mode,
                selectedColor = Color(0xFF2196F3), // Blue (Green 대신)
                onClick = { onModeSelect(mode) }
            )
        }
    }
}

// iPhone 스타일: Secondary Options Bar (Sliders)
@Composable
private fun SecondaryOptionsBar(
    renderStyle: RenderStyle,
    rotationEnabled: Boolean,
    onRotationEnabledChange: (Boolean) -> Unit,
    zoomLevel: Float,
    onZoomLevelChange: (Float) -> Unit,
    transparency: Float,
    onTransparencyChange: (Float) -> Unit,
    atomSize: Float,
    onAtomSizeChange: (Float) -> Unit,
    ribbonWidth: Float,
    onRibbonWidthChange: (Float) -> Unit,
    ribbonFlatness: Float,
    onRibbonFlatnessChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(60.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Rotation Toggle (회전 활성화/비활성화)
        OptionsSliderItem(
            icon = Icons.Default.RotateRight,
            label = "Rotate",
            isToggle = true,
            toggleValue = rotationEnabled,
            onToggleChange = onRotationEnabledChange,
            modifier = Modifier.width(80.dp)
        )
        
        // Zoom Level (줌 레벨)
        OptionsSliderItem(
            icon = Icons.Default.ZoomIn,
            label = "Zoom",
            sliderValue = zoomLevel,
            sliderRange = 0.5f..3.0f, // iPhone과 동일한 범위
            onSliderChange = onZoomLevelChange,
            modifier = Modifier.width(80.dp)
        )
        
        // Transparency (투명도)
        OptionsSliderItem(
            icon = Icons.Default.Visibility,
            label = "Opacity",
            sliderValue = transparency,
            sliderRange = 0.1f..1.0f,
            onSliderChange = onTransparencyChange,
            modifier = Modifier.width(80.dp)
        )
        
        // Atom Size (원자 크기)
        OptionsSliderItem(
            icon = Icons.Default.Circle,
            label = "Size",
            sliderValue = atomSize,
            sliderRange = 0.5f..2.0f,
            onSliderChange = onAtomSizeChange,
            modifier = Modifier.width(80.dp)
        )
        
        // Ribbon Width (리본 모드일 때만 표시)
        if (renderStyle == RenderStyle.RIBBON) {
            OptionsSliderItem(
                icon = Icons.Default.ArrowForward,
                label = "Width",
                sliderValue = ribbonWidth,
                sliderRange = 1.0f..8.0f,
                onSliderChange = onRibbonWidthChange,
                modifier = Modifier.width(80.dp)
            )
        }
        
        // Ribbon Flatness (리본 모드일 때만 표시)
        if (renderStyle == RenderStyle.RIBBON) {
            OptionsSliderItem(
                icon = Icons.Default.Rectangle,
                label = "Flat",
                sliderValue = ribbonFlatness,
                sliderRange = 0.1f..1.0f, // iPhone과 동일한 범위
                onSliderChange = onRibbonFlatnessChange,
                modifier = Modifier.width(80.dp)
            )
        }
        
        // Reset Button (iPhone과 동일)
        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF44336).copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.width(100.dp) // 너비를 80dp에서 100dp로 증가
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OptionsSliderItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    isToggle: Boolean = false,
    toggleValue: Boolean = false,
    onToggleChange: (Boolean) -> Unit = {},
    sliderValue: Float = 0f,
    sliderRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onSliderChange: (Float) -> Unit = {}
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 아이콘과 라벨
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isToggle) {
            // Toggle 버튼
            Switch(
                checked = toggleValue,
                onCheckedChange = onToggleChange,
                modifier = Modifier.size(24.dp)
            )
        } else {
            // 슬라이더
            Slider(
                value = sliderValue,
                onValueChange = onSliderChange,
                valueRange = sliderRange,
                modifier = Modifier.width(70.dp)
            )
        }
    }
}

// 아이콘 매핑 함수들
private fun getStyleIcon(style: RenderStyle): ImageVector {
    return when (style) {
        RenderStyle.RIBBON -> Icons.Default.ShowChart // waveform.path.ecg
        RenderStyle.SPHERES -> Icons.Default.Circle // circle.fill
        RenderStyle.STICKS -> Icons.Default.ViewHeadline // line.3.horizontal
        RenderStyle.CARTOON -> Icons.Default.WaterfallChart // waveform.path
        RenderStyle.SURFACE -> Icons.Default.Public // globe
    }
}

private fun getColorModeIcon(mode: ColorMode): ImageVector {
    return when (mode) {
        ColorMode.ELEMENT -> Icons.Default.Science // atom
        ColorMode.CHAIN -> Icons.Default.Link // link
        ColorMode.UNIFORM -> Icons.Default.Brush // paintbrush
        ColorMode.SECONDARY_STRUCTURE -> Icons.Default.WaterfallChart // waveform
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
    // 이 함수는 더 이상 사용하지 않음 (SecondaryOptionsBar로 대체)
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
