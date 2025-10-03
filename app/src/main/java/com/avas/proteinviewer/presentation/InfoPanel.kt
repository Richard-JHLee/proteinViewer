package com.avas.proteinviewer.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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

// 표준 아미노산 목록
private val standardResidues = setOf(
    "ALA", "ARG", "ASN", "ASP", "CYS", "GLN", "GLU", "GLY", "HIS", "ILE",
    "LEU", "LYS", "MET", "PHE", "PRO", "SER", "THR", "TRP", "TYR", "VAL"
)

// 아이폰 스타일: 잔기 색상 함수
private fun getResidueColorForComposition(residueName: String): Color {
    val hydrophobicResidues = listOf("ALA", "VAL", "ILE", "LEU", "MET", "PHE", "TRP", "PRO")
    val polarResidues = listOf("SER", "THR", "ASN", "GLN", "TYR", "CYS")
    val chargedResidues = listOf("LYS", "ARG", "HIS", "ASP", "GLU")
    
    return when {
        hydrophobicResidues.contains(residueName) -> Color(0xFFFF9500) // 주황색
        polarResidues.contains(residueName) -> Color(0xFF007AFF) // 파란색
        chargedResidues.contains(residueName) -> Color(0xFFFF3B30) // 빨간색
        else -> Color.Gray
    }
}

