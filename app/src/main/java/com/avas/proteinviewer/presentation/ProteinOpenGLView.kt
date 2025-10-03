package com.avas.proteinviewer.presentation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.avas.proteinviewer.domain.model.PDBStructure
import com.avas.proteinviewer.domain.model.RenderStyle
import com.avas.proteinviewer.domain.model.ColorMode
import com.avas.proteinviewer.presentation.components.OpenGL30SurfaceView

/**
 * Compose wrapper for OpenGL ES 3.0 protein viewer
 * 아이폰의 SceneKit과 동일한 OpenGL 기반 3D 렌더링
 */
@Composable
fun ProteinOpenGLView(
    structure: PDBStructure,
    renderStyle: RenderStyle = RenderStyle.RIBBON,
    colorMode: ColorMode = ColorMode.CHAIN,
    highlightedChains: Set<String> = emptySet(),
    focusedElement: String? = null,
    rotationEnabled: Boolean = false,
    zoomLevel: Float = 1.0f,
    transparency: Float = 1.0f,
    atomSize: Float = 1.0f,
    ribbonWidth: Float = 1.2f,
    ribbonFlatness: Float = 0.5f,
    isInfoMode: Boolean = false, // Info 모드 여부
    onRenderingComplete: () -> Unit = {}, // 렌더링 완료 콜백
    onRenderingStart: () -> Unit = {}, // 렌더링 시작 콜백
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            // OpenGL30SurfaceView의 init 블록에서 이미 모든 설정을 완료함
            OpenGL30SurfaceView(context)
        },
        update = { view ->
            // 구조, 렌더 스타일, 컬러 모드, 하이라이트, 포커스 업데이트
            view.updateStructure(structure)
            view.updateRenderStyle(renderStyle)
            view.updateColorMode(colorMode)
            view.updateHighlightedChains(highlightedChains)
            view.updateFocusedElement(focusedElement)
            view.updateOptions(
                rotationEnabled = rotationEnabled,
                zoomLevel = zoomLevel,
                transparency = transparency,
                atomSize = atomSize,
                ribbonWidth = ribbonWidth,
                ribbonFlatness = ribbonFlatness
            )
            view.setInfoMode(isInfoMode) // Info 모드 설정
            view.setAutoRotation(rotationEnabled) // 자동 회전 설정
            view.setOnRenderingCompleteCallback(onRenderingComplete) // 렌더링 완료 콜백 설정
            view.setOnRenderingStartCallback(onRenderingStart) // 렌더링 시작 콜백 설정
        },
        modifier = modifier
    )
    
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("ProteinOpenGLView", "OpenGL view disposed")
        }
    }
}

