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
    
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            if (scaleFactor == 0f || scaleFactor.isNaN()) return false
            queueEvent {
                renderer.zoom(scaleFactor)
            }
            requestRender()
            return true
        }
    })

    private var lastX = 0f
    private var lastY = 0f
    private var isMultiTouch = false
    
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // Single finger만 orbit (두 손가락은 onTouchEvent에서 처리)
            if (e2.pointerCount == 1 && !isMultiTouch) {
                queueEvent {
                    renderer.orbit(distanceX, distanceY)
                }
                requestRender()
            }
            return true
        }
    })

    init {
        try {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = RENDERMODE_CONTINUOUSLY
            android.util.Log.d("OpenGL30SurfaceView", "OpenGL ES 3.0 surface created")
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount
        
        // Zoom 제스처 처리 (항상 먼저)
        val scaledHandled = scaleDetector.onTouchEvent(event)
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                isMultiTouch = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // 두 번째 손가락 감지
                if (pointerCount == 2) {
                    isMultiTouch = true
                    lastX = (event.getX(0) + event.getX(1)) / 2
                    lastY = (event.getY(0) + event.getY(1)) / 2
                    android.util.Log.d("OpenGL30SurfaceView", "Multi-touch detected for PAN")
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerCount == 2 && isMultiTouch && !scaleDetector.isInProgress) {
                    // 두 손가락 Pan (Zoom 중이 아닐 때)
                    val currentX = (event.getX(0) + event.getX(1)) / 2
                    val currentY = (event.getY(0) + event.getY(1)) / 2
                    val dx = currentX - lastX
                    val dy = currentY - lastY
                    
                    queueEvent {
                        renderer.pan(-dx, dy)
                    }
                    requestRender()
                    
                    lastX = currentX
                    lastY = currentY
                    android.util.Log.d("OpenGL30SurfaceView", "PAN: dx=$dx, dy=$dy")
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
    
}
