package com.avas.proteinviewer.ui.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.util.LanguageHelper

@Composable
private fun getBasicUsageSteps() = listOf(
    LanguageHelper.localizedText("단백질 선택", "Select Protein") to 
    LanguageHelper.localizedText(
        "홈 화면에서 단백질 라이브러리를 선택하거나 PDB ID를 입력하여 단백질을 로드합니다.",
        "Select a protein from the library on the home screen or enter a PDB ID to load a protein."
    ),
    LanguageHelper.localizedText("3D 뷰어 탐색", "3D Viewer Navigation") to 
    LanguageHelper.localizedText(
        "단백질이 로드되면 3D 뷰어에서 구조를 탐색할 수 있습니다.",
        "Once the protein is loaded, you can explore the structure in the 3D viewer."
    ),
    LanguageHelper.localizedText("렌더링 스타일 변경", "Change Rendering Style") to 
    LanguageHelper.localizedText(
        "하단 컨트롤 바에서 Spheres, Sticks, Cartoon, Surface 스타일을 선택할 수 있습니다.",
        "You can select Spheres, Sticks, Cartoon, or Surface styles from the bottom control bar."
    )
)

@Composable
private fun getViewerModeFeatures() = listOf(
    GuideFeatureItem(
        title = LanguageHelper.localizedText("뷰어 모드 전환", "Switch to Viewer Mode"),
        description = LanguageHelper.localizedText(
            "상단의 'Viewer' 버튼을 탭하여 뷰어 모드로 전환합니다.",
            "Tap the 'Viewer' button at the top to switch to viewer mode."
        )
    ),
    GuideFeatureItem(
        title = LanguageHelper.localizedText("렌더링 스타일 선택", "Select Rendering Style"),
        description = LanguageHelper.localizedText(
            "하단 Primary Bar에서 원하는 렌더링 스타일을 선택합니다.",
            "Select your desired rendering style from the bottom Primary Bar."
        )
    ),
    GuideFeatureItem(
        title = LanguageHelper.localizedText("색상 모드 선택", "Select Color Mode"),
        description = LanguageHelper.localizedText(
            "Color Schemes에서 Element, Chain, Secondary Structure 색상 모드를 선택합니다.",
            "Select Element, Chain, or Secondary Structure color modes from Color Schemes."
        )
    ),
    GuideFeatureItem(
        title = LanguageHelper.localizedText("옵션 조정", "Adjust Options"),
        description = LanguageHelper.localizedText(
            "Options에서 회전, 확대/축소, 투명도, 원자 크기를 조정할 수 있습니다.",
            "You can adjust rotation, zoom, transparency, and atom size in Options."
        )
    )
)

@Composable
private fun getInteractionGuide() = listOf(
    GuideFeatureItem(
        title = LanguageHelper.localizedText("드래그", "Drag"),
        description = LanguageHelper.localizedText("3D 모델 회전", "Rotate 3D model")
    ),
    GuideFeatureItem(
        title = LanguageHelper.localizedText("핀치", "Pinch"),
        description = LanguageHelper.localizedText("확대/축소", "Zoom in/out")
    ),
    GuideFeatureItem(
        title = LanguageHelper.localizedText("더블 탭", "Double Tap"),
        description = LanguageHelper.localizedText("자동 회전 토글", "Toggle auto rotation")
    ),
    GuideFeatureItem(
        title = LanguageHelper.localizedText("롱 프레스", "Long Press"),
        description = LanguageHelper.localizedText("원자 정보 표시", "Show atom information")
    )
)

@Composable
private fun getTipsAndTricks() = listOf(
    GuideFeatureItem(
        title = LanguageHelper.localizedText("하이라이트 기능", "Highlight Feature"),
        description = LanguageHelper.localizedText(
            "Chain, Ligand, Pocket을 선택하여 특정 부분을 하이라이트할 수 있습니다.",
            "Select Chain, Ligand, or Pocket to highlight specific parts of the structure."
        )
    ),
    GuideFeatureItem(
        title = LanguageHelper.localizedText("슬라이스 기능", "Slice Feature"),
        description = LanguageHelper.localizedText(
            "복잡한 구조에서 특정 부분만 보기 위해 슬라이스 기능을 사용하세요.",
            "Use the slice feature to view only specific parts of complex structures."
        )
    ),
    GuideFeatureItem(
        title = LanguageHelper.localizedText("색상 모드 활용", "Color Mode Usage"),
        description = LanguageHelper.localizedText(
            "Secondary Structure 모드에서 α-helix와 β-sheet를 쉽게 구분할 수 있습니다.",
            "In Secondary Structure mode, you can easily distinguish between α-helix and β-sheet."
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGuideScreen(
    onNavigateBack: () -> Unit
) {
    val basicUsageSteps = getBasicUsageSteps()
    val viewerModeFeatures = getViewerModeFeatures()
    val interactionGuide = getInteractionGuide()
    val tipsAndTricks = getTipsAndTricks()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageHelper.localizedText("사용자 가이드", "User Guide")) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 기본사용법 Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = LanguageHelper.localizedText("기본사용법", "Basic Usage"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            basicUsageSteps.forEachIndexed { index, (title, description) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 뷰어모드 사용법 Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = LanguageHelper.localizedText("뷰어모드 사용법", "Viewer Mode Usage"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            viewerModeFeatures.forEach { feature ->
                                FeatureRow(feature, MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }

            // 인터랙션 가이드 Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = LanguageHelper.localizedText("인터랙션 가이드", "Interaction Guide"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            interactionGuide.forEach { interaction ->
                                FeatureRow(interaction, MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // 팁과 요령 Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = LanguageHelper.localizedText("팁과 요령", "Tips & Tricks"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            tipsAndTricks.forEach { tip ->
                                FeatureRow(tip, MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(item: GuideFeatureItem, iconColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = when(item.title) {
                // 인터랙션 가이드 아이콘들 (영어/한국어 모두 지원)
                "드래그", "Drag" -> Icons.Default.RotateRight
                "핀치", "Pinch" -> Icons.Default.ZoomIn
                "더블 탭", "Double Tap" -> Icons.Default.Refresh
                "롱 프레스", "Long Press" -> Icons.Default.Info
                // 팁과 요령 아이콘들 (영어/한국어 모두 지원)
                "하이라이트 기능", "Highlight Feature" -> Icons.Default.Star
                "슬라이스 기능", "Slice Feature" -> Icons.Default.ContentCut
                "색상 모드 활용", "Color Mode Usage" -> Icons.Default.Palette
                // 뷰어모드 사용법 아이콘들
                else -> Icons.Default.CheckCircle
            },
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.padding(top = 2.dp).size(20.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class GuideFeatureItem(
    val title: String,
    val description: String
)