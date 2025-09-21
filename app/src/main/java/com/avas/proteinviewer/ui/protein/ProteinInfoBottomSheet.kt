package com.avas.proteinviewer.ui.protein

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.ui.protein.InfoTab

@Composable
fun ProteinInfoBottomSheet(
    structure: PDBStructure?,
    proteinId: String = "",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember { InfoTab.values().toList() }
    var selectedTab by remember { mutableStateOf(InfoTab.Overview) }

    Surface(
        modifier = modifier,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            SheetHeader(onDismiss = onDismiss, structure = structure, proteinId = proteinId)

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                InfoTab.Overview -> OverviewSection(structure, proteinId)
                InfoTab.Chains -> ChainsSection(structure)
                InfoTab.Residues -> ResiduesSection(structure)
                InfoTab.Ligands -> LigandsSection(structure)
                InfoTab.Pockets -> PocketsSection(structure)
                InfoTab.Sequence -> SequenceSection(structure)
                InfoTab.Annotations -> AnnotationsSection(structure)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TabBar(tabs = tabs, selected = selectedTab, onSelect = { selectedTab = it })
        }
    }
}

// InfoTab은 ProteinViewerView.kt에서 import하여 사용

@Composable
private fun SheetHeader(onDismiss: () -> Unit, structure: PDBStructure?, proteinId: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = proteinId.ifBlank { "Protein" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Structure Details",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun TabBar(tabs: List<InfoTab>, selected: InfoTab, onSelect: (InfoTab) -> Unit) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}



@Composable
private fun OverviewSection(structure: PDBStructure?, proteinId: String) {
    structure ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SheetInfoCard(
            title = "Structure Information",
            rows = listOf(
                SheetInfoItem("PDB ID", proteinId.ifBlank { "Unknown" }),
                SheetInfoItem("Total Atoms", structure.atoms.size.toString()),
                SheetInfoItem("Total Bonds", structure.bonds.size.toString()),
                SheetInfoItem("Chains", structure.chains.joinToString()),
                SheetInfoItem("Residues", structure.residues.size.toString())
            )
        )
    }
}

@Composable
private fun SheetInfoCard(title: String, rows: List<SheetInfoItem>) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            rows.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = item.value, style = MaterialTheme.typography.bodyMedium)
                    }
                    item.description?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private data class SheetInfoItem(val title: String, val value: String, val description: String? = null)

@Composable
private fun ChainsSection(structure: PDBStructure?) {
    structure ?: return
    SheetInfoCard(
        title = "Protein Chains",
        rows = structure.chains.sorted().map { chain ->
            val residueCount = structure.residues.count { it.chain == chain }
            SheetInfoItem("Chain $chain", "$residueCount residues")
        }
    )
}

@Composable
private fun ResiduesSection(structure: PDBStructure?) {
    structure ?: return
    val residueTypes = structure.residues.map { it.residueName }.groupingBy { it }.eachCount()
    SheetInfoCard(
        title = "Residue Types",
        rows = residueTypes.entries.sortedByDescending { it.value }.map { (name, count) ->
            SheetInfoItem(name, "$count occurrences")
        }
    )
}

@Composable
private fun LigandsSection(structure: PDBStructure?) {
    structure ?: return
    val ligands = structure.atoms.filter { it.isLigand }
    SheetInfoCard(
        title = "Ligands",
        rows = listOf(
            SheetInfoItem("Ligand Atoms", ligands.size.toString(), "Atoms classified as ligands in this structure"),
            SheetInfoItem("Presence", if (ligands.isNotEmpty()) "Present" else "None")
        )
    )
}

@Composable
private fun PocketsSection(structure: PDBStructure?) {
    structure ?: return
    val pocketAtoms = structure.atoms.filter { it.isPocket }
    SheetInfoCard(
        title = "Binding Pockets",
        rows = listOf(
            SheetInfoItem("Pocket Atoms", pocketAtoms.size.toString(), "Atoms classified as binding pockets"),
            SheetInfoItem("Presence", if (pocketAtoms.isNotEmpty()) "Present" else "None")
        )
    )
}

@Composable
private fun SequenceSection(structure: PDBStructure?) {
    structure ?: return
    val chains = structure.atoms.map { it.chain }.distinct().sorted()
    SheetInfoCard(
        title = "Sequence Information",
        rows = listOf(
            SheetInfoItem("Chains", chains.size.toString(), "Number of polypeptide chains"),
            SheetInfoItem("Total Residues", structure.residues.size.toString(), "Total number of residues")
        )
    )
}

@Composable
private fun AnnotationsSection(structure: PDBStructure?) {
    structure ?: return
    val annotations = structure.annotations
    if (annotations.isEmpty()) {
        SheetInfoCard(
            title = "Annotations",
            rows = listOf(
                SheetInfoItem("Status", "No annotations", "Supplementary annotation data was not provided")
            )
        )
    } else {
        SheetInfoCard(
            title = "Annotations",
            rows = annotations.map { annotation ->
                SheetInfoItem(
                    title = annotation.type.displayName,
                    value = annotation.value,
                    description = annotation.description.takeIf { it.isNotBlank() }
                )
            }
        )
    }
}
