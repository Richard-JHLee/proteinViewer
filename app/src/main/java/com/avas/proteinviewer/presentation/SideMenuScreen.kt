package com.avas.proteinviewer.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class MenuItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
) {
    PROTEIN_LIBRARY("Protein Library", Icons.Default.LibraryBooks, "Browse protein database"),
    ABOUT("About", Icons.Default.Info, "App information and version"),
    USER_GUIDE("User Guide", Icons.Default.Book, "User guide"),
    FEATURES("Features", Icons.Default.Star, "Key features"),
    SETTINGS("Settings", Icons.Default.Settings, "App settings"),
    HELP("Help", Icons.Default.Help, "Help and FAQ"),
    PRIVACY("Privacy Policy", Icons.Default.PrivacyTip, "Privacy Policy"),
    TERMS("Terms of Service", Icons.Default.Description, "Terms of Service"),
    LICENSE("License", Icons.Default.Policy, "License information")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideMenuContent(
    onMenuItemClick: (MenuItem) -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "ProteinViewerApp",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "3D Protein Viewer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Menu Items
            LazyColumn {
                items(MenuItem.values()) { menuItem ->
                    MenuItem(
                        item = menuItem,
                        onClick = { onMenuItemClick(menuItem) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "© 2025 AVAS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "All rights reserved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MenuItem(
    item: MenuItem,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(item.title) },
        supportingContent = { Text(item.description) },
        leadingContent = {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

