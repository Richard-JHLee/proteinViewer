package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.data.preferences.PerformanceSettings
import com.avas.proteinviewer.domain.model.MenuItemType
import com.avas.proteinviewer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuDetailScreen(
    menuItem: MenuItemType,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(menuItem.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            when (menuItem) {
                MenuItemType.ABOUT -> AboutView()
                MenuItemType.USER_GUIDE -> UserGuideView()
                MenuItemType.FEATURES -> FeaturesView()
                MenuItemType.SETTINGS -> SettingsView()
                MenuItemType.HELP -> HelpView()
                MenuItemType.PRIVACY -> PrivacyView()
                MenuItemType.TERMS -> TermsView()
                MenuItemType.LICENSE -> LicenseView()
            }
        }
    }
}

@Composable
private fun AboutView() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 앱 아이콘 및 이름
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Science,
                contentDescription = "App Icon",
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = context.getString(R.string.about_app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = context.getString(R.string.about_version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
                HorizontalDivider()
        
        // 앱 설명
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = context.getString(R.string.about_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = context.getString(R.string.about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // 주요 기능
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = context.getString(R.string.about_features_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureRow(
                    icon = Icons.Default.ViewInAr,
                    title = context.getString(R.string.about_feature_1_title),
                    description = context.getString(R.string.about_feature_1_desc)
                )
                FeatureRow(
                    icon = Icons.Default.Palette,
                    title = context.getString(R.string.about_feature_2_title),
                    description = context.getString(R.string.about_feature_2_desc)
                )
                FeatureRow(
                    icon = Icons.Default.ColorLens,
                    title = context.getString(R.string.about_feature_3_title),
                    description = context.getString(R.string.about_feature_3_desc)
                )
                FeatureRow(
                    icon = Icons.Default.TouchApp,
                    title = context.getString(R.string.about_feature_4_title),
                    description = context.getString(R.string.about_feature_4_desc)
                )
                FeatureRow(
                    icon = Icons.Default.Info,
                    title = context.getString(R.string.about_feature_5_title),
                    description = context.getString(R.string.about_feature_5_desc)
                )
                FeatureRow(
                    icon = Icons.Default.Search,
                    title = context.getString(R.string.about_feature_6_title),
                    description = context.getString(R.string.about_feature_6_desc)
                )
                FeatureRow(
                    icon = Icons.Default.Speed,
                    title = context.getString(R.string.about_feature_7_title),
                    description = context.getString(R.string.about_feature_7_desc)
                )
            }
        }
        
                HorizontalDivider()
        
        // 개발자 정보
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = context.getString(R.string.about_developer_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = context.getString(R.string.about_developer_name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = context.getString(R.string.about_copyright),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun UserGuideView() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 기본 사용법
        GuideSectionCard(
            title = context.getString(R.string.guide_basic_usage_title),
            icon = Icons.Default.PlayArrow
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GuideStepItem(
                    step = "1",
                    title = context.getString(R.string.guide_step_1_title),
                    description = context.getString(R.string.guide_step_1_desc)
                )
                GuideStepItem(
                    step = "2",
                    title = context.getString(R.string.guide_step_2_title),
                    description = context.getString(R.string.guide_step_2_desc)
                )
                GuideStepItem(
                    step = "3",
                    title = context.getString(R.string.guide_step_3_title),
                    description = context.getString(R.string.guide_step_3_desc)
                )
            }
        }
        
        // 뷰어 모드 사용법
        GuideSectionCard(
            title = context.getString(R.string.guide_viewer_mode_title),
            icon = Icons.Default.RemoveRedEye
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GuideStepItem(
                    step = "1",
                    title = context.getString(R.string.guide_viewer_step_1_title),
                    description = context.getString(R.string.guide_viewer_step_1_desc)
                )
                GuideStepItem(
                    step = "2",
                    title = context.getString(R.string.guide_viewer_step_2_title),
                    description = context.getString(R.string.guide_viewer_step_2_desc)
                )
                GuideStepItem(
                    step = "3",
                    title = context.getString(R.string.guide_viewer_step_3_title),
                    description = context.getString(R.string.guide_viewer_step_3_desc)
                )
                GuideStepItem(
                    step = "4",
                    title = context.getString(R.string.guide_viewer_step_4_title),
                    description = context.getString(R.string.guide_viewer_step_4_desc)
                )
            }
        }
        
        // 인터랙션 가이드
        GuideSectionCard(
            title = context.getString(R.string.guide_interaction_title),
            icon = Icons.Default.TouchApp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InteractionGuideItem(
                    gesture = context.getString(R.string.guide_gesture_drag),
                    description = context.getString(R.string.guide_gesture_drag_desc),
                    icon = Icons.Default.Refresh
                )
                InteractionGuideItem(
                    gesture = context.getString(R.string.guide_gesture_pinch),
                    description = context.getString(R.string.guide_gesture_pinch_desc),
                    icon = Icons.Default.ZoomIn
                )
                InteractionGuideItem(
                    gesture = context.getString(R.string.guide_gesture_double_tap),
                    description = context.getString(R.string.guide_gesture_double_tap_desc),
                    icon = Icons.Default.Autorenew
                )
                InteractionGuideItem(
                    gesture = context.getString(R.string.guide_gesture_long_press),
                    description = context.getString(R.string.guide_gesture_long_press_desc),
                    icon = Icons.Default.Info
                )
            }
        }
        
        // 팁과 요령
        GuideSectionCard(
            title = context.getString(R.string.guide_tips_title),
            icon = Icons.Default.Lightbulb
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TipItem(
                    icon = Icons.Default.Star,
                    title = context.getString(R.string.guide_tip_1_title),
                    description = context.getString(R.string.guide_tip_1_desc)
                )
                TipItem(
                    icon = Icons.Default.Settings,
                    title = context.getString(R.string.guide_tip_2_title),
                    description = context.getString(R.string.guide_tip_2_desc)
                )
                TipItem(
                    icon = Icons.Default.Palette,
                    title = context.getString(R.string.guide_tip_3_title),
                    description = context.getString(R.string.guide_tip_3_desc)
                )
            }
        }
    }
}