@Composable
fun InfoPanel(
    selectedTab: InfoTab,
    structure: PDBStructure,
    proteinInfo: ProteinInfo?,
    viewModel: ProteinViewModel,
    uiState: ProteinUiState,
    onStartUpdating: () -> Unit = {}
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
                
                // 아이폰 스타일: Structure Information (파란색 배경)
                InfoCard(
                    title = "Structure Information",
                    backgroundColor = Color(0xFF007AFF).copy(alpha = 0.1f),
                    borderColor = Color(0xFF007AFF).copy(alpha = 0.3f),
                    content = {
                        InfoRow("PDB ID", proteinInfo?.id ?: "Unknown", "Protein Data Bank identifier - unique code for this structure")
                        InfoRow("Total Atoms", "${structure.atomCount}", "All atoms in the structure including protein and ligands")
                        InfoRow("Total Bonds", "${structure.bonds.size}", "Chemical bonds connecting atoms in the structure")
                        InfoRow("Chains", "${structure.chainCount}", "Number of polypeptide chains in the protein")
                        val uniqueElements = structure.atoms.map { it.element }.toSet()
                        InfoRow("Elements", "${uniqueElements.size}", "Number of different chemical elements present")
                        InfoRow("Element Types", uniqueElements.sorted().joinToString(", "), "Chemical elements found in this structure")
                    }
                )
                
                // 아이폰 스타일: Chemical Composition (초록색 배경)
                InfoCard(
                    title = "Chemical Composition",
                    backgroundColor = Color(0xFF34C759).copy(alpha = 0.1f),
                    borderColor = Color(0xFF34C759).copy(alpha = 0.3f),
                    content = {
                        val uniqueResidues = structure.atoms.map { it.residueName }.toSet()
                        InfoRow("Residue Types", "${uniqueResidues.size}", "Number of different amino acid types present")
                        val totalResidues = structure.atoms
                            .map { "${it.chain}:${it.residueNumber}" }
                            .distinct()
                            .size
                        InfoRow("Total Residues", "$totalResidues", "Total number of amino acid residues across all chains")
                        val chainList = structure.chains.sorted()
                        InfoRow("Chain IDs", chainList.joinToString(", "), "Identifiers for each polypeptide chain")
                        val hasLigands = structure.atoms.any { it.isLigand }
                        InfoRow("Ligands", if (hasLigands) "Present" else "None", 
                            if (hasLigands) "Small molecules or ions bound to the protein" else "No small molecules detected in this structure")
                    }
                )
                
                // 아이폰 스타일: Experimental Details (주황색 배경)
                InfoCard(
                    title = "Experimental Details",
                    backgroundColor = Color(0xFFFF9500).copy(alpha = 0.1f),
                    borderColor = Color(0xFFFF9500).copy(alpha = 0.3f),
                    content = {
                        InfoRow("Structure Type", "Protein", "This is a protein structure determined by experimental methods")
                        InfoRow("Data Source", "PDB", "Protein Data Bank - worldwide repository of 3D structure data")
                        InfoRow("Quality", "Experimental", "Structure determined through experimental techniques like X-ray crystallography")
                        val firstAtom = structure.atoms.firstOrNull()
                        if (firstAtom != null) {
                            InfoRow("First Residue", firstAtom.residueName, "Chain ${firstAtom.chain}")
                        }
                    }
                )
            }
            
            InfoTab.CHAINS -> {
                val chains = structure.chains
                
                if (chains.isEmpty()) {
                    // 아이폰 스타일: 빈 상태 메시지
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Chains Found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This structure does not contain any protein chains.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                } else {
                    // 아이폰 스타일: 전체 통계 헤더
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF2F2F7)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Protein Chains",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Total Chains",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                    Text(
                                        "${chains.size}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF007AFF)
                                    )
                                }
                                
                                Column {
                                    val totalAtoms = structure.atoms.size
                                    Text(
                                        "Total Atoms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                    Text(
                                        "$totalAtoms",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34C759)
                                    )
                                }
                                
                                Column {
                                    val totalResidues = structure.atoms.map { "${it.chain}_${it.residueNumber}" }.toSet().size
                                    Text(
                                        "Total Residues",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                    Text(
                                        "$totalResidues",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9500)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 아이폰 스타일: 체인별 리스트
                    chains.forEach { chain ->
                        val chainAtoms = structure.atoms.filter { it.chain == chain }
                        val uniqueResidues = chainAtoms.map { it.residueName }.toSet()
                        val caAtoms = chainAtoms.filter { it.name == "CA" }.sortedBy { it.residueNumber }
                        
                        // 아이폰 스타일: 리스트 아이템
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 체인 헤더
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 체인 아이콘
                                        Surface(
                                            modifier = Modifier.size(32.dp),
                                            color = Color(0xFF007AFF),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    chain,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        Column {
                                            Text(
                                                "Chain $chain",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "${caAtoms.size} residues",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    
                                    // 아이폰 스타일: 접근성 인디케이터
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                // 아이폰 스타일: 체인 개요 정보
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Length",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        Text(
                                            "${caAtoms.size} residues",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Column {
                                        Text(
                                            "Atoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        Text(
                                            "${chainAtoms.size}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Column {
                                        Text(
                                            "Residue Types",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        Text(
                                            "${uniqueResidues.size}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                
                                Divider()
                                
                                // 아이폰 스타일: 시퀀스 정보
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Sequence Information",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Text(
                                        "Length: ${caAtoms.size} amino acids",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                    
                                    // 아이폰 스타일: 시퀀스 박스
                                    val sequence = caAtoms.joinToString("") { residueToSingleLetter(it.residueName) }
                                    Text(
                                        sequence,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                            .background(
                                                Color.Gray.copy(alpha = 0.1f), 
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                                
                                Divider()
                                
                                // 아이폰 스타일: 구조적 특성
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Structural Characteristics",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    val backboneAtoms = chainAtoms.count { it.isBackbone }
                                    val sidechainAtoms = chainAtoms.count { !it.isBackbone }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Backbone atoms: $backboneAtoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        Text(
                                            "Side chain atoms: $sidechainAtoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    // Secondary structure elements
                                    val helixCount = chainAtoms.count { it.secondaryStructure == SecondaryStructure.HELIX }
                                    val sheetCount = chainAtoms.count { it.secondaryStructure == SecondaryStructure.SHEET }
                                    val coilCount = chainAtoms.count { it.secondaryStructure == SecondaryStructure.COIL }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "α-helix: $helixCount atoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFF3B30)
                                        )
                                        Text(
                                            "β-sheet: $sheetCount atoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFFCC00)
                                        )
                                        Text(
                                            "Coil: $coilCount atoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                
                                // 아이폰 스타일: 액션 버튼들
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val isHighlighted = uiState.highlightedChains.contains("chain:$chain")
                                    val isFocused = uiState.focusedElement == "chain:$chain"
                                    
                                    // Highlight 버튼 (아이폰 스타일)
                                    Button(
                                        onClick = { 
                                            onStartUpdating()
                                            viewModel.toggleChainHighlight(chain)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isHighlighted) Color(0xFF007AFF) else Color(0xFF007AFF).copy(alpha = 0.1f),
                                            contentColor = if (isHighlighted) Color.White else Color(0xFF007AFF)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            if (isHighlighted) Icons.Default.Edit else Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (isHighlighted) "Unhighlight" else "Highlight",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    
                                    // Focus 버튼 (아이폰 스타일)
                                    Button(
                                        onClick = { 
                                            onStartUpdating()
                                            viewModel.toggleChainFocus(chain)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFocused) Color(0xFF34C759) else Color(0xFF34C759).copy(alpha = 0.1f),
                                            contentColor = if (isFocused) Color.White else Color(0xFF34C759)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            if (isFocused) Icons.Default.MyLocation else Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (isFocused) "Unfocus" else "Focus",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            InfoTab.LIGANDS -> {
                val ligands = structure.atoms.filter { it.isLigand }
                val ligandGroups = ligands.groupBy { it.residueName }
                
                if (ligands.isEmpty()) {
                    // 아이폰 스타일: 빈 상태 메시지
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))
                        Icon(
                            Icons.Default.Science,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Ligands Detected",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This structure does not contain any small molecules or ions bound to the protein.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 아이폰 스타일: 리간드 개요
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF007AFF).copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Ligand Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column {
                                        Text(
                                            "Total Ligands",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        Text(
                                            "${ligandGroups.size}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF007AFF)
                                        )
                                    }
                                    
                                    Column {
                                        Text(
                                            "Total Atoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        Text(
                                            "${ligands.size}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34C759)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        
                        // 아이폰 스타일: 개별 리간드들
                        ligandGroups.keys.sorted().forEach { ligandName ->
                            val ligandAtoms = ligandGroups[ligandName] ?: emptyList()
                            val uniqueChains = ligandAtoms.map { it.chain }.toSet()
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 리간드 헤더
                                    Text(
                                        ligandName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    
                                    // 리간드 정보
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Atoms",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray
                                            )
                                            Text(
                                                "${ligandAtoms.size}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Chains",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray
                                            )
                                            Text(
                                                uniqueChains.sorted().joinToString(", "),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        
                                        // Element composition
                                        val elementCounts = ligandAtoms.groupBy { it.element }
                                            .mapValues { it.value.size }
                                            .toList()
                                            .sortedByDescending { it.second }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Elements",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray
                                            )
                                            Text(
                                                elementCounts.joinToString(", ") { "${it.first}${it.second}" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    
                                    // Binding Information
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Binding Information",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Binding Sites",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                "${uniqueChains.size} chain(s)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Molecular Weight",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                "~${ligandAtoms.size * 12} Da",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    
                                    // 아이폰 스타일: 인터랙티브 버튼들
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val isHighlighted = uiState.highlightedChains.contains("ligand:$ligandName")
                                        val isFocused = uiState.focusedElement == "ligand:$ligandName"
                                        
                                        // Highlight 버튼 (아이폰 스타일)
                                        Button(
                                            onClick = { 
                                                onStartUpdating()
                                                viewModel.toggleLigandHighlight(ligandName)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isHighlighted) Color(0xFF007AFF) else Color(0xFF007AFF).copy(alpha = 0.1f),
                                                contentColor = if (isHighlighted) Color.White else Color(0xFF007AFF)
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(
                                                if (isHighlighted) Icons.Default.Edit else Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                if (isHighlighted) "Unhighlight" else "Highlight",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        
                                        // Focus 버튼 (아이폰 스타일)
                                        Button(
                                            onClick = { 
                                                onStartUpdating()
                                                viewModel.toggleLigandFocus(ligandName)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isFocused) Color(0xFF34C759) else Color(0xFF34C759).copy(alpha = 0.1f),
                                                contentColor = if (isFocused) Color.White else Color(0xFF34C759)
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(
                                                if (isFocused) Icons.Default.MyLocation else Icons.Default.LocationOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                if (isFocused) "Unfocus" else "Focus",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
                    
                    pockets.forEach { (pocketName, _) ->
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
            
            InfoTab.RESIDUES -> {
                // 체인별로 잔기 그룹화
                val residuesByChain = structure.atoms
                    .groupBy { it.chain }
                    .mapValues { (_, atoms) ->
                        atoms.groupBy { "${it.chain}_${it.residueNumber}_${it.residueName}" }
                            .mapValues { (_, residueAtoms) ->
                                val firstAtom = residueAtoms.first()
                                val residueType = when {
                                    firstAtom.isLigand -> "Ligand"
                                    firstAtom.isPocket -> "Pocket"
                                    standardResidues.contains(firstAtom.residueName) -> "Amino Acid"
                                    else -> "Other"
                                }
                                ResidueInfo(
                                    chain = firstAtom.chain,
                                    residueNumber = firstAtom.residueNumber,
                                    residueName = firstAtom.residueName,
                                    residueType = residueType,
                                    atomCount = residueAtoms.size,
                                    atoms = residueAtoms,
                                    secondaryStructure = firstAtom.secondaryStructure
                                )
                            }
                    }

                if (residuesByChain.isEmpty()) {
                    // 아이폰 스타일: 빈 상태 메시지
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))
                        Icon(
                            Icons.Default.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Residues Found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This structure does not contain any residues.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                } else {
                    // 아이폰 스타일: 잔기 구성 개요
                    val residueCounts = structure.atoms
                        .groupBy { it.residueName }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(15)
                    
                    val totalResidues = residueCounts.sumOf { it.second }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF007AFF).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Residue Composition",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            // 상위 15개 잔기 타입 표시
                            residueCounts.forEach { (residueName, count) ->
                                val percentage = (count.toDouble() / totalResidues * 100)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        residueName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(60.dp)
                                    )
                                    
                                    Text(
                                        "$count",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray,
                                        modifier = Modifier.width(40.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    // 색상별 막대 그래프
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(20.dp)
                                            .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(percentage.toFloat() / 100f)
                                                .background(getResidueColorForComposition(residueName), RoundedCornerShape(4.dp))
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        "${String.format("%.1f", percentage)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        modifier = Modifier.width(50.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 아이폰 스타일: 물리화학적 특성
                    val hydrophobicResidues = listOf("ALA", "VAL", "ILE", "LEU", "MET", "PHE", "TRP", "PRO")
                    val polarResidues = listOf("SER", "THR", "ASN", "GLN", "TYR", "CYS")
                    val chargedResidues = listOf("LYS", "ARG", "HIS", "ASP", "GLU")
                    
                    val hydrophobicCount = structure.atoms.count { hydrophobicResidues.contains(it.residueName) }
                    val polarCount = structure.atoms.count { polarResidues.contains(it.residueName) }
                    val chargedCount = structure.atoms.count { chargedResidues.contains(it.residueName) }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF34C759).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Physical-Chemical Properties",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Hydrophobic",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFF9500)
                                )
                                Text(
                                    "$hydrophobicCount atoms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Polar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF007AFF)
                                )
                                Text(
                                    "$polarCount atoms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Charged",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFF3B30)
                                )
                                Text(
                                    "$chargedCount atoms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 아이폰 스타일: 구조적 역할
                    val backboneAtoms = structure.atoms.count { it.isBackbone }
                    val sidechainAtoms = structure.atoms.count { !it.isBackbone }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFAF52DE).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Structural Roles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Backbone",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFAF52DE)
                                )
                                Text(
                                    "$backboneAtoms atoms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Side Chain",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF00C7BE)
                                )
                                Text(
                                    "$sidechainAtoms atoms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // 체인별 잔기 정보
                    residuesByChain.forEach { (chain, residues) ->
                        val chainResidues = residues.values.sortedBy { it.residueNumber }
                        val aminoAcids = chainResidues.filter { it.residueType == "Amino Acid" }
                        val ligands = chainResidues.filter { it.residueType == "Ligand" }
                        val caAtoms = aminoAcids.flatMap { it.atoms.filter { atom -> atom.name == "CA" } }
                            .sortedBy { it.residueNumber }

                        // 아이폰 스타일: 체인 카드
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // 체인 헤더
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 체인 아이콘
                                        Surface(
                                            modifier = Modifier.size(32.dp),
                                            color = Color(0xFF34C759),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    chain,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        Column {
                                            Text(
                                                "Chain $chain Residues",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "${chainResidues.size} total residues",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                // 통계 정보
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "${aminoAcids.size}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34C759)
                                        )
                                        Text(
                                            "Amino Acids",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    if (ligands.isNotEmpty()) {
                                        Column {
                                            Text(
                                                "${ligands.size}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF9500)
                                            )
                                            Text(
                                                "Ligands",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    
                                    Column {
                                        val helixCount = aminoAcids.count { it.secondaryStructure == SecondaryStructure.HELIX }
                                        Text(
                                            "$helixCount",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF3B30)
                                        )
                                        Text(
                                            "Helix",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    Column {
                                        val sheetCount = aminoAcids.count { it.secondaryStructure == SecondaryStructure.SHEET }
                                        Text(
                                            "$sheetCount",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFAF52DE)
                                        )
                                        Text(
                                            "Sheet",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                // 아미노산 시퀀스
                                if (caAtoms.isNotEmpty()) {
                                    Divider()
                                    
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Amino Acid Sequence",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        
                                        // 아이폰 스타일: 시퀀스 박스
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFF8F9FA)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    "Length: ${caAtoms.size} residues",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray
                                                )
                                                
                        // 시퀀스 표시 (한 줄당 60개)
                        val sequence = caAtoms.joinToString("") { residueToSingleLetter(it.residueName) }
                        val chunks = sequence.chunked(60)
                                                chunks.forEachIndexed { index, chunk ->
                                                    Text(
                                                        "${(index * 60 + 1).toString().padStart(3)} $chunk",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 리간드 정보
                                if (ligands.isNotEmpty()) {
                                    Divider()
                                    
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Ligands",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        
                                        ligands.forEach { ligand ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        modifier = Modifier.size(24.dp),
                                                        color = Color(0xFFFF9500),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier.fillMaxSize()
                                                        ) {
                                                            Text(
                                                                ligand.residueNumber.toString(),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    
                                                    Column {
                                                        Text(
                                                            ligand.residueName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            "Residue ${ligand.residueNumber}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                                
                                                Text(
                                                    "${ligand.atomCount} atoms",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
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
    // 아이폰 스타일: InfoCard - 배경색과 테두리 포함
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
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
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
    // 아이폰 스타일: StatCard - 배경 없이 간단한 레이아웃
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    description: String? = null
) {
    // 아이폰 스타일: InfoRow - 배경과 패딩 포함
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
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

// ResidueInfo 데이터 클래스
data class ResidueInfo(
    val chain: String,
    val residueNumber: Int,
    val residueName: String,
    val residueType: String,
    val atomCount: Int,
    val atoms: List<com.avas.proteinviewer.domain.model.Atom>,
    val secondaryStructure: SecondaryStructure
)