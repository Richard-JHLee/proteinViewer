package com.avas.proteinviewer.ui.protein

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.data.model.PDBStructure

@Composable
fun ProteinInfoPanel(
    structure: PDBStructure?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Bottom Navigation에서 선택된 탭에 따라 콘텐츠 표시
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            when (selectedTab) {
                0 -> { // Overview
                    structure?.let { proteinStructure ->
                        OverviewContent(structure = proteinStructure)
                    }
                }
                1 -> { // Chains
                    structure?.let { proteinStructure ->
                        ChainsContent(structure = proteinStructure)
                    }
                }
                2 -> { // Residues
                    structure?.let { proteinStructure ->
                        ResiduesContent(structure = proteinStructure)
                    }
                }
                3 -> { // Ligands
                    LigandsContent()
                }
                4 -> { // Pockets
                    structure?.let { proteinStructure ->
                        PocketsContent(structure = proteinStructure)
                    }
                }
                5 -> { // Sequence
                    structure?.let { proteinStructure ->
                        SequenceContent(structure = proteinStructure)
                    }
                }
                6 -> { // Annotations
                    structure?.let { proteinStructure ->
                        AnnotationsContent(structure = proteinStructure)
                    }
                }
            }
        }
    }
}


@Composable
private fun OverviewContent(structure: PDBStructure) {
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
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        StatCard(
                            value = structure.atoms.size.toString(),
                            label = "Atoms",
                            color = androidx.compose.ui.graphics.Color(0xFF2196F3)
                        )
                    }
                    item {
                        StatCard(
                            value = structure.bonds.size.toString(),
                            label = "Bonds",
                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        )
                    }
                    item {
                        StatCard(
                            value = structure.chains.size.toString(),
                            label = "Chains",
                            color = androidx.compose.ui.graphics.Color(0xFFFF9800)
                        )
                    }
                    item {
                        StatCard(
                            value = structure.residues.size.toString(),
                            label = "Residues",
                            color = androidx.compose.ui.graphics.Color(0xFF9C27B0)
                        )
                    }
                }
            }
        }

        // 기본 정보
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Protein Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InfoRow(label = "Total Atoms", value = structure.atoms.size.toString())
                InfoRow(label = "Total Bonds", value = structure.bonds.size.toString())
                InfoRow(label = "Chains", value = structure.chains.joinToString(", "))
                InfoRow(label = "Residues", value = structure.residues.size.toString())
            }
        }
    }
}

@Composable
private fun ChainsContent(structure: PDBStructure) {
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
            items(structure.chains) { chain ->
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
                            text = "${structure.residues.count { it.chain == chain }} residues",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResiduesContent(structure: PDBStructure) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Residues (First 20)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(structure.residues.take(20)) { residue ->
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
private fun PocketsContent(structure: PDBStructure) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Binding Pockets",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Pocket Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InfoRow(label = "Total Pockets", value = "3")
                InfoRow(label = "Largest Pocket", value = "Pocket 1 (45.2 Å³)")
                InfoRow(label = "Average Volume", value = "28.7 Å³")
                InfoRow(label = "Accessibility", value = "High")
            }
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("Pocket 1", "Pocket 2", "Pocket 3")) { pocket ->
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
                            text = pocket,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "45.2 Å³",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SequenceContent(structure: PDBStructure) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Amino Acid Sequence",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Primary Structure",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Sample sequence (first 50 residues)
                val sampleSequence = "MKLLILTCLVAVALARPKHPIKHQGLPQEVLNENLLRFFVAPFPEVFGKEKVNEL"
                
                Text(
                    text = "Chain A (First 50 residues):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = sampleSequence,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Total Length: ${structure.residues.size} residues",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Sequence Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InfoRow(label = "Total Residues", value = structure.residues.size.toString())
                InfoRow(label = "Unique Residues", value = "20")
                InfoRow(label = "Molecular Weight", value = "4,876 Da")
                InfoRow(label = "Isoelectric Point", value = "6.2")
            }
        }
    }
}

@Composable
private fun AnnotationsContent(structure: PDBStructure) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Protein Annotations",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Functional Annotations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InfoRow(label = "Protein Name", value = "Crambin")
                InfoRow(label = "Organism", value = "Crambe abyssinica")
                InfoRow(label = "Function", value = "Plant defense protein")
                InfoRow(label = "EC Number", value = "N/A")
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Structural Annotations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InfoRow(label = "Resolution", value = "0.54 Å")
                InfoRow(label = "R-Factor", value = "0.12")
                InfoRow(label = "Space Group", value = "P21")
                InfoRow(label = "Unit Cell", value = "a=40.8, b=18.5, c=22.5")
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Database References",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InfoRow(label = "PDB ID", value = "1CRN")
                InfoRow(label = "UniProt", value = "P01542")
                InfoRow(label = "PubMed", value = "12345678")
                InfoRow(label = "DOI", value = "10.1000/xyz123")
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

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
