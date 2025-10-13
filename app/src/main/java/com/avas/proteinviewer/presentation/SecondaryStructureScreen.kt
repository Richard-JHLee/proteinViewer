package com.avas.proteinviewer.presentation

import androidx.compose.foundation.background
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
import com.avas.proteinviewer.domain.model.SecondaryStructureData

/**
 * 아이폰 SecondaryStructureView와 완전히 동일
 */

// Helper function for structure type ordering
private fun getStructureOrder(type: String): Int {
    val lowerType = type.lowercase()
    return when {
        lowerType.contains("helix") || lowerType.contains("α-helix") -> 1 // α-helix first
        lowerType.contains("strand") || lowerType.contains("β-strand") || lowerType.contains("sheet") -> 2 // β-strand second
        lowerType.contains("turn") -> 3 // turn third
        lowerType.contains("bend") -> 4 // bend fourth
        lowerType.contains("coil") -> 5 // coil last
        else -> 6 // others
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryStructureScreen(
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
                        text = "Secondary Structure",
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
                    uiState.isSecondaryStructureLoading -> {
                        SecondaryStructureLoadingView()
                    }
                    uiState.secondaryStructureError != null -> {
                        SecondaryStructureErrorView(uiState.secondaryStructureError!!) { }
                    }
                    uiState.secondaryStructureData.isNotEmpty() -> {
                        // 구조 타입별로 정렬: α-helix, β-strand, coil 순서
                        val sortedStructures = uiState.secondaryStructureData.sortedWith(
                            compareBy<SecondaryStructureData> { getStructureOrder(it.type) }
                                .thenBy { it.start }
                        )
                        SecondaryStructureContentView(protein, sortedStructures)
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No secondary structure data available")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondaryStructureLoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Text("Loading secondary structure...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SecondaryStructureErrorView(error: String, onRetry: () -> Unit) {
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
private fun SecondaryStructureContentView(
    protein: ProteinInfo,
    structures: List<SecondaryStructureData>
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
                text = "Secondary Structure Elements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "PDB ID: ${protein.id}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${structures.size} elements found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Group by type
        val grouped = structures.groupBy { it.displayName }
        
        grouped.keys.sorted().forEach { typeName ->
            val items = grouped[typeName] ?: emptyList()
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = typeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${items.size} elements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                items.forEach { structure ->
                    Surface(
                        color = Color(structure.color).copy(alpha = 0.1f),
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
                                    text = structure.type,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(structure.color)
                                )
                                Text(
                                    text = "Residues ${structure.start}-${structure.end}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Length: ${structure.length} residues",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                if (structure.confidence < 1.0) {
                                    Text(
                                        text = "Confidence: ${(structure.confidence * 100).toInt()}%",
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
    }
}

