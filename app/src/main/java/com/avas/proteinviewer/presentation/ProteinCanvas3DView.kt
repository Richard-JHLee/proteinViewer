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
import androidx.compose.animation.core.*

@Composable
fun ProteinCanvas3DView(
    structure: PDBStructure,
    renderStyle: RenderStyle,
    colorMode: ColorMode,
    highlightedChains: Set<String> = emptySet(),
    focusedElement: String? = null, // 아이폰과 동일: Focus 지원
    modifier: Modifier = Modifier
) {
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    // 아이폰과 동일: Focus 시 자동 카메라 이동
    LaunchedEffect(focusedElement) {
        if (focusedElement != null) {
            // Focus된 요소의 중심 계산
            val focusedAtoms = structure.atoms.filter { atom ->
                when {
                    focusedElement.startsWith("chain:") -> {
                        val chainId = focusedElement.removePrefix("chain:")
                        atom.chain == chainId
                    }
                    focusedElement.startsWith("ligand:") -> {
                        val ligandName = focusedElement.removePrefix("ligand:")
                        atom.residueName == ligandName
                    }
                    focusedElement.startsWith("pocket:") -> {
                        val pocketName = focusedElement.removePrefix("pocket:")
                        atom.residueName == pocketName
                    }
                    else -> false
                }
            }
            
            if (focusedAtoms.isNotEmpty()) {
                // Focus된 영역의 중심 계산
                val centerX = focusedAtoms.map { it.position.x }.average().toFloat()
                val centerY = focusedAtoms.map { it.position.y }.average().toFloat()
                
                // 전체 단백질의 중심
                val allCenterX = structure.atoms.map { it.position.x }.average().toFloat()
                val allCenterY = structure.atoms.map { it.position.y }.average().toFloat()
                
                // Focus 영역으로 화면 이동 (아이폰과 동일: 애니메이션)
                val targetOffsetX = (allCenterX - centerX) * 50f // 스케일 조정
                val targetOffsetY = (allCenterY - centerY) * 50f
                
                // 부드러운 애니메이션으로 이동
                offsetX = targetOffsetX
                offsetY = targetOffsetY
                
                android.util.Log.d("ProteinCanvas3DView", "Focus camera moved to: offsetX=$offsetX, offsetY=$offsetY")
            }
        }
    }
    
    // iOS 스타일 렌더러 생성
    // highlightedChains에서 "chain:", "ligand:", "pocket:" 접두사 제거하여 순수 ID만 추출
    val cleanedHighlights = remember(highlightedChains) {
        android.util.Log.d("ProteinCanvas3DView", "Original highlightedChains: $highlightedChains")
        val cleaned = highlightedChains.mapNotNull { 
            when {
                it.startsWith("chain:") -> it.removePrefix("chain:")
                it.startsWith("ligand:") -> it.removePrefix("ligand:")
                it.startsWith("pocket:") -> it.removePrefix("pocket:")
                else -> it
            }
        }.toSet()
        android.util.Log.d("ProteinCanvas3DView", "Cleaned highlights for renderer: $cleaned")
        cleaned
    }
    
    val renderer = remember(structure, renderStyle, colorMode, cleanedHighlights, focusedElement) {
        android.util.Log.d("ProteinCanvas3DView", "Creating renderer with highlights: $cleanedHighlights, focusedElement: $focusedElement")
        Protein3DRenderer(
            structure = structure,
            renderStyle = renderStyle,
            colorMode = colorMode,
            highlightedChains = cleanedHighlights,
            focusedElement = focusedElement // Focus 전달
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
