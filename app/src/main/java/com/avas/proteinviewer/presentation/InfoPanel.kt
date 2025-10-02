package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        // Content only - 탭바는 InfoModeScreen의 bottomBar로 이동
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                InfoTab.OVERVIEW -> {
                    // Basic Statistics Cards (아이폰과 동일)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(
                            title = "Atoms",
                            value = "${structure.atomCount}",
                            color = Color(0xFF2196F3), // Blue
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Chains",
                            value = "${structure.chainCount}",
                            color = Color(0xFF4CAF50), // Green
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Residues",
                            value = "${structure.residueCount}",
                            color = Color(0xFFFF9800), // Orange
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Structure Information (파란색 카드)
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
                    
                    // Chemical Composition (녹색 카드)
                    InfoCard(
                        title = "Chemical Composition",
                        backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        borderColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                        content = {
                            val uniqueResidues = structure.atoms.map { it.residueName }.toSet()
                            InfoRow("Residue Types", "${uniqueResidues.size}", "Number of different amino acid types present")
                            InfoRow("Total Residues", "${structure.residueCount}", "Total number of amino acid residues across all chains")
                            val chainList = structure.chains.sorted().joinToString(", ")
                            InfoRow("Chain IDs", chainList, "Identifiers for all polypeptide chains")
                            val hasLigands = structure.atoms.any { it.isLigand }
                            InfoRow("Ligands", if (hasLigands) "Present" else "None", 
                                if (hasLigands) "Small molecules or ions bound to the protein" else "No small molecules detected in this structure")
                        }
                    )
                    
                    // Experimental Details (주황색 카드)
                    InfoCard(
                        title = "Experimental Details",
                        backgroundColor = Color(0xFFFF9800).copy(alpha = 0.1f),
                        borderColor = Color(0xFFFF9800).copy(alpha = 0.3f),
                        content = {
                            InfoRow("Structure Type", "Protein", "This is a protein structure determined by experimental methods")
                            InfoRow("Data Source", "PDB", "Protein Data Bank - worldwide repository of 3D structure data")
                            InfoRow("Quality", "Experimental", "Structure determined through experimental techniques like X-ray crystallography")
                            structure.atoms.firstOrNull()?.let { firstAtom ->
                                InfoRow("First Residue", firstAtom.residueName, "Chain ${firstAtom.chain}")
                            }
                        }
                    )
                }
                
                InfoTab.CHAINS -> {
                    structure.chains.sorted().forEach { chain ->
                        val chainAtoms = structure.atoms.filter { it.chain == chain }
                        val residueCount = chainAtoms.map { it.residueNumber }.toSet().size
                        val uniqueResidueTypes = chainAtoms.map { it.residueName }.toSet().size
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Chain Header (아이폰과 동일)
                                Text(
                                    text = "Chain $chain",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                // Chain Overview (아이폰과 동일)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "Length",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "$residueCount residues",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "Atoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "${chainAtoms.size}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "Residue Types",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "$uniqueResidueTypes",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                
                                // Sequence Information (아이폰과 동일)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color(0xFFF5F5F5),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Sequence Information",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    val caAtoms = chainAtoms.filter { it.name == "CA" }.sortedBy { it.residueNumber }
                                    val sequence = caAtoms.joinToString("") { residueToSingleLetter(it.residueName) }
                                    
                                    Text(
                                        "Length: ${caAtoms.size} residues",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    // Sequence (scrollable, 아이폰과 동일)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .background(
                                                Color(0xFFE0E0E0),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .verticalScroll(rememberScrollState())
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = sequence,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                                
                                // Structural Characteristics (아이폰과 동일)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color(0xFFF5F5F5),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Structural Characteristics",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    val backboneAtoms = chainAtoms.count { it.isBackbone }
                                    val sidechainAtoms = chainAtoms.size - backboneAtoms
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Backbone atoms: $backboneAtoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Side chain atoms: $sidechainAtoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    // Secondary Structure
                                    val helixAtoms = chainAtoms.count { it.secondaryStructure == SecondaryStructure.HELIX }
                                    val sheetAtoms = chainAtoms.count { it.secondaryStructure == SecondaryStructure.SHEET }
                                    val coilAtoms = chainAtoms.count { it.secondaryStructure == SecondaryStructure.COIL }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "α-helix: $helixAtoms atoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFF44336) // Red
                                        )
                                        Text(
                                            "β-sheet: $sheetAtoms atoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFFEB3B) // Yellow
                                        )
                                        Text(
                                            "Coil: $coilAtoms atoms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                
                                // Highlight and Focus Buttons (아이폰과 동일)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { /* TODO: Toggle highlight */ },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2196F3)
                                        )
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Highlight")
                                    }
                                    
                                    Button(
                                        onClick = { /* TODO: Focus */ },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50)
                                        )
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Focus")
                                    }
                                }
                            }
                        }
                    }
                }
                
                InfoTab.RESIDUES -> {
                    // Residue Composition (아이폰과 동일)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                        )
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
                            
                            val residueCounts = structure.atoms
                                .groupBy { it.residueName }
                                .mapValues { it.value.size }
                                .toList()
                                .sortedByDescending { it.second }
                            
                            val totalResidues = residueCounts.sumOf { it.second }
                            
                            // Top 15 residues (아이폰과 동일)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                residueCounts.take(15).forEach { (residue, count) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = residue,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.width(50.dp)
                                        )
                                        
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(35.dp)
                                        )
                                        
                                        // Progress bar (화면 너비에 맞게)
                                        val percentage = count.toFloat() / totalResidues
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(16.dp)
                                                .background(
                                                    Color(0xFFE0E0E0),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(percentage)
                                                    .background(
                                                        getResidueColor(residue),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                            )
                                        }
                                        
                                        Text(
                                            text = "${String.format("%.1f", percentage * 100)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(45.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
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
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
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

private fun getResidueColor(residueName: String): Color {
    // 아미노산 타입별 색상 (아이폰과 유사)
    return when (residueName.uppercase()) {
        // 소수성 (Hydrophobic) - 파란색 계열
        "ALA", "VAL", "ILE", "LEU", "MET", "PHE", "TRP", "PRO" -> Color(0xFF2196F3)
        // 극성 (Polar) - 녹색 계열
        "SER", "THR", "CYS", "TYR", "ASN", "GLN" -> Color(0xFF4CAF50)
        // 양전하 (Positive) - 빨간색 계열
        "LYS", "ARG", "HIS" -> Color(0xFFF44336)
        // 음전하 (Negative) - 주황색 계열
        "ASP", "GLU" -> Color(0xFFFF9800)
        // 특수 - 회색
        "GLY" -> Color(0xFF9E9E9E)
        // 기타 - 보라색
        else -> Color(0xFF9C27B0)
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

