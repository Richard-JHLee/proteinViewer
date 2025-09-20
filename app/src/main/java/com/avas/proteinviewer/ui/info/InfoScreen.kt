package com.avas.proteinviewer.ui.info

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
fun InfoScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProteinViewModel = hiltViewModel()
) {
    val currentProteinId by viewModel.currentProteinId.collectAsState()
    val currentProteinName by viewModel.currentProteinName.collectAsState()
    val structure by viewModel.structure.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protein Information") },
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
            // Current protein info
            if (currentProteinId.isNotEmpty()) {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Current Protein",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ID: $currentProteinId",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Name: $currentProteinName",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            structure?.let { proteinStructure ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Structure Statistics",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Atoms: ${proteinStructure.atomCount}")
                                Text("Residues: ${proteinStructure.residueCount}")
                                Text("Chains: ${proteinStructure.chainCount}")
                                
                                // Annotations
                                if (proteinStructure.annotations.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Annotations",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    proteinStructure.annotations.take(3).forEach { annotation ->
                                        Text(
                                            text = "${annotation.type.displayName}: ${annotation.value}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // App information
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "About Protein Viewer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "An educational Android app for visualizing protein structures in 3D.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Version: 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Features
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Features",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val features = listOf(
                            "3D Protein Visualization" to "Interactive 3D rendering with OpenGL ES",
                            "Multiple Render Modes" to "Spheres, Sticks, and Cartoon representations",
                            "Color Schemes" to "Element, Chain, Uniform, and Secondary Structure coloring",
                            "PDB Integration" to "Direct download from RCSB PDB database",
                            "Educational Content" to "Learn about protein structure and function",
                            "Offline Viewing" to "View downloaded structures without internet"
                        )
                        
                        features.forEach { (title, description) ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Usage instructions
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "How to Use",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val instructions = listOf(
                            "1. Load a protein using the default button or search for a specific PDB ID",
                            "2. Use touch gestures to rotate and zoom the 3D structure",
                            "3. Change render modes using the control panel",
                            "4. Switch color schemes to highlight different aspects",
                            "5. Toggle bonds on/off to focus on specific elements"
                        )
                        
                        instructions.forEach { instruction ->
                            Text(
                                text = instruction,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            // Data sources
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Data Sources",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Protein structure data is provided by the RCSB Protein Data Bank (PDB)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Website: https://www.rcsb.org/",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
