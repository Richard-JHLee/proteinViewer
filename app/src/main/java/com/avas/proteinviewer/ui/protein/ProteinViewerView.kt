package com.avas.proteinviewer.ui.protein

import android.app.ActivityManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.avas.proteinviewer.data.model.Atom
import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.data.model.SecondaryStructure
import com.avas.proteinviewer.ui.theme.CoilColor
import com.avas.proteinviewer.ui.theme.HelixColor
import com.avas.proteinviewer.ui.theme.SheetColor
import com.avas.proteinviewer.viewmodel.ProteinViewModel

@Composable
fun ProteinViewerView(
    structure: PDBStructure?,
    proteinId: String = "",
    proteinName: String? = null,
    modifier: Modifier = Modifier,
    backend: RendererBackend = RendererBackend.OpenGL,
    onMenuClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onSwitchToViewer: () -> Unit = {},
    viewModel: ProteinViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val supportsEs3 = remember {
        val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? ActivityManager
        val config = am?.deviceConfigurationInfo
        (config?.reqGlEsVersion ?: 0) >= 0x00030000
    }
    val appState by viewModel.appState.collectAsState()
    var selectedTab by remember { mutableStateOf(InfoTab.Overview) }
    var surfaceView by remember { mutableStateOf<OpenGL30SurfaceView?>(null) }
    var highlightedChains by remember { mutableStateOf(setOf<String>()) }
    var focusedChain by remember { mutableStateOf<String?>(null) }
    var highlightedPockets by remember { mutableStateOf(appState.highlightedPockets) }
    var focusedPocket by remember { mutableStateOf(appState.focusedPocket) }

    LaunchedEffect(appState.highlightedPockets) {
        if (highlightedPockets != appState.highlightedPockets) {
            highlightedPockets = appState.highlightedPockets
        }
    }

    LaunchedEffect(appState.focusedPocket) {
        if (focusedPocket != appState.focusedPocket) {
            focusedPocket = appState.focusedPocket
        }
    }

    LaunchedEffect(proteinId) {
        highlightedChains = emptySet()
        focusedChain = null
        surfaceView?.updateHighlights(emptySet(), null)
        highlightedPockets = emptySet()
        focusedPocket = null
        surfaceView?.updatePocketHighlights(emptySet(), null)
        viewModel.updateHighlightedPockets(emptySet())
        viewModel.updateFocusedPocket(null)
    }

    LaunchedEffect(highlightedChains, focusedChain, highlightedPockets, focusedPocket, surfaceView, supportsEs3, backend) {
        if (backend == RendererBackend.OpenGL && supportsEs3) {
            surfaceView?.updateHighlights(highlightedChains, focusedChain)
            surfaceView?.updatePocketHighlights(highlightedPockets, focusedPocket)
        }
    }
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
                                OpenGL30SurfaceView(ctx).also { view ->
                                    surfaceView = view
                                    view.updateStructure(structure)
                                    view.updateHighlights(highlightedChains, focusedChain)
                                    view.updatePocketHighlights(highlightedPockets, focusedPocket)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { view ->
                                view.updateStructure(structure)
                                view.updateHighlights(highlightedChains, focusedChain)
                                view.updatePocketHighlights(highlightedPockets, focusedPocket)
                            }
                        )
                   }
                    else -> {
                        surfaceView = null
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
                    InfoTab.Chains -> ChainsContent(
                        structure = data,
                        highlightedChains = highlightedChains,
                        focusedChain = focusedChain,
                        onToggleHighlight = { chain ->
                            highlightedChains = highlightedChains.toMutableSet().also { set ->
                                if (!set.add(chain)) set.remove(chain)
                            }.toSet()
                        },
                        onToggleFocus = { chain ->
                            focusedChain = if (focusedChain == chain) null else chain
                        }
                    )
                    InfoTab.Residues -> ResiduesContent(data)
                    InfoTab.Ligands -> LigandsContent(data)
                    InfoTab.Pockets -> PocketsContent(
                        structure = data,
                        highlightedPockets = highlightedPockets,
                        focusedPocket = focusedPocket,
                        onToggleHighlight = { pocket ->
                            val newHighlightedPockets = highlightedPockets.toMutableSet().also { set ->
                                if (!set.add(pocket)) set.remove(pocket)
                            }.toSet()
                            highlightedPockets = newHighlightedPockets
                            viewModel.updateHighlightedPockets(newHighlightedPockets)
                        },
                        onToggleFocus = { pocket, atomCount ->
                            if (atomCount == 0) {
                                Toast.makeText(context, "No atoms available to focus.", Toast.LENGTH_SHORT).show()
                            } else {
                                focusedPocket = if (focusedPocket == pocket) null else pocket
                                viewModel.updateFocusedPocket(focusedPocket)
                            }
                        }
                    )
                    InfoTab.Sequence -> SequenceContent(data)
                    InfoTab.Annotations -> AnnotationsContent(data, proteinId)
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
    Ligands("Ligands", Icons.Filled.LocalPharmacy),
    Pockets("Pockets", Icons.Filled.Category),
    Sequence("Sequence", Icons.Filled.ViewList),
    Annotations("Annotations", Icons.Filled.Article)
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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // iPhone과 동일: PDB ID가 메인 타이틀 (.title3, .fontWeight(.semibold))
                if (proteinId.isNotBlank()) {
                    Text(
                        text = proteinId,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // iPhone과 동일: 단백질 이름이 서브타이틀 (.callout, .foregroundColor(.secondary))
                if (proteinName?.isNotBlank() == true) {
                    Text(
                        text = proteinName ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // iPhone과 동일: books.vertical 아이콘 (라이브러리)
                IconButton(onClick = onLibraryClick) {
                    Icon(Icons.Filled.MenuBook, contentDescription = "Protein Library")
                }
                // iPhone과 동일: eye 아이콘 (뷰어)
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
private fun ChainsContent(
    structure: PDBStructure,
    highlightedChains: Set<String>,
    focusedChain: String?,
    onToggleHighlight: (String) -> Unit,
    onToggleFocus: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        structure.chains.forEach { chain ->
            val chainAtoms = structure.atoms.filter { it.chain == chain }
            val residueNumbers = chainAtoms.map { it.residueNumber }.distinct().sorted()
            val residueCount = residueNumbers.size
            val residueTypes = chainAtoms.map { it.residueName }.distinct()
            val sequence = buildString {
                residueNumbers.forEach { resNo ->
                    val resName = chainAtoms.firstOrNull { it.residueNumber == resNo }?.residueName ?: "UNK"
                    append(residueThreeToOne(resName))
                }
            }
            val backboneCount = chainAtoms.count { it.isBackbone }
            val sideChainCount = chainAtoms.size - backboneCount
            val helixCount = chainAtoms.count { it.secondaryStructure == SecondaryStructure.HELIX }
            val sheetCount = chainAtoms.count { it.secondaryStructure == SecondaryStructure.SHEET }
            val coilCount = chainAtoms.count { it.secondaryStructure == SecondaryStructure.COIL || it.secondaryStructure == SecondaryStructure.UNKNOWN }
            val isHighlighted = highlightedChains.contains(chain)
            val isFocused = focusedChain == chain

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Chain $chain", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ChainStat(label = "Length", value = "$residueCount residues")
                        ChainStat(label = "Atoms", value = chainAtoms.size.toString())
                        ChainStat(label = "Residue Types", value = residueTypes.size.toString())
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sequence", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = sequence.ifEmpty { "-" },
                                fontSize = 12.sp,
                                letterSpacing = 1.sp,
                                modifier = Modifier
                                    .background(Color(0xFFE7E9F3), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Structural Characteristics", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Backbone atoms: $backboneCount", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Side-chain atoms: $sideChainCount", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("α-helix: $helixCount", style = MaterialTheme.typography.bodySmall, color = HelixColor)
                            Text("β-sheet: $sheetCount", style = MaterialTheme.typography.bodySmall, color = SheetColor)
                            Text("Coil: $coilCount", style = MaterialTheme.typography.bodySmall, color = CoilColor)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            onClick = { onToggleHighlight(chain) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Filled.Highlight, contentDescription = "Highlight")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHighlighted) "Unhighlight" else "Highlight")
                        }

                        if (isFocused) {
                            FilledTonalButton(
                                onClick = { onToggleFocus(chain) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Icon(Icons.Filled.Star, contentDescription = "Clear focus")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Focus")
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { onToggleFocus(chain) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Filled.Star, contentDescription = "Focus")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Focus")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResiduesContent(structure: PDBStructure) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ResidueCompositionCard(structure)
        PhysicalChemicalPropertiesCard(structure)
        StructuralRolesCard(structure)
    }
}

@Composable
private fun ResidueCompositionCard(structure: PDBStructure) {
    val residueCounts = structure.atoms.groupBy { it.residueName }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
    val totalResidues = residueCounts.sumOf { it.second }.coerceAtLeast(1)
    val maxBarWidth = 150.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Residue Composition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (residueCounts.isEmpty()) {
                Text("No residue data available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                residueCounts.take(15).forEach { (residue, count) ->
                    val percentage = (count.toDouble() / totalResidues.toDouble()) * 100.0
                    val barColor = residueCategoryColor(residue)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = residue,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(54.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(48.dp),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .height(18.dp)
                                .width(maxBarWidth)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFD6E4FF))
                        ) {
                            val fraction = (percentage.toFloat() / 100f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(maxBarWidth * fraction)
                                    .background(barColor, RoundedCornerShape(4.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${String.format("%.1f", percentage)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(72.dp),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhysicalChemicalPropertiesCard(structure: PDBStructure) {
    val hydrophobicCount = structure.atoms.count { hydrophobicResidues.contains(it.residueName) }
    val polarCount = structure.atoms.count { polarResidues.contains(it.residueName) }
    val chargedCount = structure.atoms.count { chargedResidues.contains(it.residueName) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Physical-Chemical Properties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            PropertyRow(label = "Hydrophobic", value = "$hydrophobicCount atoms", color = Color(0xFFF57C00))
            PropertyRow(label = "Polar", value = "$polarCount atoms", color = Color(0xFF1976D2))
            PropertyRow(label = "Charged", value = "$chargedCount atoms", color = Color(0xFFD32F2F))
        }
    }
}

@Composable
private fun StructuralRolesCard(structure: PDBStructure) {
    val backboneCount = structure.atoms.count { it.isBackbone }
    val sideChainCount = structure.atoms.count { !it.isBackbone }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Structural Roles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            PropertyRow(label = "Backbone", value = "$backboneCount atoms", color = Color(0xFF8E24AA))
            PropertyRow(label = "Side Chain", value = "$sideChainCount atoms", color = Color(0xFF00ACC1))
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = color)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun residueCategoryColor(residue: String): Color {
    val key = residue.uppercase()
    return when {
        hydrophobicResidues.contains(key) -> Color(0xFFF57C00)
        polarResidues.contains(key) -> Color(0xFF1976D2)
        chargedResidues.contains(key) -> Color(0xFFD32F2F)
        else -> Color(0xFF9E9E9E)
    }
}

private val hydrophobicResidues = setOf("ALA", "VAL", "ILE", "LEU", "MET", "PHE", "TRP", "PRO")
private val polarResidues = setOf("SER", "THR", "ASN", "GLN", "TYR", "CYS")
private val chargedResidues = setOf("LYS", "ARG", "HIS", "ASP", "GLU")

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
private fun PocketsContent(
    structure: PDBStructure,
    highlightedPockets: Set<String>,
    focusedPocket: String?,
    onToggleHighlight: (String) -> Unit,
    onToggleFocus: (String, Int) -> Unit
) {
    val pocketAtoms = structure.atoms.filter { it.isPocket }
    if (pocketAtoms.isEmpty()) {
        InfoCard(
            title = "Binding Pockets",
            rows = listOf(
                InfoRowData("Detected Pockets", "0", "No binding pockets or active sites were identified"),
                InfoRowData("Pocket Atoms", "0", null)
            )
        )
        return
    }

    val pocketGroups = pocketAtoms.groupBy { it.residueName }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Binding Pocket Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Text("Pockets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(pocketGroups.size.toString(), style = MaterialTheme.typography.titleMedium, color = Color(0xFF8E24AA))
                    }
                    Column {
                        Text("Pocket Atoms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(pocketAtoms.size.toString(), style = MaterialTheme.typography.titleMedium, color = Color(0xFFF57C00))
                    }
                }

                val topElements = pocketAtoms.groupBy { it.element }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(4)
                if (topElements.isNotEmpty()) {
                    Text("Common Elements", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        topElements.joinToString { "${it.first} ${it.second}" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        val sortedPockets = pocketGroups.toList().sortedWith(
            compareByDescending<Pair<String, List<com.avas.proteinviewer.data.model.Atom>>> { (name, _) ->
                if (focusedPocket != null && focusedPocket == name) 1 else 0
            }.thenByDescending { it.second.size }
        )

        sortedPockets.forEach { (name, atoms) ->
            val chains = atoms.map { it.chain }.distinct().sorted()
            val atomCount = atoms.size
            val pocketLabel = if (name.isBlank()) "Pocket" else "Pocket $name"
            val isHighlighted = highlightedPockets.contains(name)
            val isFocused = focusedPocket == name
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFocused) Color(0xFFD1C4E9) else Color(0xFFEDE7F6)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(pocketLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    PropertyRow(label = "Atoms", value = atomCount.toString(), color = Color(0xFF5E35B1))
                    PropertyRow(label = "Chains", value = chains.joinToString(", "), color = Color(0xFF00897B))

                    val elementCounts = atoms.groupBy { it.element }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(6)
                    if (elementCounts.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Element Composition", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            elementCounts.forEach { (element, count) ->
                                Text("$element: $count", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            onClick = { onToggleHighlight(name) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Filled.Highlight, contentDescription = "Highlight pocket")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHighlighted) "Unhighlight" else "Highlight")
                        }

                        FilledTonalButton(
                            onClick = { onToggleFocus(name, atomCount) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (isFocused) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = "Focus pocket")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFocused) "Clear Focus" else "Focus")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SequenceContent(structure: PDBStructure) {
    val chains = structure.atoms.map { it.chain }.distinct().sorted()
    val totalResidues = structure.residues.size

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column {
                    Text("Chains", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(chains.size.toString(), style = MaterialTheme.typography.titleMedium, color = Color(0xFF1E88E5))
                }
                Column {
                    Text("Total Residues", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(totalResidues.toString(), style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                }
            }
        }

        chains.forEach { chain ->
            val chainAtoms = structure.atoms.filter { it.chain == chain }
            val chainResidues = structure.residues.filter { it.chain == chain }
            val sequence = chainResidues.joinToString(separator = "") { residueThreeToOne(it.residueName) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Chain $chain Sequence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Length: ${sequence.length} residues", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEEEEEE))
                            .padding(12.dp)
                    ) {
                        Text(sequence, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    }

                    val composition = chainAtoms.groupBy { it.residueName }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                    if (composition.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Sequence Composition", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            composition.take(10).forEach { (resName, count) ->
                                val percentage = (count.toDouble() / chainAtoms.size.toDouble()) * 100.0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(resName, modifier = Modifier.width(50.dp), style = MaterialTheme.typography.bodySmall)
                                    Text(count.toString(), modifier = Modifier.width(48.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(16.dp)
                                            .width(160.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFD6E4FF))
                                    ) {
                                        val fraction = (percentage.toFloat() / 100f).coerceIn(0f, 1f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(160.dp * fraction)
                                                .background(residueCategoryColor(resName), RoundedCornerShape(4.dp))
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${String.format("%.1f", percentage)}%",
                                        modifier = Modifier.width(64.dp),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val allResidues = structure.atoms.map { it.residueName }
        if (allResidues.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Overall Sequence Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    val overallComposition = allResidues.groupBy { it }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Most Common Residues", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        overallComposition.take(5).forEach { (residue, count) ->
                            val percentage = (count.toDouble() / allResidues.size.toDouble()) * 100.0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(residue, modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(count.toString(), modifier = Modifier.width(48.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .width(180.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFFFE0B2))
                                ) {
                                    val fraction = (percentage.toFloat() / 100f).coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(180.dp * fraction)
                                            .background(residueCategoryColor(residue), RoundedCornerShape(4.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${String.format("%.1f", percentage)}%",
                                    modifier = Modifier.width(64.dp),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sequence Statistics", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val uniqueResidues = overallComposition.size
                        val averageFrequency = if (uniqueResidues == 0) 0.0 else allResidues.size.toDouble() / uniqueResidues.toDouble()
                        PropertyRow(label = "Unique Residue Types", value = uniqueResidues.toString(), color = Color(0xFFFB8C00))
                        PropertyRow(label = "Average Residue Frequency", value = String.format("%.1f", averageFrequency), color = Color(0xFFEF6C00))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotationsContent(structure: PDBStructure, proteinId: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Structure Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                InfoRow(InfoRowData("PDB ID", proteinId.ifBlank { structure.annotations.firstOrNull { it.type.displayName == "PDB ID" }?.value ?: "Unknown" }, "Protein Data Bank identifier - unique code for this structure"))
                InfoRow(InfoRowData("Total Atoms", structure.atoms.size.toString(), "All atoms in the structure including protein and ligands"))
                InfoRow(InfoRowData("Total Bonds", structure.bonds.size.toString(), "Chemical bonds connecting atoms in the structure"))
                InfoRow(InfoRowData("Chains", structure.chainCount.toString(), "Number of polypeptide chains in the protein"))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Chemical Composition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val uniqueElements = structure.atoms.map { it.element }.distinct().sorted()
                InfoRow(InfoRowData("Elements", uniqueElements.size.toString(), "Number of different chemical elements present"))
                InfoRow(InfoRowData("Element Types", uniqueElements.joinToString(), "Chemical elements found in this structure"))
                val chainList = structure.chains
                InfoRow(InfoRowData("Chain IDs", chainList.joinToString(), "Identifiers for each polypeptide chain"))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Protein Classification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val uniqueResidues = structure.atoms.map { it.residueName }.distinct().sorted()
                InfoRow(InfoRowData("Residue Types", uniqueResidues.size.toString(), "Number of different amino acid types present"))
                InfoRow(InfoRowData("Residue Names", uniqueResidues.joinToString(), "Three-letter codes of amino acids in this protein"))
                val totalResidues = structure.residues.size
                InfoRow(InfoRowData("Total Residues", totalResidues.toString(), "Total number of amino acid residues across all chains"))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Biological Context", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                InfoRow(InfoRowData("Structure Type", "Protein", "This is a protein structure determined by experimental methods"))
                InfoRow(InfoRowData("Data Source", "PDB", "Protein Data Bank - worldwide repository of 3D structure data"))
                InfoRow(InfoRowData("Quality", "Experimental", "Structure determined through experimental techniques like X-ray crystallography"))
                val hasLigands = structure.atoms.any { it.isLigand }
                InfoRow(InfoRowData("Ligands", if (hasLigands) "Present" else "None", if (hasLigands) "Small molecules or ions bound to the protein" else "No small molecules detected in this structure"))
            }
        }

        if (structure.annotations.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Additional Annotations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    structure.annotations.forEachIndexed { index, annotation ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(annotation.type.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(annotation.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            annotation.description.takeIf { it.isNotBlank() }?.let { desc ->
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (index < structure.annotations.size - 1) {
                                Divider(color = Color(0xFFE3E5E8), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainStat(label: String, value: String) {
    Column {
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
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

private fun residueThreeToOne(name: String): String {
    return when (name.uppercase()) {
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

@Composable
private fun InfoTabBar(selectedTab: InfoTab, onTabSelected: (InfoTab) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(InfoTab.values().size) { index ->
                val tab = InfoTab.values()[index]
                val isSelected = tab == selectedTab
                
                Surface(
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp)),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(18.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
