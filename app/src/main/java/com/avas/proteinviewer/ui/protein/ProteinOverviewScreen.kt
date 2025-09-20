package com.avas.proteinviewer.ui.protein

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.data.model.PDBStructure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProteinOverviewScreen(
    structure: PDBStructure,
    proteinId: String,
    proteinName: String,
    modifier: Modifier = Modifier
) {
    // iPhone과 동일한 하단 정보 시트 형태
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Statistics Row - iPhone과 동일한 상단 통계
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StatCard(
                        value = structure.atoms.size.toString(),
                        label = "Atoms",
                        color = Color(0xFF2196F3)
                    )
                }
                item {
                    StatCard(
                        value = structure.bonds.size.toString(),
                        label = "Bonds",
                        color = Color(0xFF4CAF50)
                    )
                }
                item {
                    StatCard(
                        value = structure.chains.size.toString(),
                        label = "Chains",
                        color = Color(0xFFFF9800)
                    )
                }
                item {
                    StatCard(
                        value = structure.residues.size.toString(),
                        label = "Residues",
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        }
        
        // Structure Information Section - iPhone과 동일한 스타일
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD) // .blue.opacity(0.1)
                ),
                border = BorderStroke(1.dp, Color(0xFFBBDEFB)) // .blue.opacity(0.3)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Structure Information",
                        style = MaterialTheme.typography.titleMedium, // .title3에 해당
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // PDB ID
                    InfoRow(
                        label = "PDB ID",
                        value = proteinId,
                        description = "Protein Data Bank identifier - unique code for this structure"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Total Atoms
                    InfoRow(
                        label = "Total Atoms",
                        value = structure.atoms.size.toString(),
                        description = "All atoms in the structure including protein and ligands"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Total Bonds
                    InfoRow(
                        label = "Total Bonds",
                        value = structure.bonds.size.toString(),
                        description = "Chemical bonds connecting atoms in the structure"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Chains
                    InfoRow(
                        label = "Chains",
                        value = structure.chains.size.toString(),
                        description = "Number of polypeptide chains in the protein"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Elements
                    val uniqueElements = structure.atoms.map { it.element }.distinct()
                    InfoRow(
                        label = "Elements",
                        value = uniqueElements.size.toString(),
                        description = "Number of different chemical elements present"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Element Types
                    InfoRow(
                        label = "Element Types",
                        value = uniqueElements.sorted().joinToString(", "),
                        description = "Chemical elements found in this structure"
                    )
                }
            }
        }
        
        // Chemical Composition Section - iPhone과 동일한 스타일
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E8) // .green.opacity(0.1)
                ),
                border = BorderStroke(1.dp, Color(0xFFC8E6C9)) // .green.opacity(0.3)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Chemical Composition",
                        style = MaterialTheme.typography.titleMedium, // .title3에 해당
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val uniqueResidues = structure.atoms.map { it.residueName }.distinct()
                    val totalResidues = structure.atoms.map { "${it.chain}_${it.residueNumber}" }.distinct().size
                    val chainList = structure.atoms.map { it.chain }.distinct().sorted()
                    val hasLigands = structure.atoms.any { it.isLigand }
                    
                    InfoRow(
                        label = "Residue Types",
                        value = uniqueResidues.size.toString(),
                        description = "Number of different amino acid types present"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    InfoRow(
                        label = "Total Residues",
                        value = totalResidues.toString(),
                        description = "Total number of amino acid residues across all chains"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    InfoRow(
                        label = "Chain IDs",
                        value = chainList.joinToString(", "),
                        description = "Identifiers for each polypeptide chain"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    InfoRow(
                        label = "Ligands",
                        value = if (hasLigands) "Present" else "None",
                        description = if (hasLigands) "Small molecules or ions bound to the protein" else "No small molecules detected in this structure"
                    )
                }
            }
        }
        
        // Experimental Details Section - iPhone과 동일한 스타일
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0) // .orange.opacity(0.1)
                ),
                border = BorderStroke(1.dp, Color(0xFFFFE0B2)) // .orange.opacity(0.3)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Experimental Details",
                        style = MaterialTheme.typography.titleMedium, // .title3에 해당
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InfoRow(
                        label = "Structure Type",
                        value = "Protein",
                        description = "This is a protein structure determined by experimental methods"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    InfoRow(
                        label = "Data Source",
                        value = "PDB",
                        description = "Protein Data Bank - worldwide repository of 3D structure data"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    InfoRow(
                        label = "Quality",
                        value = "Experimental",
                        description = "Structure determined through experimental techniques like X-ray crystallography"
                    )
                    
                    if (structure.atoms.isNotEmpty()) {
                        val firstAtom = structure.atoms.first()
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        InfoRow(
                            label = "First Residue",
                            value = firstAtom.residueName,
                            description = "Chain ${firstAtom.chain}"
                        )
                    }
                }
            }
        }
        
        // Secondary Structure Information
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Secondary Structure",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val helixCount = structure.atoms.count { it.secondaryStructure.name == "HELIX" }
                    val sheetCount = structure.atoms.count { it.secondaryStructure.name == "SHEET" }
                    val coilCount = structure.atoms.count { it.secondaryStructure.name == "COIL" }
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            StatCard(
                                value = helixCount.toString(),
                                label = "α-Helix",
                                color = Color(0xFFE91E63)
                            )
                        }
                        item {
                            StatCard(
                                value = sheetCount.toString(),
                                label = "β-Sheet",
                                color = Color(0xFF9C27B0)
                            )
                        }
                        item {
                            StatCard(
                                value = coilCount.toString(),
                                label = "Coil",
                                color = Color(0xFF607D8B)
                            )
                        }
                    }
                }
            }
        }
        
        // Chain Information
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Chain Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    structure.chains.forEachIndexed { index, chain ->
                        ChainInfoCard(chain = chain, structure = structure)
                        if (index < structure.chains.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    // iPhone과 동일한 StatCard - Card 없이 VStack만 사용
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium, // .title2에 해당
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall, // .caption에 해당
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    description: String,
    modifier: Modifier = Modifier
) {
    // iPhone과 동일한 InfoRow 스타일
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge, // .callout에 해당 (16pt)
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge, // .callout에 해당 (16pt)
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall, // .footnote에 해당 (13pt)
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChainInfoCard(
    chain: String,
    structure: PDBStructure,
    modifier: Modifier = Modifier
) {
    val chainAtoms = structure.atoms.filter { it.chain == chain }
    val chainResidues = chainAtoms.map { it.residueNumber }.distinct().sorted()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chain $chain",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${chainAtoms.size} atoms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Residues: ${chainResidues.first()} - ${chainResidues.last()} (${chainResidues.size} total)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
