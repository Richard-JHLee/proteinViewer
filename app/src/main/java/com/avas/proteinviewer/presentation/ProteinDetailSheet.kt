package com.avas.proteinviewer.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.domain.model.ProteinInfo
import com.avas.proteinviewer.domain.model.ResearchDetailType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 아이폰 InfoSheet와 완전히 동일한 단백질 상세 정보 모달
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProteinDetailSheet(
    protein: ProteinInfo,
    uiState: ProteinUiState,
    onDismiss: () -> Unit,
    onView3D: (String) -> Unit,
    onLoadDiseaseAssociations: (String) -> Unit,
    onLoadResearchStatus: (String) -> Unit,
    onLoadFunctionDetails: (String, String) -> Unit,
    onLoadPrimaryStructure: (String) -> Unit,
    onLoadSecondaryStructure: (String) -> Unit,
    onLoadTertiaryStructure: (String) -> Unit,
    onLoadQuaternaryStructure: (String) -> Unit,
    onLoadRelatedProteins: (String) -> Unit,
    onShowResearchDetail: (ResearchDetailType) -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    // 아이폰과 동일: onAppear에서 API 호출
    LaunchedEffect(protein.id) {
        onLoadDiseaseAssociations(protein.id)
        onLoadResearchStatus(protein.id)
    }
    
    // Experimental Details도 자동 로드 (아이폰과 동일)
    LaunchedEffect(protein.id) {
        // ViewModel에서 loadExperimentalDetails 호출 필요
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.95f),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Navigation Bar (아이폰과 동일: 타이틀 + 백 버튼)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back 버튼 (아이폰과 동일)
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Back",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    // Title (아이폰과 동일)
                    Text(
                        text = "Protein Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // 우측 공간 (대칭을 위해)
                    Spacer(modifier = Modifier.width(80.dp))
                }
            }
            
            HorizontalDivider()
            
            // Section Navigation Bar (아이폰과 동일: 5개 섹션 버튼)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("Overview", Icons.Default.Info, 0),
                        Triple("Function", Icons.Default.Functions, 1),
                        Triple("Structure", Icons.Default.ViewInAr, 2),
                        Triple("Disease", Icons.Default.MedicalServices, 3),
                        Triple("Research", Icons.Default.Science, 4)
                    ).forEach { (title, icon, sectionIndex) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    // Scroll to section (simplified - just scroll down)
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(sectionIndex * 600)
                                    }
                                },
                            color = Color(protein.category.color).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color(protein.category.color),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            
            // Scrollable Content
                Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Header Section (아이폰 HeaderSectionView - #overview)
                HeaderSection(protein = protein)
                
                    // 2. Function Section (아이폰 MainInfoSectionView - #function)
                    FunctionSection(
                        protein = protein,
                        onViewDetails = {
                            onLoadFunctionDetails(protein.id, protein.description)
                        },
                        onLoadPrimaryStructure = onLoadPrimaryStructure,
                        onLoadSecondaryStructure = onLoadSecondaryStructure,
                        onLoadTertiaryStructure = onLoadTertiaryStructure,
                        onLoadQuaternaryStructure = onLoadQuaternaryStructure
                    )
                
                // 3. Structure Section (아이폰 DetailedInfoSectionView - #structure)
                StructureSection(
                    protein = protein,
                    uiState = uiState,
                    onViewRelatedProteins = {
                        onLoadRelatedProteins(protein.id)
                    }
                )
                
                // 4. Additional Information (아이폰 AdditionalInfoSectionView)
                AdditionalInfoSection(protein = protein)
                
                // 5. Disease Association (#disease) - 실제 API 데이터
                DiseaseSection(
                    protein = protein,
                    uiState = uiState
                )
                
                // 6. Research Status (#research) - 실제 API 데이터
                ResearchSection(
                    protein = protein,
                    uiState = uiState,
                    onShowResearchDetail = onShowResearchDetail
                )
                
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // Action Buttons (아이폰 ActionButtonsSectionView와 동일)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // View 3D Structure 버튼 (아이폰과 동일)
                    Button(
                        onClick = { onView3D(protein.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(protein.category.color)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "View 3D Structure",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Section Composables (아이폰 각 View와 동일)

@Composable
private fun HeaderSection(protein: ProteinInfo) {
    // 아이폰 HeaderSectionView와 동일
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon + Name
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gradient Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(protein.category.color),
                                    Color(protein.category.color).copy(alpha = 0.6f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Name
                Text(
                    text = protein.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
            }
            
            // PDB ID + Category Tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PDB ID Tag
                Surface(
                    color = Color(protein.category.color).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color(protein.category.color),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "PDB ${protein.id}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(protein.category.color),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Category Tag
                Surface(
                    color = Color(protein.category.color).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = protein.category.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FunctionSection(
    protein: ProteinInfo,
    onViewDetails: () -> Unit,
    onLoadPrimaryStructure: (String) -> Unit,
    onLoadSecondaryStructure: (String) -> Unit,
    onLoadTertiaryStructure: (String) -> Unit,
    onLoadQuaternaryStructure: (String) -> Unit
) {
    var selectedButton by remember { mutableStateOf<SelectedButton>(SelectedButton.NONE) }
    
    // 아이폰 MainInfoSectionView - Function Summary
    DetailInfoCard(
        icon = Icons.Default.Functions,
        title = "Function Summary",
        tint = Color(protein.category.color)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = protein.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.5f)
            )
            
            // View Details 버튼 (아이폰과 동일)
            TextButton(
                onClick = onViewDetails
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View Details",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color(protein.category.color)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(protein.category.color),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    // 3개 핵심 버튼 (아이폰과 동일: Structure, Coloring, Interact)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Structure 버튼
        MetricPillButton(
            title = "Structure",
            value = stringResource(id = com.avas.proteinviewer.R.string.structure_levels_short),
            icon = Icons.Default.GridOn,
            isSelected = selectedButton == SelectedButton.STRUCTURE,
            tint = Color(protein.category.color),
            modifier = Modifier.weight(1f)
        ) {
            selectedButton = if (selectedButton == SelectedButton.STRUCTURE) {
                SelectedButton.NONE
            } else {
                SelectedButton.STRUCTURE
            }
        }
        
        // Coloring 버튼
        MetricPillButton(
            title = "Coloring",
            value = "Element/Chain/SS",
            icon = Icons.Default.Palette,
            isSelected = selectedButton == SelectedButton.COLORING,
            tint = Color(protein.category.color),
            modifier = Modifier.weight(1f)
        ) {
            selectedButton = if (selectedButton == SelectedButton.COLORING) {
                SelectedButton.NONE
            } else {
                SelectedButton.COLORING
            }
        }
        
        // Interact 버튼
        MetricPillButton(
            title = "Interact",
            value = "Rotate/Zoom/Slice",
            icon = Icons.Default.TouchApp,
            isSelected = selectedButton == SelectedButton.INTERACT,
            tint = Color(protein.category.color),
            modifier = Modifier.weight(1f)
        ) {
            selectedButton = if (selectedButton == SelectedButton.INTERACT) {
                SelectedButton.NONE
            } else {
                SelectedButton.INTERACT
            }
        }
    }
    
    // 버튼 선택 시 확장 정보 (여백 없이 바로 아래 표시)
    // Structure 버튼 선택 시 - 4단계 구조 정보
    if (selectedButton == SelectedButton.STRUCTURE) {
        Spacer(modifier = Modifier.height(0.dp)) // 여백 없음
        StructureLevelsCard(
            protein = protein,
            onLevelClick = { level ->
                when (level) {
                    1 -> onLoadPrimaryStructure(protein.id)
                    2 -> onLoadSecondaryStructure(protein.id)
                    3 -> onLoadTertiaryStructure(protein.id)
                    4 -> onLoadQuaternaryStructure(protein.id)
                }
            }
        )
    }
    
    // Coloring 버튼 선택 시 - 색상 구분 방식
    if (selectedButton == SelectedButton.COLORING) {
        Spacer(modifier = Modifier.height(0.dp)) // 여백 없음
        ColoringSchemesCard(protein = protein)
    }
    
    // Interact 버튼 선택 시 - 상호작용 방법
    if (selectedButton == SelectedButton.INTERACT) {
        Spacer(modifier = Modifier.height(0.dp)) // 여백 없음
        InteractionControlsCard(protein = protein)
    }
    
    // 다음 섹션과의 여백
    Spacer(modifier = Modifier.height(4.dp))
    
    // External Resources (아이폰과 동일)
    DetailInfoCard(
        icon = Icons.Default.Link,
        title = "External Resources",
        tint = Color(0xFF007AFF) // Blue
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ExternalLinkRow(
                title = "View on PDB Website",
                subtitle = "rcsb.org/structure/${protein.id}",
                icon = Icons.Default.Language,
                tint = Color(0xFF007AFF)
            )
            
            HorizontalDivider()
            
            ExternalLinkRow(
                title = "View on UniProt",
                subtitle = "Protein sequence & function",
                icon = Icons.Default.Storage,
                tint = Color(0xFF34C759) // Green
            )
        }
    }
}

// Selected Button Enum (아이폰과 동일)
enum class SelectedButton {
    NONE, STRUCTURE, COLORING, INTERACT
}

@Composable
private fun MetricPillButton(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f) // 정사각형 (가로 = 세로)
            .clickable(onClick = onClick),
        color = if (isSelected) tint.copy(alpha = 0.15f) else Color.Transparent,
        border = if (isSelected) {
            BorderStroke(2.dp, tint)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) tint else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StructureLevelsCard(protein: ProteinInfo, onLevelClick: (Int) -> Unit) {
    // 아이폰 structureDetailsView와 동일
    DetailInfoCard(
        icon = Icons.Default.ViewInAr,
        title = "Protein Structure Levels",
        tint = Color(0xFF00BCD4) // Cyan
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                StructureLevel("1", "Primary Structure", "Amino acid sequence", Color(0xFF007AFF)),
                StructureLevel("2", "Secondary Structure", "Alpha helices, beta sheets", Color(0xFF34C759)),
                StructureLevel("3", "Tertiary Structure", "3D protein fold (PDB file)", Color(0xFFFF9500)),
                StructureLevel("4", "Quaternary Structure", "Multi-subunit assembly", Color(0xFF9C27B0))
            ).forEachIndexed { index, level ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 단계 번호
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(level.color.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = level.number,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = level.color
                        )
                    }
                    
                    // 제목과 설명
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = level.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = level.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // API 엔드포인트 버튼 (아이폰과 동일)
                        IconButton(onClick = {
                            onLevelClick(index + 1) // 1, 2, 3, 4
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = level.color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                }
                
                if (index < 3) {
                    HorizontalDivider(modifier = Modifier.alpha(0.5f))
                }
            }
        }
    }
}

data class StructureLevel(
    val number: String,
    val title: String,
    val description: String,
    val color: Color
)

@Composable
private fun ColoringSchemesCard(protein: ProteinInfo) {
    // 아이폰 Coloring 섹션과 동일
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Coloring Schemes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Element Coloring (실제 ColorMaps 색상 사용)
                ColoringSchemeRow(
                    title = "Element Coloring",
                    description = "Color by element type",
                    colorSamples = listOf(
                        "C" to Color(com.avas.proteinviewer.rendering.ColorMaps.cpk("C")),
                        "N" to Color(com.avas.proteinviewer.rendering.ColorMaps.cpk("N")),
                        "O" to Color(com.avas.proteinviewer.rendering.ColorMaps.cpk("O")),
                        "S" to Color(com.avas.proteinviewer.rendering.ColorMaps.cpk("S")),
                        "P" to Color(com.avas.proteinviewer.rendering.ColorMaps.cpk("P")),
                        "H" to Color(com.avas.proteinviewer.rendering.ColorMaps.cpk("H"))
                    )
                )
                
                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                
                // Chain Coloring (실제 ColorMaps 색상 사용)
                ColoringSchemeRow(
                    title = "Chain Coloring",
                    description = "Color by chain",
                    colorSamples = listOf(
                        "A" to Color(com.avas.proteinviewer.rendering.ColorMaps.chainColor("A")),
                        "B" to Color(com.avas.proteinviewer.rendering.ColorMaps.chainColor("B")),
                        "C" to Color(com.avas.proteinviewer.rendering.ColorMaps.chainColor("C")),
                        "D" to Color(com.avas.proteinviewer.rendering.ColorMaps.chainColor("D"))
                    )
                )
                
                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                
                // Secondary Structure (실제 ColorMaps 색상 사용)
                ColoringSchemeRow(
                    title = "Secondary Structure",
                    description = "Color by structure type",
                    colorSamples = listOf(
                        "α-Helix" to Color(com.avas.proteinviewer.rendering.ColorMaps.secondaryStructureColor("HELIX")),
                        "β-Sheet" to Color(com.avas.proteinviewer.rendering.ColorMaps.secondaryStructureColor("SHEET")),
                        "Coil" to Color(com.avas.proteinviewer.rendering.ColorMaps.secondaryStructureColor("COIL"))
                    )
                )
            }
        }
    }
}

@Composable
private fun InteractionControlsCard(protein: ProteinInfo) {
    // 아이폰 Interact 섹션과 동일
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "3D Interaction Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InteractionControlRow(
                    icon = Icons.Default.Sync,
                    title = "Rotate",
                    description = "Rotate structure with single touch",
                    gesture = "Drag"
                )
                
                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                
                InteractionControlRow(
                    icon = Icons.Default.ZoomIn,
                    title = "Zoom",
                    description = "Pinch to zoom in/out",
                    gesture = "Pinch"
                )
                
                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                
                InteractionControlRow(
                    icon = Icons.AutoMirrored.Filled.CallSplit,
                    title = "Slice",
                    description = "Slice structure with plane",
                    gesture = "Slider"
                )
                
                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                
                InteractionControlRow(
                    icon = Icons.Default.TouchApp,
                    title = "Select",
                    description = "Select atom/residue and show info",
                    gesture = "Tap"
                )
                
                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                
                InteractionControlRow(
                    icon = Icons.Default.RestartAlt,
                    title = "Reset View",
                    description = "Return to initial view",
                    gesture = "Double Tap"
                )
            }
        }
    }
}