@Composable
private fun FeaturesView() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 3D 시각화 기능
        FeatureCardWithList(
            title = context.getString(R.string.features_3d_viz_title),
            icon = Icons.Default.ViewInAr,
            color = Color(0xFF2196F3), // Blue
            features = listOf(
                context.getString(R.string.features_3d_viz_1),
                context.getString(R.string.features_3d_viz_2),
                context.getString(R.string.features_3d_viz_3),
                context.getString(R.string.features_3d_viz_4)
            )
        )
        
        // 렌더링 스타일
        FeatureCardWithList(
            title = context.getString(R.string.features_rendering_title),
            icon = Icons.Default.Palette,
            color = Color(0xFF4CAF50), // Green
            features = listOf(
                context.getString(R.string.features_rendering_1),
                context.getString(R.string.features_rendering_2),
                context.getString(R.string.features_rendering_3),
                context.getString(R.string.features_rendering_4)
            )
        )
        
        // 색상 모드
        FeatureCardWithList(
            title = context.getString(R.string.features_color_title),
            icon = Icons.Default.ColorLens,
            color = Color(0xFF9C27B0), // Purple
            features = listOf(
                context.getString(R.string.features_color_1),
                context.getString(R.string.features_color_2),
                context.getString(R.string.features_color_3),
                context.getString(R.string.features_color_4)
            )
        )
        
        // 인터랙션 기능
        FeatureCardWithList(
            title = context.getString(R.string.features_interaction_title),
            icon = Icons.Default.TouchApp,
            color = Color(0xFFFF9800), // Orange
            features = listOf(
                context.getString(R.string.features_interaction_1),
                context.getString(R.string.features_interaction_2),
                context.getString(R.string.features_interaction_3),
                context.getString(R.string.features_interaction_4)
            )
        )
        
        // 하이라이트 기능
        FeatureCardWithList(
            title = context.getString(R.string.features_highlight_title),
            icon = Icons.Default.Star,
            color = Color(0xFFF44336), // Red
            features = listOf(
                context.getString(R.string.features_highlight_1),
                context.getString(R.string.features_highlight_2),
                context.getString(R.string.features_highlight_3),
                context.getString(R.string.features_highlight_4)
            )
        )
        
        // 정보 제공
        FeatureCardWithList(
            title = context.getString(R.string.features_info_title),
            icon = Icons.Default.Info,
            color = Color(0xFF009688), // Teal
            features = listOf(
                context.getString(R.string.features_info_1),
                context.getString(R.string.features_info_2),
                context.getString(R.string.features_info_3),
                context.getString(R.string.features_info_4)
            )
        )
        
        // 성능 최적화
        FeatureCardWithList(
            title = context.getString(R.string.features_performance_title),
            icon = Icons.Default.Speed,
            color = Color(0xFF3F51B5), // Indigo
            features = listOf(
                context.getString(R.string.features_performance_1),
                context.getString(R.string.features_performance_2),
                context.getString(R.string.features_performance_3),
                context.getString(R.string.features_performance_4)
            )
        )
    }
}

