package com.avas.proteinviewer.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.avas.proteinviewer.domain.model.PDBStructure
import com.avas.proteinviewer.domain.model.RenderStyle
import com.avas.proteinviewer.domain.model.ColorMode

@Composable
fun ProteinCanvas3DView(
    structure: PDBStructure,
    renderStyle: RenderStyle,
    colorMode: ColorMode,
    highlightedChains: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    // iOS 스타일 렌더러 생성
    val renderer = remember(structure, renderStyle, colorMode, highlightedChains) {
        Protein3DRenderer(
            structure = structure,
            renderStyle = renderStyle,
            colorMode = colorMode,
            highlightedChains = highlightedChains
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale *= zoom
                    scale = scale.coerceIn(0.5f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    rotationY += dragAmount.x * 0.5f
                    rotationX += dragAmount.y * 0.5f
                }
            }
    ) {
        // 렌더러에 변환 업데이트
        renderer.updateTransform(rotationX, rotationY, scale, offsetX, offsetY)
        // 렌더링 실행
        renderer.render(this)
    }
}
