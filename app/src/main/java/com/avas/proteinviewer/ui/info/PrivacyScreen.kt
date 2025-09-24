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
private fun getPrivacySections() = listOf(
    PrivacySection(
        title = LanguageHelper.localizedText("1. 수집하는 정보", "1. Information We Collect"),
        content = LanguageHelper.localizedText(
            "ProteinApp은 사용자의 개인정보를 수집하지 않습니다.\n\n• PDB ID: 단백질 구조 데이터를 다운로드하기 위한 공개 식별자로, 개인정보가 아닙니다.\n• 앱 사용 데이터: 앱 내에서 생성되는 모든 데이터는 기기에만 저장됩니다.\n• 네트워크 요청: 단백질 구조 데이터 다운로드를 위한 API 호출만 수행합니다.",
            "ProteinApp does not collect users' personal information.\n\n• PDB ID: Public identifiers for downloading protein structure data, which are not personal information.\n• App usage data: All data generated within the app is stored only on the device.\n• Network requests: Only API calls for downloading protein structure data are performed."
        )
    ),
    PrivacySection(
        title = LanguageHelper.localizedText("2. 정보 사용 목적", "2. Purpose of Information Use"),
        content = LanguageHelper.localizedText(
            "• 단백질 구조 시각화 및 3D 렌더링\n• 교육 및 연구 목적의 데이터 제공\n• 앱 기능 향상을 위한 로컬 데이터 처리\n• 사용자 경험 개선을 위한 앱 내 기능 제공",
            "• Protein structure visualization and 3D rendering\n• Providing data for educational and research purposes\n• Local data processing for app functionality improvement\n• In-app features for improving user experience"
        )
    ),
    PrivacySection(
        title = LanguageHelper.localizedText("3. 정보 보호 및 보안", "3. Information Protection and Security"),
        content = LanguageHelper.localizedText(
            "• 모든 데이터는 사용자 기기에만 저장됩니다.\n• 외부 서버로 개인정보가 전송되지 않습니다.\n• PDB API 호출 시에는 공개 데이터만 요청합니다.\n• 앱 삭제 시 모든 로컬 데이터가 함께 삭제됩니다.",
            "• All data is stored only on the user's device.\n• Personal information is not transmitted to external servers.\n• Only public data is requested when calling PDB APIs.\n• All local data is deleted when the app is uninstalled."
        )
    ),
    PrivacySection(
        title = LanguageHelper.localizedText("4. 외부 서비스", "4. External Services"),
        content = LanguageHelper.localizedText(
            "• RCSB PDB (data.rcsb.org): 단백질 구조 데이터 제공\n• PDBe (www.ebi.ac.uk): 추가 단백질 정보 제공\n• UniProt (rest.uniprot.org): 단백질 기능 정보 제공\n\n이들 서비스는 모두 공개 API이며 개인정보를 요구하지 않습니다.",
            "• RCSB PDB (data.rcsb.org): Protein structure data provider\n• PDBe (www.ebi.ac.uk): Additional protein information provider\n• UniProt (rest.uniprot.org): Protein function information provider\n\nAll of these services are public APIs and do not require personal information."
        )
    ),
    PrivacySection(
        title = LanguageHelper.localizedText("5. 사용자 권리", "5. User Rights"),
        content = LanguageHelper.localizedText(
            "• 데이터 삭제: 앱 삭제를 통해 모든 데이터를 삭제할 수 있습니다.\n• 데이터 수정: 앱 내에서 입력한 정보는 언제든 수정 가능합니다.\n• 문의: 개인정보 관련 문의는 앱 정보 페이지를 이용해주세요.",
            "• Data deletion: All data can be deleted by uninstalling the app.\n• Data modification: Information entered within the app can be modified at any time.\n• Inquiries: Please use the app information page for privacy-related inquiries."
        )
    ),
    PrivacySection(
        title = LanguageHelper.localizedText("6. 정책 변경", "6. Policy Changes"),
        content = LanguageHelper.localizedText(
            "개인정보 처리방침은 필요에 따라 변경될 수 있으며, 변경 시 앱 내에서 공지합니다. 마지막 업데이트: 2025년 1월 9일",
            "The privacy policy may be changed as necessary, and changes will be announced within the app. Last updated: January 9, 2025"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onNavigateBack: () -> Unit
) {
    val privacySections = getPrivacySections()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageHelper.localizedText("개인정보 처리방침", "Privacy Policy")) },
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
                        text = LanguageHelper.localizedText("개인정보 처리방침", "Privacy Policy"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 개인정보 처리방침 섹션들
            items(privacySections.size) { index ->
                PrivacySectionCard(privacySections[index])
            }
        }
    }
}

@Composable
private fun PrivacySectionCard(section: PrivacySection) {
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

private data class PrivacySection(
    val title: String,
    val content: String
)