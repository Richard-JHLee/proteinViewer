package com.avas.proteinviewer.ui.info

import androidx.compose.foundation.layout.Arrangement
import com.avas.proteinviewer.util.LanguageHelper
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

@Composable
private fun getFaqItems() = listOf(
    FAQItem(
        question = LanguageHelper.localizedText("앱이 느리게 실행됩니다", "The app runs slowly"),
        answer = LanguageHelper.localizedText(
            "대용량 단백질 구조의 경우 로딩 시간이 오래 걸릴 수 있습니다. 작은 단백질부터 시작해보세요.",
            "Large protein structures may take longer to load. Try starting with smaller proteins."
        )
    ),
    FAQItem(
        question = LanguageHelper.localizedText("3D 모델이 회전하지 않습니다", "3D model doesn't rotate"),
        answer = LanguageHelper.localizedText(
            "뷰어 모드에서 드래그 제스처를 사용하여 모델을 회전시킬 수 있습니다.",
            "Use drag gestures in viewer mode to rotate the model."
        )
    ),
    FAQItem(
        question = LanguageHelper.localizedText("색상이 변경되지 않습니다", "Colors don't change"),
        answer = LanguageHelper.localizedText(
            "Color Schemes에서 원하는 색상 모드를 선택한 후 잠시 기다려주세요.",
            "Select your desired color mode from Color Schemes and wait a moment."
        )
    ),
    FAQItem(
        question = LanguageHelper.localizedText("단백질을 로드할 수 없습니다", "Cannot load protein"),
        answer = LanguageHelper.localizedText(
            "인터넷 연결을 확인하고 유효한 PDB ID를 입력했는지 확인해주세요.",
            "Check your internet connection and ensure you've entered a valid PDB ID."
        )
    ),
    FAQItem(
        question = LanguageHelper.localizedText("추가 도움이 필요합니다", "Need additional help"),
        answer = LanguageHelper.localizedText(
            "문제가 지속되거나 다른 질문이 있으시면 앱 정보 페이지의 문의 정보를 통해 연락해주세요.",
            "If problems persist or you have other questions, please contact us through the app info page."
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit
) {
    val faqItems = getFaqItems()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageHelper.localizedText("도움말", "Help")) },
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
                        text = LanguageHelper.localizedText("도움말 및 FAQ", "Help & FAQ"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // FAQ 항목들
            items(faqItems.size) { index ->
                FAQCard(faqItems[index])
            }
        }
    }
}

@Composable
private fun FAQCard(item: FAQItem) {
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
                text = item.question,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = item.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class FAQItem(
    val question: String,
    val answer: String
)
