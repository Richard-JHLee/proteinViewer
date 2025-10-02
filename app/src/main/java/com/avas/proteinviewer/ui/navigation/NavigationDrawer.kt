package com.avas.proteinviewer.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import com.avas.proteinviewer.util.LanguageHelper
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.avas.proteinviewer.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProteinViewerNavigationDrawer(
    modifier: Modifier = Modifier,
    onItemSelected: (DrawerItemType) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(max = 320.dp),
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            DrawerHeader(onDismiss = onDismiss)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(DrawerItemType.values()) { item ->
                    DrawerMenuRow(item = item) {
                        onItemSelected(item)
                    }
                }
            }

            DrawerFooter()
        }
    }
}

@Composable
private fun DrawerHeader(onDismiss: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "ProteinViewerApp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "3D Protein Viewer",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close menu")
            }
        }
        Divider()
    }
}

@Composable
private fun DrawerMenuRow(item: DrawerItemType, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DrawerFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Divider()
        Text(
            text = "© 2025 AVAS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "All rights reserved",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class DrawerItemType(val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Library(Icons.Default.LibraryBooks),
    About(Icons.Default.Info),
    UserGuide(Icons.Default.MenuBook),
    Features(Icons.Default.Star),
    Settings(Icons.Default.Settings),
    Help(Icons.Default.HelpOutline),
    Privacy(Icons.Default.PrivacyTip),
    Terms(Icons.Default.Description),
    License(Icons.Default.Article);
    
    val title: String
        get() = when (this) {
            Library -> LanguageHelper.localizedText("단백질 라이브러리", "Protein Library")
            About -> LanguageHelper.localizedText("정보", "About")
            UserGuide -> LanguageHelper.localizedText("사용자 가이드", "User Guide")
            Features -> LanguageHelper.localizedText("기능", "Features")
            Settings -> LanguageHelper.localizedText("설정", "Settings")
            Help -> LanguageHelper.localizedText("도움말", "Help")
            Privacy -> LanguageHelper.localizedText("개인정보 처리방침", "Privacy Policy")
            Terms -> LanguageHelper.localizedText("이용약관", "Terms of Service")
            License -> LanguageHelper.localizedText("라이선스", "License")
        }
    
    val subtitle: String
        get() = when (this) {
            Library -> LanguageHelper.localizedText("단백질 데이터베이스 검색", "Search protein database")
            About -> LanguageHelper.localizedText("앱 정보 및 버전", "App information and version")
            UserGuide -> LanguageHelper.localizedText("사용자 가이드", "Step-by-step guidance")
            Features -> LanguageHelper.localizedText("주요 기능", "Key capabilities of the app")
            Settings -> LanguageHelper.localizedText("앱 설정", "Configure preferences")
            Help -> LanguageHelper.localizedText("도움말 및 FAQ", "FAQ and troubleshooting")
            Privacy -> LanguageHelper.localizedText("개인정보 처리방침", "How we handle your data")
            Terms -> LanguageHelper.localizedText("이용약관", "Usage agreement")
            License -> LanguageHelper.localizedText("라이선스 정보", "Open-source licenses")
        }
}
