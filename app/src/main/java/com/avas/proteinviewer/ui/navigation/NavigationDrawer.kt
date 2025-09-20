package com.avas.proteinviewer.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProteinViewerNavigationDrawer(
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 닫기 버튼
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Divider()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Menu Items
        val menuItems = listOf(
            MenuItem(
                title = "Search Proteins",
                icon = Icons.Default.Search,
                onClick = onNavigateToSearch
            ),
            MenuItem(
                title = "Protein Library",
                icon = Icons.Default.LibraryBooks,
                onClick = onNavigateToLibrary
            ),
            MenuItem(
                title = "Settings",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings
            ),
            MenuItem(
                title = "About",
                icon = Icons.Default.Info,
                onClick = onNavigateToAbout
            )
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(menuItems) { item ->
                NavigationDrawerItem(
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(item.title) },
                    selected = false,
                    onClick = {
                        item.onClick()
                        onDismiss()
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class MenuItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)
