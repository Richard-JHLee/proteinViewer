package com.avas.proteinviewer.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.R

enum class MenuItem(
    val titleRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val descriptionRes: Int
) {
    PROTEIN_LIBRARY(com.avas.proteinviewer.R.string.library_title, Icons.AutoMirrored.Filled.LibraryBooks, com.avas.proteinviewer.R.string.desc_browse_protein_db),
    ABOUT(com.avas.proteinviewer.R.string.about, Icons.Default.Info, com.avas.proteinviewer.R.string.desc_app_info_version),
    USER_GUIDE(com.avas.proteinviewer.R.string.user_guide, Icons.Default.Book, com.avas.proteinviewer.R.string.desc_user_guide),
    FEATURES(com.avas.proteinviewer.R.string.features, Icons.Default.Star, com.avas.proteinviewer.R.string.desc_features),
    SETTINGS(com.avas.proteinviewer.R.string.settings, Icons.Default.Settings, com.avas.proteinviewer.R.string.desc_settings),
    HELP(com.avas.proteinviewer.R.string.help, Icons.AutoMirrored.Filled.Help, com.avas.proteinviewer.R.string.desc_help_faq),
    PRIVACY(com.avas.proteinviewer.R.string.privacy_title, Icons.Default.PrivacyTip, com.avas.proteinviewer.R.string.desc_privacy),
    TERMS(com.avas.proteinviewer.R.string.terms_title, Icons.Default.Description, com.avas.proteinviewer.R.string.desc_terms),
    LICENSE(com.avas.proteinviewer.R.string.license_title, Icons.Default.Policy, com.avas.proteinviewer.R.string.desc_license)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideMenuContent(
    onMenuItemClick: (MenuItem) -> Unit,
    onClose: () -> Unit = {},
    selectedMenuItem: MenuItem? = null
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            // Header with Close Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 앱 아이콘 컨테이너
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo_safe),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ProteinViewer",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(id = R.string.app_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    
                    // Close Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.close_menu),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Menu Items (PROTEIN_LIBRARY 제외)
            LazyColumn {
                items(MenuItem.values().filter { it != MenuItem.PROTEIN_LIBRARY }) { menuItem ->
                    MenuItem(
                        item = menuItem,
                        isSelected = selectedMenuItem == menuItem,
                        onClick = { onMenuItemClick(menuItem) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Copyright,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "2025 AVAS",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.all_rights_reserved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    item: MenuItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val iconColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val descriptionColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아이콘 컨테이너
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconColor
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 텍스트 컨텐츠
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(id = item.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(id = item.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = descriptionColor
                )
            }
            
            // 선택된 상태 표시 화살표
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

