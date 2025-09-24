package com.avas.proteinviewer.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.util.LanguageHelper

@Composable
private fun getVisualizationFeatures() = listOf(
    LanguageHelper.localizedText(
        "SceneKit 기반 고품질 3D 렌더링",
        "High-quality 3D rendering based on SceneKit"
    ),
    LanguageHelper.localizedText(
        "실시간 인터랙티브 조작",
        "Real-time interactive manipulation"
    ),
    LanguageHelper.localizedText(
        "다양한 렌더링 스타일 지원",
        "Support for various rendering styles"
    ),
    LanguageHelper.localizedText(
        "LOD(Level of Detail) 최적화",
        "LOD (Level of Detail) optimization"
    )
)

@Composable
private fun getRenderingStyleFeatures() = listOf(
    LanguageHelper.localizedText(
        "Spheres: 원자 구체 표현",
        "Spheres: Atomic sphere representation"
    ),
    LanguageHelper.localizedText(
        "Sticks: 결합선 표현",
        "Sticks: Bond line representation"
    ),
    LanguageHelper.localizedText(
        "Cartoon: 만화 스타일 표현",
        "Cartoon: Cartoon style representation"
    ),
    LanguageHelper.localizedText(
        "Surface: 표면 표현",
        "Surface: Surface representation"
    )
)

@Composable
private fun getColorModeFeatures() = listOf(
    LanguageHelper.localizedText(
        "Element: 원소별 색상",
        "Element: Element-based coloring"
    ),
    LanguageHelper.localizedText(
        "Chain: 체인별 색상",
        "Chain: Chain-based coloring"
    ),
    LanguageHelper.localizedText(
        "Secondary Structure: 2차 구조별 색상",
        "Secondary Structure: Secondary structure-based coloring"
    ),
    LanguageHelper.localizedText(
        "Uniform: 단일 색상",
        "Uniform: Single color"
    )
)

@Composable
private fun getInteractionFeatures() = listOf(
    LanguageHelper.localizedText(
        "회전: 드래그로 3D 모델 회전",
        "Rotation: Rotate 3D model by dragging"
    ),
    LanguageHelper.localizedText(
        "확대/축소: 핀치 제스처",
        "Zoom: Pinch gesture for zoom in/out"
    ),
    LanguageHelper.localizedText(
        "슬라이스: 특정 부분만 표시",
        "Slice: Display only specific parts"
    ),
    LanguageHelper.localizedText(
        "자동 회전: 더블 탭으로 토글",
        "Auto rotation: Toggle with double tap"
    )
)

@Composable
private fun getHighlightFeatures() = listOf(
    LanguageHelper.localizedText(
        "Chain 하이라이트: 특정 체인 강조",
        "Chain Highlight: Emphasize specific chains"
    ),
    LanguageHelper.localizedText(
        "Ligand 하이라이트: 리간드 강조",
        "Ligand Highlight: Emphasize ligands"
    ),
    LanguageHelper.localizedText(
        "Pocket 하이라이트: 포켓 강조",
        "Pocket Highlight: Emphasize pockets"
    ),
    LanguageHelper.localizedText(
        "전체 체인 하이라이트: 모든 체인 강조",
        "All Chain Highlight: Emphasize all chains"
    )
)

@Composable
private fun getInformationFeatures() = listOf(
    LanguageHelper.localizedText(
        "PDB 데이터베이스 연동",
        "PDB database integration"
    ),
    LanguageHelper.localizedText(
        "단백질 구조 정보",
        "Protein structure information"
    ),
    LanguageHelper.localizedText(
        "원자 정보 표시",
        "Atomic information display"
    ),
    LanguageHelper.localizedText(
        "구조 단계별 정보",
        "Hierarchical structure information"
    )
)

@Composable
private fun getPerformanceFeatures() = listOf(
    LanguageHelper.localizedText(
        "지오메트리 캐싱",
        "Geometry caching"
    ),
    LanguageHelper.localizedText(
        "LOD 시스템",
        "LOD system"
    ),
    LanguageHelper.localizedText(
        "메모리 최적화",
        "Memory optimization"
    ),
    LanguageHelper.localizedText(
        "배터리 효율성",
        "Battery efficiency"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesScreen(
    onNavigateBack: () -> Unit
) {
    val visualizationFeatures = getVisualizationFeatures()
    val renderingStyleFeatures = getRenderingStyleFeatures()
    val colorModeFeatures = getColorModeFeatures()
    val interactionFeatures = getInteractionFeatures()
    val highlightFeatures = getHighlightFeatures()
    val informationFeatures = getInformationFeatures()
    val performanceFeatures = getPerformanceFeatures()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageHelper.localizedText("기능", "Features")) },
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
            // 3D 단백질 시각화
            item {
                FeatureCard(
                    title = LanguageHelper.localizedText("3D 단백질 시각화", "3D Protein Visualization"),
                    icon = Icons.Default.ViewInAr,
                    color = Color(0xFF2196F3), // Blue
                    features = visualizationFeatures
                )
            }

            // 렌더링 스타일
            item {
                FeatureCard(
                    title = LanguageHelper.localizedText("렌더링 스타일", "Rendering Styles"),
                    icon = Icons.Default.Palette,
                    color = Color(0xFF4CAF50), // Green
                    features = renderingStyleFeatures
                )
            }

            // 색상 모드
            item {
                FeatureCard(
                    title = LanguageHelper.localizedText("색상 모드", "Color Modes"),
                    icon = Icons.Default.Palette,
                    color = Color(0xFF9C27B0), // Purple
                    features = colorModeFeatures
                )
            }

            // 인터랙션 기능
            item {
                FeatureCard(
                    title = LanguageHelper.localizedText("인터랙션 기능", "Interaction Features"),
                    icon = Icons.Default.TouchApp,
                    color = Color(0xFFFF9800), // Orange
                    features = interactionFeatures
                )
            }

            // 하이라이트 기능
            item {
                FeatureCard(
                    title = LanguageHelper.localizedText("하이라이트 기능", "Highlight Features"),
                    icon = Icons.Default.Star,
                    color = Color(0xFFF44336), // Red
                    features = highlightFeatures
                )
            }

            // 정보 제공
            item {
                FeatureCard(
                    title = LanguageHelper.localizedText("정보 제공", "Information Features"),
                    icon = Icons.Default.Info,
                    color = Color(0xFF009688), // Teal
                    features = informationFeatures
                )
            }

            // 성능 최적화
            item {
                FeatureCard(
                    title = LanguageHelper.localizedText("성능 최적화", "Performance Optimization"),
                    icon = Icons.Default.Speed,
                    color = Color(0xFF3F51B5), // Indigo
                    features = performanceFeatures
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    icon: ImageVector,
    color: Color,
    features: List<String>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = color.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 기능 목록
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                features.forEach { feature ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}