package com.avas.proteinviewer.ui.protein

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.avas.proteinviewer.data.model.PDBStructure
import com.avas.proteinviewer.rendering.gles.GLESProteinRenderer

/**
 * OpenGL ES 3.0 전용 3D 단백질 뷰어
 */
class OpenGL30SurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer = GLESProteinRenderer()
    
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

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val pointerCount = e2.pointerCount
            queueEvent {
                if (pointerCount >= 2 && !scaleDetector.isInProgress) {
                    renderer.pan(-distanceX, distanceY)
                } else {
                    renderer.orbit(distanceX, distanceY)
                }
            }
            requestRender()
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount
        
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
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
