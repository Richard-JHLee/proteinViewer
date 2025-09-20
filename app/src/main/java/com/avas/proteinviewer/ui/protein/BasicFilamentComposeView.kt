package com.avas.proteinviewer.ui.protein

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.avas.proteinviewer.data.model.FilamentStructure
import com.avas.proteinviewer.rendering.BasicFilamentView

/**
 * 기본 Filament 기반 3D 단백질 뷰어
 */
@Composable
fun BasicFilamentComposeView(
    structure: FilamentStructure?,
    onStructureLoaded: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var surfaceView by remember { mutableStateOf<BasicFilamentView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 생명주기 관리
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // 생명주기 이벤트 처리
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // 생명주기 이벤트 처리
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 구조 업데이트
    LaunchedEffect(structure) {
        structure?.let { 
            surfaceView?.loadStructure(it)
            onStructureLoaded?.invoke()
        }
    }
    
    AndroidView(
        factory = { context ->
            BasicFilamentView(context).apply {
                surfaceView = this
            }
        },
        update = { view ->
            // View 업데이트는 LaunchedEffect에서 처리
            structure?.let { 
                view.loadStructure(it)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
