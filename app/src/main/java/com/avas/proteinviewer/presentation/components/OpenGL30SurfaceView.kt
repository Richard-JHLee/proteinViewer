package com.avas.proteinviewer.presentation.components

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.avas.proteinviewer.domain.model.PDBStructure
import com.avas.proteinviewer.domain.model.RenderStyle
import com.avas.proteinviewer.domain.model.ColorMode
import com.avas.proteinviewer.rendering.gles.ProperRibbonRenderer

/**
 * OpenGL ES 3.0 전용 3D 단백질 뷰어 (Ribbon 스타일)
 * Catmull-Rom 스플라인 + 튜브 메쉬로 폭이 있는 Ribbon 렌더링
 */
class OpenGL30SurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer = ProperRibbonRenderer()
    private var rotationEnabled = false
    private var isInfoMode = false
    private var onRenderingCompleteCallback: (() -> Unit)? = null
    private var onRenderingStartCallback: (() -> Unit)? = null
    
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            if (scaleFactor == 0f || scaleFactor.isNaN()) return false
            queueEvent {
                renderer.zoom(scaleFactor)
            }
            // 줌할 때마다 부드럽게 움직이도록 렌더링 요청
            requestRender()
            return true
        }
    })

    private var lastX = 0f
    private var lastY = 0f
    private var isMultiTouch = false
    private var isGestureInProgress = false // 제스처 진행 상태 추적
    
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // Single finger만 rotate (두 손가락은 onTouchEvent에서 처리)
            // Info 모드와 Viewer 모드 모두 항상 회전 허용
            if (e2.pointerCount == 1 && !isMultiTouch) {
                isGestureInProgress = true
                queueEvent {
                    renderer.rotate(distanceX, distanceY)
                }
                // 드래그할 때마다 부드럽게 움직이도록 렌더링 요청
                requestRender()
            }
            return true
        }
    })

    init {
        try {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            // 부드러운 제스처를 위해 연속 렌더링 사용
            renderMode = RENDERMODE_CONTINUOUSLY
            android.util.Log.d("OpenGL30SurfaceView", "OpenGL ES 3.0 surface created with RENDERMODE_WHEN_DIRTY")
        } catch (e: Exception) {
            android.util.Log.e("OpenGL30SurfaceView", "Error creating OpenGL surface", e)
        }
    }

    fun updateStructure(structure: PDBStructure?) {
        queueEvent {
            renderer.updateStructure(structure)
            android.util.Log.d("OpenGL30SurfaceView", "Structure updated: ${structure?.atoms?.size ?: 0} atoms")
        }
        requestRender()
    }
    
    fun updateRenderStyle(style: RenderStyle) {
        queueEvent {
            renderer.updateRenderStyle(style)
            android.util.Log.d("OpenGL30SurfaceView", "Render style updated: $style")
        }
        requestRender()
    }
    
    fun updateColorMode(mode: ColorMode) {
        queueEvent {
            renderer.updateColorMode(mode)
            android.util.Log.d("OpenGL30SurfaceView", "Color mode updated: $mode")
        }
        requestRender()
    }
    
    fun updateOptions(
        rotationEnabled: Boolean = false,
        zoomLevel: Float = 1.0f,
        transparency: Float = 1.0f,
        atomSize: Float = 1.0f,
        ribbonWidth: Float = 1.2f,
        ribbonFlatness: Float = 0.5f
    ) {
        this.rotationEnabled = rotationEnabled // 상태 저장
        queueEvent {
            renderer.updateOptions(
                rotationEnabled = rotationEnabled,
                zoomLevel = zoomLevel,
                transparency = transparency,
                atomSize = atomSize,
                ribbonWidth = ribbonWidth,
                ribbonFlatness = ribbonFlatness
            )
            android.util.Log.d("OpenGL30SurfaceView", "Options updated: rotation=$rotationEnabled, zoom=$zoomLevel, transparency=$transparency, atomSize=$atomSize, ribbonWidth=$ribbonWidth, ribbonFlatness=$ribbonFlatness")
        }
        requestRender()
    }
    
    fun updateHighlightedChains(highlightedChains: Set<String>) {
        queueEvent {
            renderer.updateHighlightedChains(highlightedChains)
            android.util.Log.d("OpenGL30SurfaceView", "Highlighted chains updated: $highlightedChains")
        }
        requestRender()
    }
    
    fun updateFocusedElement(focusedElement: String?) {
        queueEvent {
            renderer.updateFocusedElement(focusedElement)
            android.util.Log.d("OpenGL30SurfaceView", "Focused element updated: $focusedElement")
        }
        requestRender()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount
        
        // 성능 최적화: Info 모드 디버깅 로그 제거
        
        // Zoom 제스처 처리 (항상 먼저)
        val scaledHandled = scaleDetector.onTouchEvent(event)
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                isMultiTouch = false
                isGestureInProgress = true // 제스처 시작
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // 두 번째 손가락 감지
                if (pointerCount == 2) {
                    isMultiTouch = true
                    lastX = (event.getX(0) + event.getX(1)) / 2
                    lastY = (event.getY(0) + event.getY(1)) / 2
                    // 성능 최적화: Multi-touch 로그 제거
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Info 모드와 Viewer 모드 모두 동일하게 팬 제스처 허용
                val shouldAllowPan = pointerCount == 2 && isMultiTouch && !scaleDetector.isInProgress
                
                if (shouldAllowPan) {
                    // 두 손가락 Pan
                    val currentX = (event.getX(0) + event.getX(1)) / 2
                    val currentY = (event.getY(0) + event.getY(1)) / 2
                    val dx = currentX - lastX
                    val dy = currentY - lastY
                    
                    queueEvent {
                        renderer.pan(dx, -dy) // X축은 그대로, Y축만 반대로 처리
                    }
                    // 팬할 때마다 부드럽게 움직이도록 렌더링 요청
                    requestRender()
                    
                    lastX = currentX
                    lastY = currentY
                    // 성능 최적화: PAN 로그 제거
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // 손가락 하나가 떼어졌을 때
                if (pointerCount == 2) {
                    isMultiTouch = false
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isMultiTouch = false
                isGestureInProgress = false // 제스처 종료
                requestRender() // 제스처 완료 후 최종 렌더링
            }
        }
        
        // Single finger 제스처 (orbit)
        if (pointerCount == 1 && !isMultiTouch) {
            gestureDetector.onTouchEvent(event)
        }
        
        return true
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("OpenGL30SurfaceView", "Surface resumed")
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("OpenGL30SurfaceView", "Surface paused")
    }
    
    fun setInfoMode(isInfoMode: Boolean) {
        this.isInfoMode = isInfoMode // 상태 저장
        queueEvent {
            renderer.setInfoMode(isInfoMode)
        }
    }
    
    fun setAutoRotation(enabled: Boolean) {
        queueEvent {
            renderer.setAutoRotation(enabled)
        }
    }
    
    fun setOnRenderingCompleteCallback(callback: (() -> Unit)?) {
        onRenderingCompleteCallback = callback
        renderer.setOnRenderingCompleteCallback(callback)
    }
    
    fun setOnRenderingStartCallback(callback: (() -> Unit)?) {
        onRenderingStartCallback = callback
        android.util.Log.d("OpenGL30SurfaceView", "Rendering start callback set: ${callback != null}")
    }
    
}
