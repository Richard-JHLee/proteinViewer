package com.avas.proteinviewer.ui.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.util.LanguageHelper

private val licenseText = """
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
""".trimIndent()

@Composable
private fun getThirdPartyLibraries() = listOf(
    LibraryItem(
        name = "Jetpack Compose",
        description = LanguageHelper.localizedText(
            "안드로이드 네이티브 UI 구축을 위한 최신 툴킷",
            "Modern toolkit for building native Android UI"
        ),
        license = "Apache 2.0"
    ),
    LibraryItem(
        name = "OpenGL ES",
        description = LanguageHelper.localizedText(
            "3D 렌더링을 위한 크로스 플랫폼 그래픽 API",
            "Cross-platform graphics API for 3D rendering"
        ),
        license = "MIT"
    ),
    LibraryItem(
        name = "Hilt",
        description = LanguageHelper.localizedText(
            "안드로이드용 의존성 주입 라이브러리",
            "Dependency injection library for Android"
        ),
        license = "Apache 2.0"
    ),
    LibraryItem(
        name = "Kotlin",
        description = LanguageHelper.localizedText(
            "안드로이드 개발을 위한 프로그래밍 언어",
            "Programming language for Android development"
        ),
        license = "Apache 2.0"
    ),
    LibraryItem(
        name = "Material 3",
        description = LanguageHelper.localizedText(
            "Compose용 Material Design 컴포넌트",
            "Material Design components for Compose"
        ),
        license = "Apache 2.0"
    )
)

@Composable
private fun getAppInfo() = listOf(
    LicenseInfoRow(
        title = LanguageHelper.localizedText("앱 이름", "App Name"),
        value = "ProteinViewer",
        description = LanguageHelper.localizedText("애플리케이션 이름", "Application name")
    ),
    LicenseInfoRow(
        title = LanguageHelper.localizedText("버전", "Version"),
        value = "1.0.0",
        description = LanguageHelper.localizedText("현재 앱 버전", "Current app version")
    ),
    LicenseInfoRow(
        title = LanguageHelper.localizedText("빌드", "Build"),
        value = "1",
        description = LanguageHelper.localizedText("빌드 번호", "Build number")
    ),
    LicenseInfoRow(
        title = LanguageHelper.localizedText("플랫폼", "Platform"),
        value = LanguageHelper.localizedText("Android 7.0+", "Android 7.0+"),
        description = LanguageHelper.localizedText("최소 지원 Android 버전", "Minimum supported Android version")
    ),
    LicenseInfoRow(
        title = LanguageHelper.localizedText("마지막 업데이트", "Last Updated"),
        value = LanguageHelper.localizedText("2025년 1월", "January 2025"),
        description = LanguageHelper.localizedText("마지막 업데이트 날짜", "Last update date")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(
    onNavigateBack: () -> Unit
) {
    val thirdPartyLibraries = getThirdPartyLibraries()
    val appInfo = getAppInfo()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageHelper.localizedText("라이선스", "License")) },
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
            // MIT 라이센스 헤더
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = LanguageHelper.localizedText("MIT 라이센스", "MIT License"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Copyright (c) 2025 AVAS",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Divider
            item {
                Divider()
            }

            // 라이센스 본문
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF2F2F7) // iOS systemGray6 equivalent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = licenseText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // 서드파티 라이브러리 섹션
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = LanguageHelper.localizedText("서드파티 라이브러리", "Third-party Libraries"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF2F2F7)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            thirdPartyLibraries.forEachIndexed { index, library ->
                                LibraryItemCard(library)
                                if (index < thirdPartyLibraries.size - 1) {
                                    Divider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 앱 정보 섹션
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = LanguageHelper.localizedText("앱 정보", "App Information"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF2F2F7)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            appInfo.forEach { info ->
                                LicenseInfoRowCard(info)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryItemCard(library: LibraryItem) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row {
            Text(
                text = library.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = library.license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = library.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LicenseInfoRowCard(info: LicenseInfoRow) {
    Row {
        Text(
            text = info.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = info.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private data class LibraryItem(
    val name: String,
    val description: String,
    val license: String
)

private data class LicenseInfoRow(
    val title: String,
    val value: String,
    val description: String
)