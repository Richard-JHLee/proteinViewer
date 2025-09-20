package com.avas.proteinviewer.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.data.model.FilamentStructure
import com.avas.proteinviewer.ui.protein.ProteinViewerView
import com.avas.proteinviewer.ui.protein.ProteinInfoPanel
import com.avas.proteinviewer.ui.protein.ProteinInfoBottomSheet
import com.avas.proteinviewer.data.converter.StructureConverter

@Composable
fun ResponsiveProteinViewer(
    structure: PDBStructure?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    when {
        isTablet -> {
            // 태블릿: 가로 분할 (50:50)
            Row(modifier = modifier.fillMaxSize()) {
                ProteinViewerView(
                    structure = structure,
                    modifier = Modifier.weight(1f)
                )
                ProteinInfoPanel(
                    structure = structure,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        isLandscape -> {
            // 폰 가로모드: Bottom Sheet로 정보 표시
            ProteinInfoBottomSheet(
                structure = structure,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onDismiss = { /* TODO: Dismiss 처리 */ }
            )
        }
        else -> {
            // 폰 세로모드: 세로 분할 (40:60 - iPhone과 동일)
            Column(modifier = modifier.fillMaxSize()) {
                ProteinViewerView(
                    structure = structure,
                    modifier = Modifier.weight(0.4f)
                )
                ProteinInfoPanel(
                    structure = structure,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.weight(0.6f)
                )
            }
        }
    }
}



@Composable
fun ResponsiveMainContent(
    structure: PDBStructure?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    if (structure != null) {
        when {
            isTablet -> {
                // 태블릿: 가로 분할
                ResponsiveProteinViewer(
                    structure = structure,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    modifier = modifier
                )
            }
            isLandscape -> {
            // 폰 가로모드: Bottom Sheet로 정보 표시
            ProteinInfoBottomSheet(
                structure = structure,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onDismiss = { /* TODO: Dismiss 처리 */ }
            )
            }
            else -> {
                // 폰 세로모드: 세로 분할
                ResponsiveProteinViewer(
                    structure = structure,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    modifier = modifier
                )
            }
        }
    } else {
        // 로딩/에러 상태는 모든 화면 크기에서 동일
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            // 로딩/에러 콘텐츠는 MainScreen에서 처리
        }
    }
}
