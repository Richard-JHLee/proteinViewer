package com.avas.proteinviewer.ui.layout

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.ui.protein.ProteinViewerView

@Composable
fun ResponsiveProteinViewer(
    structure: PDBStructure?,
    proteinId: String = "",
    modifier: Modifier = Modifier
) {
    ProteinViewerView(
        structure = structure,
        proteinId = proteinId,
        modifier = modifier.fillMaxSize()
    )
}



@Composable
fun ResponsiveMainContent(
    structure: PDBStructure?,
    proteinId: String = "",
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onSwitchToViewer: () -> Unit = {}
) {
    ProteinViewerView(
        structure = structure,
        proteinId = proteinId,
        modifier = modifier.fillMaxSize(),
        onMenuClick = onMenuClick,
        onLibraryClick = onLibraryClick,
        onSwitchToViewer = onSwitchToViewer
    )
}
