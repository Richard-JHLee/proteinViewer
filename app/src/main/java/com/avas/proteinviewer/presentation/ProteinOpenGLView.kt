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
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            // OpenGL30SurfaceView의 init 블록에서 이미 모든 설정을 완료함
            OpenGL30SurfaceView(context)
        },
        update = { view ->
            // 구조, 렌더 스타일, 컬러 모드, 하이라이트 업데이트
            view.updateStructure(structure)
            view.updateRenderStyle(renderStyle)
            view.updateColorMode(colorMode)
            view.updateHighlightedChains(highlightedChains)
        },
        modifier = modifier
    )
    
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("ProteinOpenGLView", "OpenGL view disposed")
        }
    }
}

