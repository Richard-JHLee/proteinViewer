package com.avas.proteinviewer.ui.education

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
import com.avas.proteinviewer.education.EducationManager
import com.avas.proteinviewer.education.ProteinExplanation
import com.avas.proteinviewer.data.model.PDBStructure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationPanel(
    proteinId: String,
    structure: PDBStructure?,
    currentColorMode: String,
    currentRenderMode: String,
    modifier: Modifier = Modifier
) {
    val educationManager = remember { EducationManager() }
    var showEducation by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = modifier) {
        // Education toggle button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Educational Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = showEducation,
                onCheckedChange = { showEducation = it }
            )
        }
        
        // Education content
        if (showEducation && structure != null) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Tab row
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Protein") },
                            icon = { Icon(Icons.Default.Science, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Structure") },
                            icon = { Icon(Icons.Default.Apartment, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Tips") },
                            icon = { Icon(Icons.Default.Lightbulb, contentDescription = null) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Tab content
                    when (selectedTab) {
                        0 -> ProteinInfoTab(
                            explanation = educationManager.getProteinExplanation(proteinId, structure)
                        )
                        1 -> StructureInfoTab(
                            structure = structure,
                            educationManager = educationManager
                        )
                        2 -> TipsTab(
                            currentColorMode = currentColorMode,
                            currentRenderMode = currentRenderMode,
                            educationManager = educationManager
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProteinInfoTab(
    explanation: ProteinExplanation
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = explanation.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = explanation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        item {
            Text(
                text = "Structure Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = explanation.structureInfo,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        item {
            Text(
                text = "Biological Function",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = explanation.biologicalFunction,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        item {
            Text(
                text = "Educational Notes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(explanation.educationalNotes) { note ->
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StructureInfoTab(
    structure: PDBStructure,
    educationManager: EducationManager
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Structure Statistics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Atoms: ${structure.atomCount}")
                Text("Residues: ${structure.residueCount}")
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Chains: ${structure.chainCount}")
                Text("Bonds: ${structure.bonds.size}")
            }
        }
        
        item {
            Divider()
        }
        
        item {
            Text(
                text = "Secondary Structure Elements",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        val helixCount = structure.atoms.count { it.secondaryStructure == com.avas.proteinviewer.data.model.SecondaryStructure.HELIX }
        val sheetCount = structure.atoms.count { it.secondaryStructure == com.avas.proteinviewer.data.model.SecondaryStructure.SHEET }
        val coilCount = structure.atoms.count { it.secondaryStructure == com.avas.proteinviewer.data.model.SecondaryStructure.COIL }
        
        item {
            Text("α-Helix atoms: $helixCount")
        }
        item {
            Text("β-Sheet atoms: $sheetCount")
        }
        item {
            Text("Coil atoms: $coilCount")
        }
        
        item {
            Divider()
        }
        
        item {
            Text(
                text = "Secondary Structure Explanation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        listOf(
            com.avas.proteinviewer.data.model.SecondaryStructure.HELIX,
            com.avas.proteinviewer.data.model.SecondaryStructure.SHEET,
            com.avas.proteinviewer.data.model.SecondaryStructure.COIL
        ).forEach { secStruct ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = secStruct.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = educationManager.getSecondaryStructureExplanation(secStruct),
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
private fun TipsTab(
    currentColorMode: String,
    currentRenderMode: String,
    educationManager: EducationManager
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Current Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Render Mode: $currentRenderMode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when (currentRenderMode.uppercase()) {
                            "SPHERES" -> "Shows individual atoms as spheres - good for seeing atomic details"
                            "STICKS" -> "Shows bonds as sticks - emphasizes chemical bonds"
                            "CARTOON" -> "Shows secondary structure - best for overall protein shape"
                            else -> "Current render mode"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Color Mode: $currentColorMode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = educationManager.getColorSchemeExplanation(currentColorMode),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        item {
            Text(
                text = "Learning Tips",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        val tips = listOf(
            "Try different render modes to understand protein structure at different levels",
            "Use element coloring to identify different types of atoms",
            "Switch to chain coloring to see protein subunits",
            "Use secondary structure coloring to identify α-helices and β-sheets",
            "Rotate the structure to see it from different angles",
            "Zoom in to examine atomic details, zoom out for overall shape",
            "Toggle bonds on/off to focus on specific structural elements"
        )
        
        items(tips) { tip ->
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.TipsAndUpdates,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
