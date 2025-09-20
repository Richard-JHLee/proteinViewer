package com.avas.proteinviewer.ui.protein

import android.app.ActivityManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import com.avas.proteinviewer.data.model.PDBStructure

@Composable
fun ProteinViewerView(
    structure: PDBStructure?,
    proteinId: String = "",
    modifier: Modifier = Modifier,
    backend: RendererBackend = RendererBackend.OpenGL
) {
    val context = LocalContext.current
    val supportsEs3 = remember {
        val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? ActivityManager
        val config = am?.deviceConfigurationInfo
        val version = config?.reqGlEsVersion ?: 0
        val supports = version >= 0x00030000
        android.util.Log.d("ProteinViewerView", "GLES version: $version, supports ES3: $supports")
        supports
    }
    var surfaceView by remember { mutableStateOf<OpenGL30SurfaceView?>(null) }

    Box(modifier = modifier) {
        when {
            backend == RendererBackend.OpenGL && supportsEs3 -> {
                AndroidView(
                    factory = { ctx ->
                        android.util.Log.d("ProteinViewerView", "Creating OpenGL30SurfaceView")
                        OpenGL30SurfaceView(ctx).apply {
                            surfaceView = this
                            updateStructure(structure)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    update = { view ->
                        android.util.Log.d("ProteinViewerView", "Updating OpenGL30SurfaceView with structure: ${structure != null}")
                        view.updateStructure(structure)
                    }
                )
            }
            else -> {
                android.util.Log.d(
                    "ProteinViewerView",
                    "Using BasicFilamentComposeView (GLES3 not supported or backend not OpenGL)"
                )
                BasicFilamentComposeView(
                    structure = structure?.let { s ->
                        com.avas.proteinviewer.data.converter.StructureConverter.convertPDBToStructure(s)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

        if (structure == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun InfoSheet(
    structure: PDBStructure?,
    selectedTab: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedTab,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                "Overview" -> {
                    structure?.let { proteinStructure ->
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                StatCard(
                                    value = proteinStructure.atoms.size.toString(),
                                    label = "Atoms",
                                    color = androidx.compose.ui.graphics.Color(0xFF2196F3)
                                )
                            }
                            item {
                                StatCard(
                                    value = proteinStructure.bonds.size.toString(),
                                    label = "Bonds",
                                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                )
                            }
                            item {
                                StatCard(
                                    value = proteinStructure.chains.size.toString(),
                                    label = "Chains",
                                    color = androidx.compose.ui.graphics.Color(0xFFFF9800)
                                )
                            }
                            item {
                                StatCard(
                                    value = proteinStructure.residues.size.toString(),
                                    label = "Residues",
                                    color = androidx.compose.ui.graphics.Color(0xFF9C27B0)
                                )
                            }
                        }
                    }
                }
                else -> {
                    Text("Content for $selectedTab will be added soon.")
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .width(120.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
