package com.avas.proteinviewer.ui.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@Composable
private fun getTermsSections() = listOf(
    TermsSection(
        title = LanguageHelper.localizedText("서비스 이용", "Service Usage"),
        content = LanguageHelper.localizedText(
            "ProteinApp은 유료 앱입니다. 앱 다운로드 및 사용을 위해서는 App Store에서 결제가 필요합니다.",
            "ProteinApp is a paid app. Payment through the App Store is required to download and use the app."
        )
    ),
    TermsSection(
        title = LanguageHelper.localizedText("결제 및 구독", "Payment & Subscription"),
        content = LanguageHelper.localizedText(
            "• 앱 구매는 App Store를 통해 처리됩니다.\n• 일회성 결제로 앱의 모든 기능을 이용할 수 있습니다.\n• 앱 구매 후 추가 결제는 없습니다.\n• 환불은 App Store 정책에 따라 제한적입니다.",
            "• App purchases are processed through the App Store.\n• One-time payment provides access to all app features.\n• No additional payments after app purchase.\n• Refunds are limited according to App Store policy."
        )
    ),
    TermsSection(
        title = LanguageHelper.localizedText("서비스 범위", "Service Scope"),
        content = LanguageHelper.localizedText(
            "• 3D 단백질 구조 시각화\n• 고급 렌더링 옵션\n• 단백질 분석 도구\n• 오프라인 데이터 저장\n• 고객 지원 서비스\n• 모든 기능이 앱 구매 시 포함됩니다.",
            "• 3D protein structure visualization\n• Advanced rendering options\n• Protein analysis tools\n• Offline data storage\n• Customer support service\n• All features are included with app purchase."
        )
    ),
    TermsSection(
        title = LanguageHelper.localizedText("환불 정책", "Refund Policy"),
        content = LanguageHelper.localizedText(
            "• 앱 구매 환불은 Apple App Store 정책에 따라 처리됩니다.\n• 환불 요청은 App Store에서 직접 신청해주세요.\n• Apple의 환불 정책: https://support.apple.com/HT204084\n• 개발자는 환불 처리 권한이 없으며, Apple이 모든 환불을 관리합니다.",
            "• App purchase refunds are processed according to Apple App Store policy.\n• Please request refunds directly through the App Store.\n• Apple's refund policy: https://support.apple.com/HT204084\n• Developers have no refund processing authority; Apple manages all refunds."
        )
    ),
    TermsSection(
        title = LanguageHelper.localizedText("앱 업데이트 및 지원", "App Updates & Support"),
        content = LanguageHelper.localizedText(
            "• 앱 업데이트는 무료로 제공됩니다.\n• 새로운 기능 추가 시 별도 결제 없이 이용 가능합니다.\n• 앱 지원 중단 시 30일 전 사전 공지합니다.\n• 지원 중단으로 인한 데이터 손실에 대해 책임지지 않습니다.",
            "• App updates are provided free of charge.\n• New features can be used without additional payment.\n• 30-day advance notice will be given before app support discontinuation.\n• We are not responsible for data loss due to support discontinuation."
        )
    ),
    TermsSection(
        title = LanguageHelper.localizedText("책임 제한", "Liability Limitation"),
        content = LanguageHelper.localizedText(
            "앱 사용으로 인한 손해에 대해 개발자는 책임지지 않습니다. 유료 앱 이용 시에도 동일하게 적용됩니다.",
            "Developers are not responsible for damages caused by app usage. This applies equally to paid app usage."
        )
    ),
    TermsSection(
        title = LanguageHelper.localizedText("앱 변경", "App Changes"),
        content = LanguageHelper.localizedText(
            "앱의 기능은 사전 통지 없이 변경될 수 있습니다. 주요 기능 변경 시에는 앱 업데이트를 통해 공지합니다.",
            "App features may be changed without prior notice. Major feature changes will be announced through app updates."
        )
    ),
    TermsSection(
        title = LanguageHelper.localizedText("문의 및 지원", "Contact & Support"),
        content = LanguageHelper.localizedText(
            "서비스 이용 관련 문의는 앱 정보 페이지를 통해 연락해주세요. 마지막 업데이트: 2025년 1월 9일",
            "For service-related inquiries, please contact us through the app info page. Last updated: January 9, 2025"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    onNavigateBack: () -> Unit
) {
    val termsSections = getTermsSections()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageHelper.localizedText("이용약관", "Terms of Service")) },
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
            // 헤더
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = LanguageHelper.localizedText("이용약관", "Terms of Service"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 이용약관 섹션들
            items(termsSections.size) { index ->
                TermsSectionCard(termsSections[index])
            }
        }
    }
}

@Composable
private fun TermsSectionCard(section: TermsSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2F2F7) // iOS systemGray6 equivalent
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = section.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class TermsSection(
    val title: String,
    val content: String
)