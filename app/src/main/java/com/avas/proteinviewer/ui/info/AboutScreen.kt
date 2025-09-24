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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.util.LanguageHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val versionName = "1.0.0"
    val buildNumber = "1"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageHelper.localizedText("정보", "About")) },
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
            // 헤더 카드
            item {
                HeaderCard(versionName = versionName, buildNumber = buildNumber)
            }

            item {
                Divider()
            }

            // 앱 설명
            item {
                DescriptionCard()
            }

            // 주요 기능
            item {
                FeatureCard()
            }

            item {
                Divider()
            }

            // 개발자 정보
            item {
                DeveloperCard()
            }
        }
    }
}

@Composable
private fun HeaderCard(versionName: String, buildNumber: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Science,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(60.dp)
            )
            
            Text(
                text = "ProteinViewerApp",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = LanguageHelper.localizedText("버전 $versionName (빌드 $buildNumber)", "Version $versionName (Build $buildNumber)"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DescriptionCard() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = LanguageHelper.localizedText("ProteinApp 소개", "About ProteinApp"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = LanguageHelper.localizedText(
                "ProteinApp은 생물학 교육을 위한 3D 단백질 구조 시각화 앱입니다. RCSB PDB 데이터베이스와 연동하여 실제 단백질 구조를 다운로드하고, 인터랙티브한 3D 환경에서 탐색할 수 있습니다.",
                "ProteinApp is a 3D protein structure visualization app for biology education. It integrates with the RCSB PDB database to download real protein structures and explore them in an interactive 3D environment."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FeatureCard() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = LanguageHelper.localizedText("주요 기능", "Key Features"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeatureRow(
                title = LanguageHelper.localizedText("3D 단백질 시각화", "3D Protein Visualization"),
                description = LanguageHelper.localizedText(
                    "SceneKit 기반 고품질 3D 렌더링으로 실제 단백질 구조를 정확하게 표시", 
                    "High-quality 3D rendering based on SceneKit for accurate display of real protein structures"
                )
            )
            FeatureRow(
                title = LanguageHelper.localizedText("다양한 렌더링 스타일", "Various Rendering Styles"),
                description = LanguageHelper.localizedText(
                    "Spheres, Sticks, Cartoon, Surface 등 4가지 시각화 모드", 
                    "4 visualization modes: Spheres, Sticks, Cartoon, Surface"
                )
            )
            FeatureRow(
                title = LanguageHelper.localizedText("색상 모드", "Color Modes"),
                description = LanguageHelper.localizedText(
                    "원소별, 체인별, 2차 구조별 색상으로 단백질 구조 이해", 
                    "Element, chain, and secondary structure color modes for better understanding"
                )
            )
            FeatureRow(
                title = LanguageHelper.localizedText("실시간 인터랙션", "Real-time Interaction"),
                description = LanguageHelper.localizedText(
                    "직관적인 터치 제스처로 회전, 확대/축소, 이동 가능", 
                    "Intuitive touch gestures for rotation, zoom, and pan"
                )
            )
            FeatureRow(
                title = LanguageHelper.localizedText("성능 최적화", "Performance Optimized"),
                description = LanguageHelper.localizedText(
                    "대용량 구조에서도 부드러운 렌더링 성능", 
                    "Smooth rendering performance even for large structures"
                )
            )
        }
    }
}

@Composable
private fun FeatureRow(title: String, description: String) {
    Row {
        Icon(
            Icons.Default.Science,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeveloperCard() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = LanguageHelper.localizedText("개발자", "Developer"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = "AVAS",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = LanguageHelper.localizedText(
                "© 2025 AVAS. 모든 권리 보유.",
                "© 2025 AVAS. All rights reserved."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}