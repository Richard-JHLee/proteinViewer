package com.avas.proteinviewer.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.domain.model.ProteinInfo
import com.avas.proteinviewer.domain.model.QuaternaryStructureData

/**
 * 아이폰 QuaternaryStructureView와 완전히 동일
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuaternaryStructureScreen(
    protein: ProteinInfo,
    uiState: ProteinUiState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.95f),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(60.dp))
                    Text(
                        text = "Quaternary Structure",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Done", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            HorizontalDivider()
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    uiState.isQuaternaryStructureLoading -> {
                        QuaternaryStructureLoadingView()
                    }
                    uiState.quaternaryStructureError != null -> {
                        QuaternaryStructureErrorView(uiState.quaternaryStructureError!!) { }
                    }
                    uiState.quaternaryStructureData != null -> {
                        QuaternaryStructureContentView(protein, uiState.quaternaryStructureData!!)
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No quaternary structure data available")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuaternaryStructureLoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Text("Loading quaternary structure...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuaternaryStructureErrorView(error: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9500), modifier = Modifier.size(64.dp))
            Text("Failed to load structure", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun QuaternaryStructureContentView(
    protein: ProteinInfo,
    data: QuaternaryStructureData
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Subunit Assembly",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "PDB ID: ${protein.id}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Protein Subunits
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Protein Subunits",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            data.subunits.forEach { subunit ->
                Surface(
                    color = Color(protein.category.color).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Subunit ${subunit.id}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(protein.category.color)
                            )
                            Text(
                                text = "${subunit.residueCount} residues",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = subunit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Assembly Information (아이폰과 동일)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Assembly Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Surface(
                color = Color(0xFF5856D6).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssemblyInfoRow("Assembly Type", data.assembly.type)
                    HorizontalDivider()
                    AssemblyInfoRow("Symmetry", data.assembly.symmetry)
                    HorizontalDivider()
                    AssemblyInfoRow("Oligomeric Count", "${data.assembly.oligomericCount}")
                    HorizontalDivider()
                    AssemblyInfoRow("Polymer Composition", data.assembly.polymerComposition)
                    HorizontalDivider()
                    AssemblyInfoRow("Total Mass", String.format("%.2f kDa", data.assembly.totalMass))
                    HorizontalDivider()
                    AssemblyInfoRow("Atom Count", "${data.assembly.atomCount}")
                    
                    data.assembly.methodDetails?.let { method ->
                        HorizontalDivider()
                        AssemblyInfoRow("Method", method)
                    }
                    
                    data.assembly.isCandidateAssembly?.let { isCandidate ->
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Biological Assembly:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isCandidate) "Yes" else "No",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCandidate) Color(0xFF34C759) else Color(0xFFFF3B30)
                            )
                        }
                    }
                }
            }
        }
        
        // Symmetry Details (아이폰과 동일)
        if (data.assembly.symmetryDetails.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Symmetry Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                data.assembly.symmetryDetails.forEach { symmetry ->
                    Surface(
                        color = Color(0xFF007AFF).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${symmetry.symbol} (${symmetry.kind})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF007AFF)
                                )
                                Text(
                                    text = symmetry.oligomericState,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (symmetry.stoichiometry.isNotEmpty()) {
                                Text(
                                    text = "Stoichiometry: ${symmetry.stoichiometry.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            symmetry.avgRmsd?.let { rmsd ->
                                Text(
                                    text = "RMSD: ${String.format("%.2f", rmsd)} Å",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Biological Relevance (아이폰과 동일)
        data.assembly.biologicalRelevance?.let { relevance ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Biological Relevance",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Surface(
                    color = Color(0xFF34C759).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = relevance,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }
        }
        
        // Subunit Interactions (아이폰과 동일)
        if (data.interactions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Subunit Interactions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                data.interactions.forEach { interaction ->
                    Surface(
                        color = Color(0xFF9C27B0).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${interaction.subunit1} ↔ ${interaction.subunit2}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF9C27B0)
                                )
                                if (interaction.contactCount > 0) {
                                    Text(
                                        text = "${interaction.contactCount} contacts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Text(
                                text = interaction.description,
                                style = MaterialTheme.typography.bodySmall,
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
private fun AssemblyInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

