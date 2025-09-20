package com.avas.proteinviewer.ui.library

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.avas.proteinviewer.viewmodel.ProteinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProteinViewModel = hiltViewModel()
) {
    val proteinCategories = listOf(
        ProteinCategory(
            name = "Enzymes",
            icon = Icons.Default.Science,
            proteins = listOf(
                ProteinInfo("1LYZ", "Lysozyme", "Antibacterial enzyme"),
                ProteinInfo("1TIM", "Triosephosphate Isomerase", "Glycolytic enzyme"),
                ProteinInfo("1AKE", "Adenylate Kinase", "Energy metabolism enzyme")
            )
        ),
        ProteinCategory(
            name = "Structural Proteins",
            icon = Icons.Default.Apartment,
            proteins = listOf(
                ProteinInfo("1TUB", "Tubulin", "Cytoskeletal protein"),
                ProteinInfo("1PGA", "Protein G", "Immunoglobulin-binding protein")
            )
        ),
        ProteinCategory(
            name = "Transport Proteins",
            icon = Icons.Default.LocalShipping,
            proteins = listOf(
                ProteinInfo("1HHO", "Hemoglobin", "Oxygen transport protein"),
                ProteinInfo("1UBQ", "Ubiquitin", "Protein degradation marker")
            )
        ),
        ProteinCategory(
            name = "Hormones",
            icon = Icons.Default.Favorite,
            proteins = listOf(
                ProteinInfo("1INS", "Insulin", "Glucose regulation hormone")
            )
        ),
        ProteinCategory(
            name = "Fluorescent Proteins",
            icon = Icons.Default.Lightbulb,
            proteins = listOf(
                ProteinInfo("1GFL", "Green Fluorescent Protein", "Bioluminescent protein")
            )
        )
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protein Library") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Browse by Category",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            items(proteinCategories) { category ->
                ProteinCategoryCard(
                    category = category,
                    onProteinClick = { protein ->
                        viewModel.loadSelectedProtein(protein.pdbId)
                        onNavigateBack()
                    }
                )
            }
        }
    }
}

@Composable
private fun ProteinCategoryCard(
    category: ProteinCategory,
    onProteinClick: (ProteinInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Category header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            // Proteins list
            if (expanded) {
                category.proteins.forEach { protein ->
                    Divider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(start = 40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${protein.pdbId} - ${protein.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = protein.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onProteinClick(protein) }
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Load",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ProteinCategory(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val proteins: List<ProteinInfo>
)

data class ProteinInfo(
    val pdbId: String,
    val name: String,
    val description: String
)
