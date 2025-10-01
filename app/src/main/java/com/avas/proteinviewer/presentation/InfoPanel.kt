package com.avas.proteinviewer.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.domain.model.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoPanel(
    structure: PDBStructure,
    selectedTab: InfoTab,
    onTabChange: (InfoTab) -> Unit,
    onClose: () -> Unit,
    proteinInfo: ProteinInfo? = null // API에서 받은 추가 정보
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 고정 높이 제거 - 내용만큼만 차지하도록
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Protein Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
            Divider()
        
        // Tabs
        ScrollableTabRow(
            selectedTabIndex = InfoTab.values().indexOf(selectedTab),
            modifier = Modifier.fillMaxWidth()
        ) {
            InfoTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabChange(tab) },
                    text = { Text(tab.name.replace("_", " ").lowercase().capitalize()) }
                )
            }
        }
        
        // Content - LazyColumn 대신 일반 Column으로 변경 (스크롤은 부모에서 처리)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                InfoTab.OVERVIEW -> {
                    // iOS 스타일: Structure Information
                    InfoCard(
                        title = "Structure Information",
                        content = {
                            InfoRow("PDB ID", proteinInfo?.id ?: "N/A")
                            InfoRow("Total Atoms", "${structure.atomCount}")
                            InfoRow("Total Bonds", "${structure.bonds.size}")
                            InfoRow("Chains", "${structure.chainCount}")
                            val uniqueResidues = structure.atoms.map { it.residueName }.toSet()
                            InfoRow("Residue Types", "${uniqueResidues.size}")
                        }
                    )
                    
                    // iOS 스타일: Chemical Composition
                    InfoCard(
                        title = "Chemical Composition",
                        content = {
                            val uniqueResidues = structure.atoms.map { it.residueName }.toSet()
                            InfoRow("Residue Types", "${uniqueResidues.size}", "Number of different amino acid types present")
                            InfoRow("Total Residues", "${structure.residueCount}", "Total number of amino acid residues across all chains")
                            val chainList = structure.chains.sorted().joinToString(", ")
                            InfoRow("Chain IDs", chainList, "Identifiers for all polypeptide chains")
                            val hasLigands = structure.atoms.any { it.isLigand }
                            InfoRow("Ligands", if (hasLigands) "Present" else "None", 
                                if (hasLigands) "Small molecules or ions bound to the protein" else "No small molecules detected")
                        }
                    )
                    
                    // iOS 스타일: Experimental Details (API 데이터 사용)
                    InfoCard(
                        title = "Experimental Details",
                        content = {
                            proteinInfo?.resolution?.let {
                                InfoRow("Resolution", "%.2f Å".format(Locale.US, it), "Quality measure of structure determination")
                            }
                            proteinInfo?.experimentalMethod?.let {
                                InfoRow("Method", it, "Experimental technique used")
                            }
                            proteinInfo?.depositionDate?.let {
                                InfoRow("Deposition Date", it, "Date structure was deposited to PDB")
                            }
                            proteinInfo?.organism?.let {
                                InfoRow("Organism", it, "Source organism of the protein")
                            }
                            proteinInfo?.classification?.let {
                                InfoRow("Classification", it, "Functional classification")
                            }
                        }
                    )
                    
                    // 추가 Annotations
                    structure.annotations.forEach { annotation ->
                        InfoCard(
                            title = annotation.type.displayName,
                            content = {
                                Text(
                                    text = annotation.value,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (annotation.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = annotation.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }
                
                InfoTab.CHAINS -> {
                    structure.chains.sorted().forEach { chain ->
                        val chainAtoms = structure.atoms.filter { it.chain == chain }
                        val residueCount = chainAtoms.map { it.residueNumber }.toSet().size
                        val uniqueResidueTypes = chainAtoms.map { it.residueName }.toSet().size
                        
                        InfoCard(
                            title = "Chain $chain",
                            content = {
                                // iOS 스타일: Chain Overview
                                Text("Chain Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                        Text("Length", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$residueCount residues", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                        Text("Atoms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${chainAtoms.size}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                        Text("Residue Types", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$uniqueResidueTypes", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Secondary Structure Composition
                                Text("Secondary Structure", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val helixCount = chainAtoms.count { it.secondaryStructure == SecondaryStructure.HELIX }
                                val sheetCount = chainAtoms.count { it.secondaryStructure == SecondaryStructure.SHEET }
                                val coilCount = chainAtoms.count { it.secondaryStructure == SecondaryStructure.COIL }
                                
                                InfoRow("α-Helix", "$helixCount atoms (${(helixCount * 100 / chainAtoms.size)}%)")
                                InfoRow("β-Sheet", "$sheetCount atoms (${(sheetCount * 100 / chainAtoms.size)}%)")
                                InfoRow("Coil", "$coilCount atoms (${(coilCount * 100 / chainAtoms.size)}%)")
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Sequence Preview (first 50 residues)
                                Text("Sequence Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                val caAtoms = chainAtoms.filter { it.name == "CA" }.sortedBy { it.residueNumber }
                                val sequence = caAtoms.take(50).joinToString("") { residueToSingleLetter(it.residueName) }
                                Text(
                                    text = sequence + if (caAtoms.size > 50) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                            }
                        )
                    }
                }
                
                InfoTab.RESIDUES -> {
                    val residueCounts = structure.atoms
                        .groupBy { it.residueName }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                    
                    residueCounts.forEach { (residue, count) ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = residue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$count atoms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                InfoTab.LIGANDS -> {
                    val ligands = structure.atoms.filter { it.isLigand }
                    if (ligands.isEmpty()) {
                        Text(
                            text = "No ligands found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ligands.groupBy { it.residueName }.keys.toList().forEach { ligandName ->
                            val ligandAtoms = ligands.filter { it.residueName == ligandName }
                            InfoCard(
                                title = ligandName,
                                content = {
                                    InfoRow("Atoms", "${ligandAtoms.size}")
                                    InfoRow("Chains", "${ligandAtoms.map { it.chain }.toSet().size}")
                                }
                            )
                        }
                    }
                }
                
                InfoTab.POCKETS -> {
                    Text(
                        text = "Pocket analysis not yet implemented",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                InfoTab.SEQUENCE -> {
                    val residues = structure.atoms
                        .filter { it.isBackbone && it.name == "CA" }
                        .sortedBy { it.residueNumber }
                    
                    structure.chains.toList().forEach { chain ->
                        val chainResidues = residues.filter { it.chain == chain }
                        InfoCard(
                            title = "Chain $chain Sequence",
                            content = {
                                Text(
                                    text = chainResidues.joinToString("") { 
                                        residueToSingleLetter(it.residueName) 
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        )
                    }
                }
                
                InfoTab.ANNOTATIONS -> {
                    structure.annotations.forEach { annotation ->
                        InfoCard(
                            title = annotation.type.displayName,
                            content = {
                                Text(
                                    text = annotation.value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (annotation.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = annotation.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
private fun InfoRow(label: String, value: String, description: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
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

private fun residueToSingleLetter(residueName: String): String {
    return when (residueName.uppercase()) {
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

