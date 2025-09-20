package com.avas.proteinviewer.ui.protein

import android.app.ActivityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.avas.proteinviewer.data.model.PDBStructure

@Composable
fun ProteinViewerView(
    structure: PDBStructure?,
    proteinId: String = "",
    proteinName: String? = null,
    modifier: Modifier = Modifier,
    backend: RendererBackend = RendererBackend.OpenGL,
    onMenuClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onSwitchToViewer: () -> Unit = {}
) {
    val context = LocalContext.current
    val supportsEs3 = remember {
        val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? ActivityManager
        val config = am?.deviceConfigurationInfo
        (config?.reqGlEsVersion ?: 0) >= 0x00030000
    }
    var selectedTab by remember { mutableStateOf(InfoTab.Overview) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderSection(
            proteinId = proteinId,
            proteinName = proteinName,
            onMenuClick = onMenuClick,
            onLibraryClick = onLibraryClick,
            onSwitchToViewer = onSwitchToViewer
        )

        Column(
            modifier = Modifier.weight(1f, fill = true)
        ) {
            // 고정된 이미지 영역
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "3D Structure Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                PreviewCard {
                    when {
                        backend == RendererBackend.OpenGL && supportsEs3 -> {
                            AndroidView(
                                factory = { ctx ->
                                    OpenGL30SurfaceView(ctx).apply {
                                        updateStructure(structure)
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { view -> view.updateStructure(structure) }
                            )
                        }
                        else -> {
                            BasicFilamentComposeView(
                                structure = structure?.let { s ->
                                    com.avas.proteinviewer.data.converter.StructureConverter.convertPDBToStructure(s)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 스크롤 가능한 정보 영역
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                structure?.let { data ->
                    StatisticsRow(data)
                    Spacer(modifier = Modifier.height(20.dp))
                    when (selectedTab) {
                        InfoTab.Overview -> OverviewContent(data, proteinId)
                        InfoTab.Chains -> ChainsContent(data)
                        InfoTab.Residues -> ResiduesContent(data)
                        InfoTab.Ligands -> LigandsContent(data)
                    }
                } ?: PlaceholderContent()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        InfoTabBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
    }
}

enum class InfoTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Overview("Overview", Icons.Filled.Info),
    Chains("Chains", androidx.compose.material.icons.Icons.Filled.Link),
    Residues("Residues", Icons.Filled.Science),
    Ligands("Ligands", Icons.Filled.LocalPharmacy)
}


@Composable
private fun HeaderSection(
    proteinId: String,
    proteinName: String?,
    onMenuClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onSwitchToViewer: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = proteinId.ifBlank { "Protein" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                proteinName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLibraryClick) {
                    Icon(Icons.Filled.Info, contentDescription = "Info")
                }
                IconButton(onClick = onSwitchToViewer) {
                    Icon(Icons.Filled.RemoveRedEye, contentDescription = "Viewer")
                }
            }
        }
        Divider(color = Color(0xFFE0E0E0), thickness = 0.7.dp)
    }
}

@Composable
private fun PreviewCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}

@Composable
private fun StatisticsRow(structure: PDBStructure) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatTile("Atoms", structure.atomCount.toString(), Color(0xFF1E88E5))
        StatTile("Chains", structure.chainCount.toString(), Color(0xFF43A047))
        StatTile("Residues", structure.residueCount.toString(), Color(0xFFF4511E))
    }
}

@Composable
private fun StatTile(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OverviewContent(structure: PDBStructure, proteinId: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard(
            title = "Structure Information",
            rows = listOf(
                InfoRowData("PDB ID", proteinId.ifBlank { "Unknown" }, "Protein Data Bank identifier"),
                InfoRowData("Total Atoms", structure.atomCount.toString(), "All atoms including ligands"),
                InfoRowData("Total Bonds", structure.bonds.size.toString(), "Chemical bonds connecting atoms"),
                InfoRowData("Chains", structure.chainCount.toString(), "Polypeptide chains in this protein")
            )
        )

        val uniqueElements = structure.atoms.map { it.element }.toSet().sorted()
        InfoCard(
            title = "Chemical Composition",
            rows = listOf(
                InfoRowData("Elements", uniqueElements.size.toString(), "Distinct elements present"),
                InfoRowData("Element Types", uniqueElements.joinToString(), null)
            )
        )

        InfoCard(
            title = "Experimental Details",
            rows = listOf(
                InfoRowData("Structure Type", "Protein", "Experimental 3D structure"),
                InfoRowData("Data Source", "PDB", "Protein Data Bank"),
                InfoRowData("Quality", "Experimental", "Determined via lab methods")
            )
        )
    }
}

@Composable
private fun ChainsContent(structure: PDBStructure) {
    InfoCard(
        title = "Chains",
        rows = structure.chains.map { chain ->
            val residueCount = structure.residues.count { it.chain == chain }
            InfoRowData("Chain $chain", "$residueCount residues", null)
        }
    )
}

@Composable
private fun ResiduesContent(structure: PDBStructure) {
    val residueCounts = structure.residues.groupingBy { it.residueName }.eachCount()
    InfoCard(
        title = "Residues",
        rows = residueCounts.entries.sortedByDescending { it.value }.map { (name, count) ->
            InfoRowData(name, count.toString(), null)
        }
    )
}

@Composable
private fun LigandsContent(structure: PDBStructure) {
    val ligandAtoms = structure.atoms.filter { it.isLigand }
    InfoCard(
        title = "Ligands",
        rows = listOf(
            InfoRowData("Presence", if (ligandAtoms.isEmpty()) "None" else "Present", "Detected ligand atoms in structure"),
            InfoRowData("Ligand Atoms", ligandAtoms.size.toString(), null)
        )
    )
}

@Composable
private fun PlaceholderContent() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Loading protein information…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InfoCard(title: String, rows: List<InfoRowData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            rows.forEach { InfoRow(it) }
        }
    }
}

private data class InfoRowData(val title: String, val value: String, val description: String?)

@Composable
private fun InfoRow(data: InfoRowData) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(data.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(data.value, style = MaterialTheme.typography.bodyMedium)
        }
        data.description?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Divider(color = Color(0xFFE3E5E8), thickness = 0.5.dp)
    }
}

@Composable
private fun InfoTabBar(selectedTab: InfoTab, onTabSelected: (InfoTab) -> Unit) {
    NavigationBar {
        InfoTab.values().forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