@Composable
private fun SettingsView() {
    val context = LocalContext.current
    val performanceSettings = remember { PerformanceSettings(context) }
    
    // Settings 상태 관찰
    val enableOptimization by performanceSettings.enableOptimization.collectAsState()
    val maxAtomsLimit by performanceSettings.maxAtomsLimit.collectAsState()
    val samplingRatio by performanceSettings.samplingRatio.collectAsState()
    
    // 설정 변경 감지하여 로그 출력
    LaunchedEffect(enableOptimization, maxAtomsLimit, samplingRatio) {
        android.util.Log.d("SettingsView", "⚙️ Settings changed: enableOptimization=$enableOptimization, maxAtomsLimit=$maxAtomsLimit, samplingRatio=$samplingRatio")
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Performance Optimization Section
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = context.getString(R.string.settings_performance_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = context.getString(R.string.settings_performance_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Enable Optimization Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(R.string.settings_enable_optimization),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Switch(
                            checked = enableOptimization,
                            onCheckedChange = { performanceSettings.setEnableOptimization(it) }
                        )
                    }
                    
                    Text(
                        text = context.getString(R.string.settings_enable_optimization_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Max Atoms Limit Slider (only show when optimization is enabled)
            if (enableOptimization) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.settings_max_atoms_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Text(
                                text = maxAtomsLimit.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Slider(
                            value = maxAtomsLimit.toFloat(),
                            onValueChange = { performanceSettings.setMaxAtomsLimit(it.toInt()) },
                            valueRange = 1000f..10000f,
                            steps = 17 // (10000-1000)/500 - 1
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "1000",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = context.getString(R.string.settings_fast_rendering),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = "10000",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = context.getString(R.string.settings_high_quality),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = context.getString(R.string.settings_max_atoms_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Sampling Ratio Slider
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.settings_sampling_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Text(
                                text = String.format("%.1f%%", samplingRatio * 100),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Slider(
                            value = samplingRatio,
                            onValueChange = { performanceSettings.setSamplingRatio(it) },
                            valueRange = 0.05f..0.5f,
                            steps = 44 // (0.5-0.05)/0.01 - 1
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "5%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = context.getString(R.string.settings_fast_processing),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = "50%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = context.getString(R.string.settings_high_quality),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = context.getString(R.string.settings_sampling_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Performance Guide Section
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Performance Guide",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            PerformanceGuideItem(
                icon = Icons.Default.Speed,
                title = "Fast Rendering",
                description = "500-1000 atoms, 5-10% sampling",
                color = Color(0xFF4CAF50) // Green
            )
            
            PerformanceGuideItem(
                icon = Icons.Default.Balance,
                title = "Balanced",
                description = "1500-2500 atoms, 10-20% sampling",
                color = Color(0xFFFF9800) // Orange
            )
            
            PerformanceGuideItem(
                icon = Icons.Default.Star,
                title = "High Quality",
                description = "3000-5000 atoms, 20-50% sampling",
                color = Color(0xFF2196F3) // Blue
            )
        }
        
        // Reset Settings Section
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = context.getString(R.string.settings_reset_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = {
                    performanceSettings.resetToDefaults()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = context.getString(R.string.settings_reset_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpView() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = context.getString(R.string.help_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FAQItem(
                question = context.getString(R.string.help_faq_1_q),
                answer = context.getString(R.string.help_faq_1_a)
            )
            
            FAQItem(
                question = context.getString(R.string.help_faq_2_q),
                answer = context.getString(R.string.help_faq_2_a)
            )
            
            FAQItem(
                question = context.getString(R.string.help_faq_3_q),
                answer = context.getString(R.string.help_faq_3_a)
            )
            
            FAQItem(
                question = context.getString(R.string.help_faq_4_q),
                answer = context.getString(R.string.help_faq_4_a)
            )
            
            FAQItem(
                question = context.getString(R.string.help_faq_5_q),
                answer = context.getString(R.string.help_faq_5_a)
            )
        }
    }
}

@Composable
private fun PrivacyView() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = context.getString(R.string.privacy_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PrivacySection(
                title = context.getString(R.string.privacy_section_1_title),
                content = context.getString(R.string.privacy_section_1_content)
            )
            
            PrivacySection(
                title = context.getString(R.string.privacy_section_2_title),
                content = context.getString(R.string.privacy_section_2_content)
            )
            
            PrivacySection(
                title = context.getString(R.string.privacy_section_3_title),
                content = context.getString(R.string.privacy_section_3_content)
            )
            
            PrivacySection(
                title = context.getString(R.string.privacy_section_4_title),
                content = context.getString(R.string.privacy_section_4_content)
            )
            
            PrivacySection(
                title = context.getString(R.string.privacy_section_5_title),
                content = context.getString(R.string.privacy_section_5_content)
            )
            
            PrivacySection(
                title = context.getString(R.string.privacy_section_6_title),
                content = context.getString(R.string.privacy_section_6_content)
            )
        }
    }
}

@Composable
private fun TermsView() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = context.getString(R.string.terms_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TermsSection(
                title = context.getString(R.string.terms_section_1_title),
                content = context.getString(R.string.terms_section_1_content)
            )
            
            TermsSection(
                title = context.getString(R.string.terms_section_2_title),
                content = context.getString(R.string.terms_section_2_content)
            )
            
            TermsSection(
                title = context.getString(R.string.terms_section_3_title),
                content = context.getString(R.string.terms_section_3_content)
            )
            
            TermsSection(
                title = context.getString(R.string.terms_section_4_title),
                content = context.getString(R.string.terms_section_4_content)
            )
            
            TermsSection(
                title = context.getString(R.string.terms_section_5_title),
                content = context.getString(R.string.terms_section_5_content)
            )
            
            TermsSection(
                title = context.getString(R.string.terms_section_6_title),
                content = context.getString(R.string.terms_section_6_content)
            )
            
            TermsSection(
                title = context.getString(R.string.terms_section_7_title),
                content = context.getString(R.string.terms_section_7_content)
            )
            
            TermsSection(
                title = context.getString(R.string.terms_section_8_title),
                content = context.getString(R.string.terms_section_8_content)
            )
        }
    }
}

@Composable
private fun LicenseView() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // MIT 라이센스 헤더
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = context.getString(R.string.license_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = context.getString(R.string.license_copyright),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
                HorizontalDivider()
        
        // 라이센스 본문
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = context.getString(R.string.license_permission),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = context.getString(R.string.license_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = context.getString(R.string.license_warranty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
                HorizontalDivider()
        
        // 오픈소스 라이브러리
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = context.getString(R.string.license_libraries_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LibraryLicenseItem(
                    name = context.getString(R.string.license_lib_1_name),
                    description = context.getString(R.string.license_lib_1_desc),
                    license = context.getString(R.string.license_lib_1_license)
                )
                LibraryLicenseItem(
                    name = context.getString(R.string.license_lib_2_name),
                    description = context.getString(R.string.license_lib_2_desc),
                    license = context.getString(R.string.license_lib_2_license)
                )
                LibraryLicenseItem(
                    name = context.getString(R.string.license_lib_3_name),
                    description = context.getString(R.string.license_lib_3_desc),
                    license = context.getString(R.string.license_lib_3_license)
                )
            }
        }
        
                HorizontalDivider()
        
        // 앱 정보
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = context.getString(R.string.license_app_info_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LicenseInfoRowItem(
                    title = context.getString(R.string.license_info_app_name),
                    value = "ProteinApp"
                )
                LicenseInfoRowItem(
                    title = context.getString(R.string.license_info_version),
                    value = "1.0.0"
                )
                LicenseInfoRowItem(
                    title = context.getString(R.string.license_info_build),
                    value = "1"
                )
                LicenseInfoRowItem(
                    title = context.getString(R.string.license_info_platform),
                    value = "Android 8.0+"
                )
                LicenseInfoRowItem(
                    title = context.getString(R.string.license_info_last_updated),
                    value = context.getString(R.string.license_info_last_updated_value)
                )
            }
        }
    }
}

// Helper Composable Functions

@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
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

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GuideSection(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@Composable
private fun PerformanceGuideItem(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FAQItem(question: String, answer: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PrivacySection(title: String, content: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TermsSection(title: String, content: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LicenseItem(name: String, license: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = license,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// User Guide Components

@Composable
private fun GuideSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            content()
        }
    }
}

@Composable
private fun GuideStepItem(step: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
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

@Composable
private fun InteractionGuideItem(gesture: String, description: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = gesture,
                style = MaterialTheme.typography.bodyLarge,
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

@Composable
private fun TipItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFFFF9800) // Orange color
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
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

@Composable
private fun FeatureCardWithList(
    title: String,
    icon: ImageVector,
    color: Color,
    features: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 헤더
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
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
                        modifier = Modifier.size(20.dp),
                        tint = color
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // 기능 목록
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                features.forEach { feature ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = color
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryLicenseItem(name: String, description: String, license: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LicenseInfoRowItem(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
