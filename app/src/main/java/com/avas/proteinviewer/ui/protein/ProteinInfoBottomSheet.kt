package com.avas.proteinviewer.ui.protein

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.ui.protein.RendererBackend

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProteinInfoBottomSheet(
    structure: PDBStructure?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val bottomSheetState = rememberBottomSheetScaffoldState()
    
    var backend by remember { mutableStateOf(RendererBackend.OpenGL) }

    BottomSheetScaffold(
        scaffoldState = bottomSheetState,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .padding(16.dp)
            ) {
                // Backend toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Renderer:", style = MaterialTheme.typography.bodyMedium)
                    FilterChip(
                        selected = backend == RendererBackend.Filament,
                        onClick = { backend = RendererBackend.Filament },
                        label = { Text("Filament") }
                    )
                    FilterChip(
                        selected = backend == RendererBackend.OpenGL,
                        onClick = { backend = RendererBackend.OpenGL },
                        label = { Text("OpenGL") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            RoundedCornerShape(2.dp)
                        )
                        .align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Overview", "Chains", "Residues", "Ligands").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { onTabSelected(index) },
                            text = { Text(title) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                when (selectedTab) {
                    0 -> OverviewContent(structure = structure)
                    1 -> ChainsContent(structure = structure)
                    2 -> ResiduesContent(structure = structure)
                    3 -> LigandsContent()
                }
            }
        },
        sheetPeekHeight = 80.dp,
        content = {
            // 3D Viewer 영역
            ProteinViewerView(
                structure = structure,
                proteinId = "",
                modifier = Modifier.fillMaxSize(),
                backend = backend
            )
        }
    )
}

@Composable
private fun OverviewContent(structure: PDBStructure?) {
    structure?.let { proteinStructure ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 통계 정보 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Structure Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard(
                            value = proteinStructure.atoms.size.toString(),
                            label = "Atoms",
                            color = androidx.compose.ui.graphics.Color(0xFF2196F3)
                        )
                        StatCard(
                            value = proteinStructure.bonds.size.toString(),
                            label = "Bonds",
                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        )
                        StatCard(
                            value = proteinStructure.chains.size.toString(),
                            label = "Chains",
                            color = androidx.compose.ui.graphics.Color(0xFFFF9800)
                        )
                        StatCard(
                            value = proteinStructure.residues.size.toString(),
                            label = "Residues",
                            color = androidx.compose.ui.graphics.Color(0xFF9C27B0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainsContent(structure: PDBStructure?) {
    structure?.let { proteinStructure ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Protein Chains",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(proteinStructure.chains) { chain ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chain $chain",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${proteinStructure.residues.count { it.chain == chain }} residues",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResiduesContent(structure: PDBStructure?) {
    structure?.let { proteinStructure ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Residues (First 10)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(proteinStructure.residues.take(10)) { residue ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${residue.residueName} ${residue.residueNumber}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Chain ${residue.chain}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LigandsContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ligands",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.LocalPharmacy,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No ligands found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
