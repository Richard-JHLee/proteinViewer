package com.avas.proteinviewer.ui.protein

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.avas.proteinviewer.data.model.FilamentStructure
import com.avas.proteinviewer.rendering.SimpleFilamentRenderer

/**
 * ValtoLibraries 방식의 간단한 Filament Compose 래퍼
 */
@Composable
fun SimpleFilamentComposeView(
    structure: FilamentStructure?,
    onStructureLoaded: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var surfaceView by remember { mutableStateOf<SimpleFilamentRenderer?>(null) }
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
    
    AndroidView(
        factory = { context ->
            SimpleFilamentRenderer(context).apply {
                surfaceView = this
            }
        },
        update = { view ->
            structure?.let { 
                view.loadStructure(it)
                onStructureLoaded?.invoke()
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
