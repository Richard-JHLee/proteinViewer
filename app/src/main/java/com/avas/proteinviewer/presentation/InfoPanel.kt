package com.avas.proteinviewer.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.domain.model.PDBStructure
import com.avas.proteinviewer.domain.model.ProteinInfo
import com.avas.proteinviewer.domain.model.AnnotationType
import com.avas.proteinviewer.domain.model.SecondaryStructure
import com.avas.proteinviewer.presentation.ProteinViewModel
import com.avas.proteinviewer.presentation.ProteinUiState


@Composable
fun InfoPanel(
    selectedTab: InfoTab,
    structure: PDBStructure,
    proteinInfo: ProteinInfo?,
    viewModel: ProteinViewModel,
    uiState: ProteinUiState,
    onStartUpdating: () -> Unit = {},
    onStopUpdating: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (selectedTab) {
            InfoTab.OVERVIEW -> {
                // Basic Statistics Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Atoms",
                        value = "${structure.atomCount}",
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Chains",
                        value = "${structure.chainCount}",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Residues",
                        value = "${structure.residueCount}",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Structure Information
                InfoCard(
                    title = "Structure Information",
                    backgroundColor = Color(0xFF2196F3).copy(alpha = 0.1f),
                    borderColor = Color(0xFF2196F3).copy(alpha = 0.3f),
                    content = {
                        InfoRow("PDB ID", proteinInfo?.id ?: "N/A", "Protein Data Bank identifier")
                        InfoRow("Total Atoms", "${structure.atomCount}", "atom")
                        InfoRow("Total Bonds", "${structure.bonds.size}", "link")
                        InfoRow("Chains", "${structure.chainCount}", "link.horizontal")
                        val uniqueElements = structure.atoms.map { it.element }.toSet()
                        InfoRow("Elements", "${uniqueElements.size}", "Number of different chemical elements present")
                        InfoRow("Element Types", uniqueElements.sorted().joinToString(", "), "Chemical elements found in this structure")
                    }
                )
                
                // Chemical Composition
                InfoCard(
                    title = "Chemical Composition",
                    backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    borderColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                    content = {
                        val uniqueResidues = structure.atoms.map { it.residueName }.toSet()
                        InfoRow("Residue Types", "${uniqueResidues.size}", "Number of different amino acid types")
                        InfoRow("Residue List", uniqueResidues.sorted().joinToString(", "), "Amino acid types found in this structure")
                        val hasLigands = structure.atoms.any { it.isLigand }
                        InfoRow("Ligands", if (hasLigands) "Present" else "None", 
                            if (hasLigands) "Small molecules or ions bound to the protein" else "No small molecules detected in this structure")
                    }
                )
                
                // Experimental Details
                InfoCard(
                    title = "Experimental Details",
                    backgroundColor = Color(0xFFFF9800).copy(alpha = 0.1f),
                    borderColor = Color(0xFFFF9800).copy(alpha = 0.3f),
                    content = {
                        val structureType = proteinInfo?.classification ?: 
                            structure.annotations.find { it.type == AnnotationType.FUNCTION }?.value ?: "Protein"
                        InfoRow("Structure Type", structureType, "Classification of this structure")
                        val dataSource = if (proteinInfo?.id?.startsWith("1") == true || proteinInfo?.id?.startsWith("2") == true) {
                            "PDB (Protein Data Bank)"
                        } else {
                            "PDB (Protein Data Bank)"
                        }
                        InfoRow("Data Source", dataSource, "Worldwide repository of 3D structure data")
                        val resolution = proteinInfo?.resolution?.toString() ?: 
                            structure.annotations.find { it.type == AnnotationType.RESOLUTION }?.value ?: "N/A"
                        InfoRow("Resolution", resolution, "Experimental resolution in Angstroms")
                        val experimentalMethod = proteinInfo?.experimentalMethod ?: 
                            structure.annotations.find { it.type == AnnotationType.EXPERIMENTAL_METHOD }?.value ?: "Unknown"
                        InfoRow("Method", experimentalMethod, "Experimental method used to determine structure")
                    }
                )
            }
            
            InfoTab.CHAINS -> {
                val chains = structure.chains
                
                if (chains.isEmpty()) {
                    InfoCard(
                        title = "No Chains Found",
                        content = {
                            Text(
                                "This structure does not contain any protein chains.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                } else {
                    chains.forEach { chain ->
                        val chainAtoms = structure.atoms.filter { it.chain == chain }
                        val uniqueResidues = chainAtoms.map { it.residueName }.toSet()
                        
                        InfoCard(
                            title = "Chain $chain",
                            backgroundColor = Color(0xFF2196F3).copy(alpha = 0.1f),
                            borderColor = Color(0xFF2196F3).copy(alpha = 0.3f),
                            content = {
                                InfoRow("Atoms", "${chainAtoms.size}", "Total number of atoms in this chain")
                                InfoRow("Residues", "${uniqueResidues.size}", "Number of different residue types")
                                
                                val caAtoms = chainAtoms.filter { it.name == "CA" }.sortedBy { it.residueNumber }
                                val sequence = caAtoms.joinToString("") { residueToSingleLetter(it.residueName) }
                                
                                Text(
                                    "Length: ${caAtoms.size} residues",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                // Secondary Structure Information (실제 데이터 사용)
                                val helixAtoms = chainAtoms.count { it.secondaryStructure == SecondaryStructure.HELIX }
                                val sheetAtoms = chainAtoms.count { it.secondaryStructure == SecondaryStructure.SHEET }
                                val coilAtoms = chainAtoms.count { it.secondaryStructure == SecondaryStructure.COIL }
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "α-helix: $helixAtoms atoms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFF44336)
                                    )
                                    Text(
                                        "β-sheet: $sheetAtoms atoms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB8860B)
                                    )
                                    Text(
                                        "Coil: $coilAtoms atoms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                
                                // Highlight and Focus Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val isHighlighted = uiState.highlightedChains.contains("chain:$chain")
                                    val isFocused = uiState.focusedElement == "chain:$chain"
                                    
                                    Button(
                                        onClick = { 
                                            onStartUpdating()
                                            viewModel.toggleChainHighlight(chain)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isHighlighted) Color(0xFF2196F3) else Color(0xFF2196F3).copy(alpha = 0.1f),
                                            contentColor = if (isHighlighted) Color.White else Color(0xFF2196F3)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            if (isHighlighted) Icons.Default.CheckCircle else Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isHighlighted) "Unhighlight" else "Highlight")
                                    }
                                    
                                    Button(
                                        onClick = { 
                                            onStartUpdating()
                                            viewModel.toggleChainFocus(chain)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFocused) Color(0xFF4CAF50) else Color(0xFF4CAF50).copy(alpha = 0.1f),
                                            contentColor = if (isFocused) Color.White else Color(0xFF4CAF50)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            if (isFocused) Icons.Default.MyLocation else Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isFocused) "Unfocus" else "Focus")
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            InfoTab.LIGANDS -> {
                val ligands = structure.atoms.filter { it.isLigand }
                    .groupBy { it.residueName }
                    .mapValues { (_, atoms) -> atoms.first() }
                
                if (ligands.isEmpty()) {
                    InfoCard(
                        title = "No Ligands Found",
                        content = {
                            Text(
                                "This structure does not contain any ligands (small molecules or ions).",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                } else {
                    ligands.forEach { (ligandName, ligandAtom) ->
                        val ligandAtoms = structure.atoms.filter { it.residueName == ligandName }
                        val uniqueChains = ligandAtoms.map { it.chain }.toSet()
                        
                        InfoCard(
                            title = "Ligand: $ligandName",
                            backgroundColor = Color(0xFFFF9800).copy(alpha = 0.1f),
                            borderColor = Color(0xFFFF9800).copy(alpha = 0.3f),
                            content = {
                                InfoRow("Atoms", "${ligandAtoms.size}", "Total number of atoms in this ligand")
                                InfoRow("Chains", uniqueChains.sorted().joinToString(", "), "Chains that contain this ligand")
                                
                                val uniqueElements = ligandAtoms.map { it.element }.toSet()
                                InfoRow("Elements", uniqueElements.sorted().joinToString(", "), "Chemical elements in this ligand")
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val isHighlighted = uiState.highlightedChains.contains("ligand:$ligandName")
                                    val isFocused = uiState.focusedElement == "ligand:$ligandName"
                                    
                                    Button(
                                        onClick = { 
                                            onStartUpdating()
                                            viewModel.toggleLigandHighlight(ligandName)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isHighlighted) Color(0xFFFF9800) else Color(0xFFFF9800).copy(alpha = 0.1f),
                                            contentColor = if (isHighlighted) Color.White else Color(0xFFFF9800)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            if (isHighlighted) Icons.Default.CheckCircle else Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isHighlighted) "Unhighlight" else "Highlight")
                                    }
                                    
                                    Button(
                                        onClick = { 
                                            onStartUpdating()
                                            viewModel.toggleLigandFocus(ligandName)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFocused) Color(0xFF4CAF50) else Color(0xFF4CAF50).copy(alpha = 0.1f),
                                            contentColor = if (isFocused) Color.White else Color(0xFF4CAF50)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            if (isFocused) Icons.Default.MyLocation else Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isFocused) "Unfocus" else "Focus")
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            InfoTab.POCKETS -> {
                val pockets = structure.atoms.filter { it.isPocket }
                    .groupBy { it.residueName }
                    .mapValues { (_, atoms) -> atoms.first() }
                
                if (pockets.isEmpty()) {
                    InfoCard(
                        title = "No Pockets Found",
                        content = {
                            Text(
                                "This structure does not contain any binding pockets.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                } else {
                    InfoCard(
                        title = "Binding Pockets Summary",
                        backgroundColor = Color(0xFF9C27B0).copy(alpha = 0.1f),
                        borderColor = Color(0xFF9C27B0).copy(alpha = 0.3f),
                        content = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Total Atoms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${pockets.size}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }
                    )
                    
                    pockets.forEach { (pocketName, pocketAtom) ->
                        val pocketAtoms = structure.atoms.filter { it.residueName == pocketName }
                        val uniqueChains = pocketAtoms.map { it.chain }.toSet()
                        
                        InfoCard(
                            title = "Pocket: $pocketName",
                            backgroundColor = Color(0xFF9C27B0).copy(alpha = 0.1f),
                            borderColor = Color(0xFF9C27B0).copy(alpha = 0.3f),
                            content = {
                                InfoRow("Atoms", "${pocketAtoms.size}", "Total number of atoms in this pocket")
                                InfoRow("Chains", uniqueChains.sorted().joinToString(", "), "Chains that contain this pocket")
                                
                                val uniqueElements = pocketAtoms.map { it.element }.toSet()
                                InfoRow("Elements", uniqueElements.sorted().joinToString(", "), "Chemical elements in this pocket")
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val isHighlighted = uiState.highlightedChains.contains("pocket:$pocketName")
                                    val isFocused = uiState.focusedElement == "pocket:$pocketName"
                                    
                                    Button(
                                        onClick = { 
                                            onStartUpdating()
                                            viewModel.togglePocketHighlight(pocketName)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isHighlighted) Color(0xFF9C27B0) else Color(0xFF9C27B0).copy(alpha = 0.1f),
                                            contentColor = if (isHighlighted) Color.White else Color(0xFF9C27B0)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            if (isHighlighted) Icons.Default.CheckCircle else Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isHighlighted) "Unhighlight" else "Highlight")
                                    }
                                    
                                    Button(
                                        onClick = { 
                                            onStartUpdating()
                                            viewModel.togglePocketFocus(pocketName)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFocused) Color(0xFF4CAF50) else Color(0xFF4CAF50).copy(alpha = 0.1f),
                                            contentColor = if (isFocused) Color.White else Color(0xFF4CAF50)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            if (isFocused) Icons.Default.MyLocation else Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isFocused) "Unfocus" else "Focus")
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            InfoTab.ANNOTATIONS -> {
                if (structure.annotations.isEmpty()) {
                    InfoCard(
                        title = "No Annotations Found",
                        content = {
                            Text(
                                "This structure does not contain any annotations.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                } else {
                    structure.annotations.forEach { annotation ->
                        InfoCard(
                            title = annotation.type.displayName,
                            content = {
                                Text(
                                    text = annotation.value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }
                }
            }
            else -> {
                InfoCard(
                    title = "Not Implemented",
                    content = {
                        Text(
                            "This tab is not yet implemented.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    description: String? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun getResidueColor(residueName: String): Color {
    return when (residueName) {
        "ALA", "VAL", "LEU", "ILE", "MET", "PHE", "TRP", "PRO" -> Color(0xFF2196F3)
        "SER", "THR", "ASN", "GLN", "TYR", "CYS" -> Color(0xFF4CAF50)
        "LYS", "ARG", "HIS" -> Color(0xFFF44336)
        "ASP", "GLU" -> Color(0xFFFF9800)
        "GLY" -> Color(0xFF9E9E9E)
        else -> Color(0xFF9C27B0)
    }
}

private fun residueToSingleLetter(residueName: String): String {
    return when (residueName) {
        "ALA" -> "A"
        "ARG" -> "R"
        "ASN" -> "N"
        "ASP" -> "D"
        "CYS" -> "C"
        "GLN" -> "Q"
        "GLU" -> "E"
        "GLY" -> "G"
        "HIS" -> "H"
        "ILE" -> "I"
        "LEU" -> "L"
        "LYS" -> "K"
        "MET" -> "M"
        "PHE" -> "F"
        "PRO" -> "P"
        "SER" -> "S"
        "THR" -> "T"
        "TRP" -> "W"
        "TYR" -> "Y"
        "VAL" -> "V"
        else -> "X"
    }
}