@Composable
private fun ColoringSchemeRow(
    title: String,
    description: String,
    colorSamples: List<Pair<String, Color>>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Color samples
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colorSamples.forEach { (label, color) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(color, CircleShape)
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractionControlRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    gesture: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = gesture,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ExternalLinkRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: Open URL */ },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StructureSection(
    protein: ProteinInfo,
    uiState: ProteinUiState,
    onViewRelatedProteins: () -> Unit
) {
    // 아이폰 DetailedInfoSectionView - Additional Information (Method, Resolution, etc.)
    DetailInfoCard(
        icon = Icons.Default.Info,
        title = "Additional Information",
        tint = MaterialTheme.colorScheme.secondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 로딩 상태 (아이폰과 동일)
            if (uiState.isExperimentalDetailsLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Loading experimental details...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 실제 API 데이터 표시
            else if (uiState.experimentalDetails != null) {
                val details = uiState.experimentalDetails!!
                
                if (details.experimentalMethod != null) {
                    InfoRowWithIcon(
                        icon = Icons.Default.Dns,
                        title = "Structure Type",
                        value = details.experimentalMethod!!,
                        tint = Color(protein.category.color)
                    )
                }
                
                if (details.resolution != null) {
                    InfoRowWithIcon(
                        icon = Icons.Default.Straighten,
                        title = "Resolution",
                        value = String.format("%.2f Å", details.resolution),
                        tint = Color(protein.category.color)
                    )
                }
                
                if (details.organism != null) {
                    InfoRowWithIcon(
                        icon = Icons.Default.Person,
                        title = "Organism",
                        value = details.organism!!,
                        tint = Color(protein.category.color)
                    )
                }
                
                if (details.expression != null) {
                    InfoRowWithIcon(
                        icon = Icons.Default.Spa,
                        title = "Expression",
                        value = details.expression!!,
                        tint = Color(protein.category.color)
                    )
                }
                
                if (details.journal != null) {
                    InfoRowWithIcon(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "Journal",
                        value = details.journal!!,
                        tint = Color(protein.category.color)
                    )
                }
            }
            // Fallback 데이터 (API 실패 시, 아이폰과 동일)
            else {
                InfoRowWithIcon(
                    icon = Icons.Default.Dns,
                    title = "Structure Type",
                    value = "X-ray Crystallography",
                    tint = Color(protein.category.color)
                )
                InfoRowWithIcon(
                    icon = Icons.Default.Straighten,
                    title = "Resolution",
                    value = "2.5 Å",
                    tint = Color(protein.category.color)
                )
                InfoRowWithIcon(
                    icon = Icons.Default.Person,
                    title = "Organism",
                    value = "Homo sapiens",
                    tint = Color(protein.category.color)
                )
                InfoRowWithIcon(
                    icon = Icons.Default.Spa,
                    title = "Expression",
                    value = "E. coli",
                    tint = Color(protein.category.color)
                )
            }
            
            // View Details 버튼 (아이폰과 동일: Related Proteins 표시)
            TextButton(
                onClick = onViewRelatedProteins
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View Details",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdditionalInfoSection(protein: ProteinInfo) {
    // 아이폰 AdditionalInfoSectionView - Keywords & Tags
    if (protein.keywords.isNotEmpty()) {
        DetailInfoCard(
            icon = Icons.Default.Key,
            title = "Keywords & Tags",
            tint = Color(protein.category.color)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height((((protein.keywords.size / 3) + 1) * 40).dp)
            ) {
                items(protein.keywords.take(10)) { keyword ->
                    Surface(
                        color = Color(protein.category.color).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = keyword,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiseaseSection(protein: ProteinInfo, uiState: ProteinUiState) {
    var showingAllDiseases by remember { mutableStateOf(false) }
    
    // 아이폰 Disease Association Section
    DetailInfoCard(
        icon = Icons.Default.MedicalServices,
        title = "Disease Association",
        tint = Color(0xFFFF9500) // Orange
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 로딩 상태 (아이폰과 동일)
            if (uiState.isDiseaseLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFFFF9500)
                    )
                    Text(
                        text = "Loading disease data...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 에러 상태 (아이폰과 동일)
            else if (uiState.diseaseError != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No disease data available",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.diseaseError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 데이터 표시 (아이폰과 동일)
            else if (uiState.diseaseSummary != null && uiState.diseaseAssociations.isNotEmpty()) {
                val summary = uiState.diseaseSummary!!
                val diseases = uiState.diseaseAssociations
                
                // Summary Header (실제 API 데이터)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DiseaseSummaryCard(
                        title = "Total",
                        value = "${summary.total}",
                        icon = Icons.Default.Warning,
                        color = Color(0xFFFF9500),
                        modifier = Modifier.weight(1f)
                    )
                    
                    DiseaseSummaryCard(
                        title = "High Risk",
                        value = "${summary.highRisk}",
                        icon = Icons.Default.Error,
                        color = Color.Red,
                        modifier = Modifier.weight(1f)
                    )
                    
                    DiseaseSummaryCard(
                        title = "Medium",
                        value = "${summary.mediumRisk}",
                        icon = Icons.Default.Info,
                        color = Color(0xFFFF9500),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                HorizontalDivider()
                
                // Disease List (실제 API 데이터)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val displayedDiseases = if (showingAllDiseases) diseases else diseases.take(3)
                    
                    displayedDiseases.forEach { disease ->
                        DiseaseItemCard(
                            diseaseName = disease.name,
                            description = disease.description,
                            riskColor = Color(disease.riskLevel.color)
                        )
                    }
                    
                    if (diseases.size > 3) {
                        TextButton(
                            onClick = { showingAllDiseases = !showingAllDiseases }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showingAllDiseases) "Show Less" else "Show All (${diseases.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF007AFF)
                                )
                                Icon(
                                    imageVector = if (showingAllDiseases) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            // Empty state (아이폰과 동일)
            else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No disease associations found",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ResearchSection(
    protein: ProteinInfo, 
    uiState: ProteinUiState,
    onShowResearchDetail: (ResearchDetailType) -> Unit
) {
    // 아이폰 Research Status Section
    DetailInfoCard(
        icon = Icons.Default.Science,
        title = "Research Status",
        tint = Color(0xFF9C27B0) // Purple
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 로딩 상태 (아이폰과 동일)
            if (uiState.isResearchLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFF9C27B0)
                    )
                    Text(
                        text = "Loading research data...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 에러 상태 (아이폰과 동일)
            else if (uiState.researchError != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No research data available",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.researchError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 데이터 표시 (실제 API 데이터 또는 기본값)
            else {
                val research = uiState.researchStatus
                
                // Research Metrics (항상 클릭 가능한 카드 표시)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Active Studies
                    ResearchMetricCard(
                        title = "Active Studies",
                        value = "${research?.activeStudies ?: 0}",
                        icon = Icons.Default.Science,
                        color = Color(0xFF34C759), // Green
                        modifier = Modifier.weight(1f),
                        onClick = { onShowResearchDetail(ResearchDetailType.ACTIVE_STUDIES) }
                    )
                    
                    // Clinical Trials
                    ResearchMetricCard(
                        title = "Clinical Trials",
                        value = "${research?.clinicalTrials ?: 0}",
                        icon = Icons.Default.MedicalServices,
                        color = Color(0xFF007AFF), // Blue
                        modifier = Modifier.weight(1f),
                        onClick = { onShowResearchDetail(ResearchDetailType.CLINICAL_TRIALS) }
                    )
                    
                    // Publications
                    ResearchMetricCard(
                        title = "Publications",
                        value = "${research?.publications ?: 0}",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        color = Color(0xFF9C27B0), // Purple
                        modifier = Modifier.weight(1f),
                        onClick = { onShowResearchDetail(ResearchDetailType.PUBLICATIONS) }
                    )
                }
                
                HorizontalDivider()
                
                // Latest Update (실제 API 데이터)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Latest Update",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.researchSummary?.lastUpdated ?: "Unknown",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// Helper Composables for Disease Section

@Composable
private fun DiseaseSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DiseaseItemCard(
    diseaseName: String,
    description: String,
    riskColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Risk indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(riskColor, CircleShape)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = diseaseName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ResearchMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// MARK: - Helper Composables

@Composable
private fun DetailInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title with icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(tint.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Content
            content()
        }
    }
}

@Composable
private fun InfoRowWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    tint: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(100.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
